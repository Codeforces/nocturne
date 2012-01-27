/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stream utilities.
 *
 * @author Mike Mirzayanov
 */
public class StreamUtil {
    private static final int CHUNK_SIZE = 262144;

    /**
     * Reads stream to byte array. Closes the stream on exit.
     *
     * @param inputStream InputStream instance to be read.
     * @return InputStream instance content as byte[].
     * @throws IOException On IO error.
     */
    public static byte[] getAsByteArray(InputStream inputStream) throws IOException {
        return getAsByteArray(inputStream, Integer.MAX_VALUE);
    }

    /**
     * Reads stream to byte array.
     *
     * @param inputStream InputStream instance to be read.
     * @param maxSize     Maximal possible content size. Throws IOException if size exceeded.
     * @return InputStream instance content as byte[].
     * @throws IOException On IO error. Throws IOException if maxSize exceeded.
     */
    public static byte[] getAsByteArray(InputStream inputStream, int maxSize) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            byte[] chunk = new byte[CHUNK_SIZE];
            while (true) {
                int size = inputStream.read(chunk);
                if (size == -1) {
                    break;
                }
                outputStream.write(chunk, 0, size);
                if (outputStream.size() > maxSize) {
                    throw new IOException("Data exceeds " + maxSize + " bytes");
                }
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // No operation.
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * Transfers the data from one stream to another. Closes streams on exit.
     *
     * @param in  Source stream.
     * @param out Destination stream.
     * @throws IOException On IO errors.
     */
    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[CHUNK_SIZE];
            int len;

            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // No operations.
            }
            try {
                out.close();
            } catch (IOException e) {
                // No operations.
            }
        }
    }
}
