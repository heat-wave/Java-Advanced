package ru.ifmo.ctddev.malimonov.walk;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;

/**
 * Created by heat_wave on 16.02.15.
 */
public class Walk {

    public static void main(String[] args) {
        Charset charset = Charset.forName("UTF-8");
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);

        try (BufferedReader reader = Files.newBufferedReader(input, charset);
             BufferedWriter writer = Files.newBufferedWriter(output, charset)) {

            String line;
            while ((line = reader.readLine()) != null) {
                Path current = Paths.get(line);
                int hash = 0;

                try (FileChannel channel = new FileInputStream(line).getChannel()) {
                    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                    hash = hashFNV(buffer);
                    buffer = null;
                }
                catch (IOException e) {
                    //System.err.println("Exception while calculating hash for file" + line);
                }

                String hashString = (hash == 0? "00000000" : Integer.toHexString(hash));
                String result = hashString + " " + current.toString()
                        + System.getProperty("line.separator");
                writer.write(result);
                writer.flush();
            }

        }
        catch (NoSuchFileException e) {
            System.err.println("Input file not found.");
        }
        catch (UnsupportedEncodingException e) {
            System.err.println("Input file charset is not UTF-8.");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static int hashFNV(MappedByteBuffer reader) {
        final int OFFSET_BASIS = 216613521;
        final int FNV_PRIME = 16777619;

        int hash = OFFSET_BASIS;

        int c = 0;
        try {
            while (reader.hasRemaining()) {
                c = reader.get();
                hash *= FNV_PRIME;
                hash ^= c;
            }
        }
        catch (Exception e) {
            System.err.println("Error while calculating hash:" + e.getMessage());
            return 0;
        }
        return hash;
    }

}
