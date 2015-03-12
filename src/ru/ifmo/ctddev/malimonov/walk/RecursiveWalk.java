package ru.ifmo.ctddev.malimonov.walk;

import javax.sql.rowset.serial.SerialRef;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * Created by heat_wave on 18.02.15.
 */
public class RecursiveWalk {
    public static void main(String[] args) {
        Charset charset = Charset.forName("UTF-8");

        if (args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Not enough input parameters");
            return;
        }

        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);

        try (BufferedReader reader = Files.newBufferedReader(input, charset);
             BufferedWriter writer = Files.newBufferedWriter(output, charset)) {

            String line;
            while ((line = reader.readLine()) != null) {
                Path current = Paths.get(line);
                if (Files.isDirectory(current)) {
                    for (String hashString : readDirectory(current))
                        writer.write(hashString);
                } else {
                    writer.write(readFile(current));
                }
                writer.flush();
            }

        }
        catch (NoSuchFileException e) {
            System.err.println("Input file not found: " + input.toString());
        }
        catch (UnsupportedEncodingException e) {
            System.err.println("Input file charset is not UTF-8.");
        }
        catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        }
        catch (NullPointerException e) {
            System.err.println("Null pointer exception: " + e.getMessage());
        }
    }

    private static ArrayList<String> readDirectory(Path current) {
        ArrayList<String> hash = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            for (Path path : stream) {
                if (Files.isDirectory(path))
                    hash.addAll(readDirectory(path));
                else
                    hash.add(readFile(path));
            }
        }
        catch (IOException e) {}
        return hash;
    }

    private static String readFile(Path current) {
        int hash = 0;

        try (FileChannel channel = new FileInputStream(current.toFile()).getChannel()) {
            hash = hashFNV(channel);
        }
        catch (IOException e) {
            hash = 0;
        }

        String hashString = String.format("%08x", hash);
        String result = hashString + " " + current.toString() + System.getProperty("line.separator");

        return result;
    }

    public static int hashFNV(FileChannel channel) {
        final int OFFSET_BASIS = 0x811c9dc5;
        final int FNV_PRIME = 0x01000193;
        final int PAGE_SIZE = 1024 * 128;

        int hash = OFFSET_BASIS;

        ByteBuffer reader = ByteBuffer.allocate(PAGE_SIZE);
        int c = 0;

        try {
            while (channel.read(reader) != -1) {
                reader.flip();

                while (reader.hasRemaining()) {
                    c = reader.get() & 0xff;
                    hash *= FNV_PRIME;
                    hash ^= c;
                }

                reader.clear();
            }

        } catch (Exception e) {
            System.err.println("Error while calculating hash:" + e.getMessage());
            return 0;
        }

        return hash;
    }
}
