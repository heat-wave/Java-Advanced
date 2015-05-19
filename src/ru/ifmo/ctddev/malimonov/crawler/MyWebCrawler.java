package ru.ifmo.ctddev.malimonov.crawler;

/**
 * Created by heat_wave on 5/13/15.
 */

import info.kgeorgiy.java.advanced.crawler.*;
import net.java.quickcheck.collection.Pair;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MyWebCrawler implements Crawler {
    private Downloader downloader;
    private ExecutorService downloadService;
    private ExecutorService extractService;
    Map<String, Object> ret;
    Map<String, Object> vis;
    Map<String, IOException> err;
    Map<String, ConcurrentLinkedDeque<Pair<String, Integer>>> downloadQueue;
    Semaphore semaphore;

    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length > 4) {
            System.err.println("incorrect usage of crawler(args: url downloads extractors perHost)");
            return;
        }
        String url = args[0];
        int downloaders = 10;
        int extractors = 10;
        int perHost = 10;
        downloaders = (args.length > 1) ? Integer.parseInt(args[1]) : downloaders;
        extractors = (args.length > 2) ? Integer.parseInt(args[2]) : extractors;
        perHost = (args.length > 3) ? Integer.parseInt(args[3]) : perHost;
        try (MyWebCrawler crawler = new MyWebCrawler(new CachingDownloader(new File("./default/")), downloaders, extractors, perHost)) {
            crawler.download(url, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public MyWebCrawler(Downloader d, int downloaders, int extractors, int perHost) {
        this.downloader = d;
        this.downloadService = Executors.newFixedThreadPool(downloaders);
        this.extractService = Executors.newFixedThreadPool(extractors);
    }

    /**
     * A method that extracts links from current document and processes them further
     * @param depth current depth
     * @param document the document to process
     */
    void extractProduce(int depth, Document document) {
        try {
            if (depth > 1) {
                List<String> links = document.extractLinks();
                links.stream().distinct().forEach(url -> {
                    try {
                        String host = URLUtils.getHost(url);
                        downloadQueue.putIfAbsent(host, new ConcurrentLinkedDeque<>());
                        downloadQueue.get(host).addLast(new Pair<>(url, depth - 1));
                        semaphore.acquire();
                        downloadService.submit(() -> this.downloadProduce(host));
                    } catch (MalformedURLException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    /**
     * A method that checks if the host has been visited, and if not, processes it
     * @param host current host to process
     */
    void downloadProduce(String host) {
        try {
            if (!downloadQueue.get(host).isEmpty()) {
                Pair<String, Integer> pair = downloadQueue.get(host).removeFirst();
                String url = pair.getFirst();
                int depth = pair.getSecond();
                if (vis.putIfAbsent(url, new Object()) == null) {
                    try {
                        Document document = downloader.download(url);
                        ret.put(url, new Object());
                        semaphore.acquire();
                        extractService.submit(() -> extractProduce(depth, document));
                    } catch (IOException e) {
                        err.put(url, e);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            semaphore.release();
        }
    }

    /**
     * A method that starts "crawling"
     * @param url initial link
     * @param depth "recursion" depth
     * @return result of the download
     */
    @Override
    public Result download(String url, int depth) {
        int semSize = Integer.MAX_VALUE;
        semaphore = new Semaphore(semSize);
        ret = new ConcurrentHashMap<>();
        vis = new ConcurrentHashMap<>();
        err = new ConcurrentHashMap<>();
        downloadQueue = new ConcurrentHashMap<>();
        try {
            String host = URLUtils.getHost(url);
            System.out.println("URL: " + url + " HOST: " + host);
            downloadQueue.putIfAbsent(host, new ConcurrentLinkedDeque<>());
            downloadQueue.get(host).addLast(new Pair<>(url, depth));
            semaphore.acquire();
            downloadService.submit(() -> this.downloadProduce(host));
        } catch (InterruptedException | MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        try {
            semaphore.acquire(semSize);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new Result(ret.keySet().stream().collect(Collectors.toList()), err);
    }

    /**
     * Close auxiliary streams
     */
    @Override
    public void close() {
        downloadService.shutdown();
        extractService.shutdown();
    }
}