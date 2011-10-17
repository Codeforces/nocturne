/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Utility class with some file operations. You can use it
 * instead of apache version if you want.
 *
 * @author Mike Mirzayanov
 */
public class FileUtil {
    /** Stores xpath factory. */
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    /**
     * Copies one file to another. Fails if the target exists.
     *
     * @param source      Source file.
     * @param destination Destination file.
     * @throws IOException Can't perform copy.
     */
    public static void copyFile(File source, File destination) throws IOException {
        if (!source.isFile()) {
            throw new IOException(source + " is not a file.");
        }

        if (destination.exists()) {
            throw new IOException("Destination file " + destination + " is already exist.");
        }

        FileChannel inChannel = new FileInputStream(source).getChannel();
        FileChannel outChannel = new FileOutputStream(destination).getChannel();

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            inChannel.close();
            outChannel.close();
        }
    }

    /**
     * Copy one directory into another. If the second one exists it copies nested files from
     * the source to destination.
     *
     * @param source      Source directory.
     * @param destination Destination directory.
     * @throws IOException when can't perform copy.
     */
    public static void copyDirectory(File source, File destination) throws IOException {
        if (!source.isDirectory()) {
            throw new IOException(source + " is not a directory.");
        }

        if (destination.isFile()) {
            throw new IOException(destination + " is a file.");
        }

        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                throw new IOException("Can't create " + destination + '.');
            }
        }

        String children[] = source.list();

        for (String child : children) {
            File nextSource = new File(source, child);
            File nextDestination = new File(destination, child);
            if (nextSource.isDirectory()) {
                copyDirectory(nextSource, nextDestination);
            } else {
                copyFile(nextSource, nextDestination);
            }
        }
    }

    /**
     * Deletes file or directory. Finishes quitely in case of no such file.
     * Directory will be deleted with each nested element.
     *
     * @param file File to be deleted.
     * @throws IOException if can't delete file.
     */
    public static void deleteTotaly(File file) throws IOException {
        if (file.exists()) {
            if (file.isFile()) {
                if (!file.delete()) {
                    throw new IOException("Can't delete " + file + '.');
                }
            } else {
                String[] children = file.list();
                for (String child : children) {
                    deleteTotaly(new File(file, child));
                }
                if (!file.delete()) {
                    throw new IOException("Can't delete " + file + '.');
                }
            }
        }
    }


    /**
     * Deletes file or directory. Finishes quitely in _any_ case.
     * Will start new thread.
     *
     * @param file File to be deleted.
     */
    public static void deleteTotalyAsync(final File file) {
        new Thread() {
            @Override
            public void run() {
                try {
                    deleteTotaly(file);
                } catch (Throwable e) {
                    // No operations.
                }
            }
        }.start();
    }

    /**
     * @param reader Reader to be processed.
     * @return String containing all characters from reader.
     * @throws IOException if can't read data.
     */
    public static String readFromReader(Reader reader) throws IOException {
        StringBuffer result = new StringBuffer();
        try {
            char[] chunk = new char[65536];
            while (true) {
                int size = reader.read(chunk);
                if (size == -1) {
                    break;
                }
                result.append(chunk, 0, size);
            }
        } catch (IOException e) {
            throw new IOException("Can't read from reader.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // No operation.
                }
            }
        }
        return result.toString();
    }

    /**
     * @param file File to be read.
     * @return String containing file data.
     * @throws IOException if can't read file. Possibly, file parameter
     *                     doesn't exists, is directory or not enought permissions.
     */
    public static String readFile(File file) throws IOException {
        return readFromReader(new FileReader(file));
    }

    private static void ensureParentDirectoryExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Can't create directory " + parent + '.');
        }
    }

    /**
     * Writes new file into filesystem. Overwrite existing if exists.
     * Creates parent directory if needed.
     *
     * @param file    File to be write.
     * @param content Content to be write.
     * @throws java.io.IOException if can't read file.
     */
    public static void writeFile(File file, String content) throws IOException {
        ensureParentDirectoryExists(file);

        FileWriter writer = null;

        try {
            writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // No operation.
                }
            }
        }
    }

    /**
     * Writes new file into filesystem. Overwrite existing if exists.
     * Creates parent directory if needed.
     *
     * @param file  File to be write.
     * @param bytes Bytes to be write.
     * @throws java.io.IOException if can't write file.
     */
    public static void writeFile(File file, byte[] bytes) throws IOException {
        ensureParentDirectoryExists(file);

        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(bytes);
        outputStream.close();
    }

    /**
     * Very like to writeFile but doesn't overwrite file.
     * Creates parent directory if needed.
     *
     * @param file  File to write.
     * @param bytes Bytes to write into file.
     * @throws IOException If file exists or can't write file.
     */
    public static void createFile(File file, byte[] bytes) throws IOException {
        if (file.exists()) {
            throw new IOException("File exists " + file);
        }

        writeFile(file, bytes);
    }

    /**
     * Very like to writeFile but doesn't overwrite file.
     *
     * @param file    File to write.
     * @param content String to write into file.
     * @throws IOException If file exists or can't write file.
     */
    public static void createFile(File file, String content) throws IOException {
        if (file.exists()) {
            throw new IOException("File exists " + file);
        }

        writeFile(file, content);
    }

    /**
     * Parses XML string and extracts value.
     *
     * @param xml   InputStream containing xml document.
     * @param xpath Xpath expression.
     * @param clazz String.class or Integer.class are supported now.
     * @param <T>   Return type.
     * @return Return value.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T extractFromXml(InputStream xml, String xpath, Class<T> clazz) {
        XPath xp = XPATH_FACTORY.newXPath();

        QName type = null;

        if (clazz.equals(String.class)) {
            type = XPathConstants.STRING;
        }

        if (clazz.equals(Integer.class)) {
            type = XPathConstants.NUMBER;
        }

        if (type == null) {
            throw new IllegalArgumentException("Illegal clazz.");
        }

        try {
            XPathExpression expression = xp.compile(xpath);

            Object result = expression.evaluate(new InputSource(xml), type);
            if (type == XPathConstants.NUMBER) {
                result = (((Double) result).intValue());
                return (T) result;
            } else {
                return (T) result;
            }
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Illegal xpath.", e);
        }
    }

    /**
     * Writes XML document into file.
     *
     * @param file     File to write.
     * @param document XML document.
     */
    public static void writeXml(File file, Document document) {
        Source source = new DOMSource(document);
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            StreamResult result = new StreamResult(new FileOutputStream(file));
            transformer.transform(source, result);
            result.getOutputStream().close();
        } catch (TransformerConfigurationException e) {
            throw new IllegalArgumentException("Transformer configuration is illegal.", e);
        } catch (TransformerException e) {
            throw new IllegalArgumentException("Transformer failed.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't perform IO.", e);
        }
    }

    /**
     * Changes the value describing by xpath to specific value. And updates file.
     *
     * @param file  Which will read first and updated later.
     * @param xpath Xpath to find specific Node.
     * @param value Value to be set for found node.
     */
    public static void updateXml(File file, String xpath, String value) {
        XPath xp = XPATH_FACTORY.newXPath();

        try {
            XPathExpression root = xp.compile("/");
            Document document = (Document) root.evaluate(new InputSource(new FileInputStream(file)), XPathConstants.NODE);
            XPathExpression nodeXpath = xp.compile(xpath);
            Node node = (Node) nodeXpath.evaluate(document, XPathConstants.NODE);
            node.setNodeValue(value);
            writeXml(file, document);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Illegal xpath.", e);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Can't find file.", e);
        }
    }

    /**
     * @param file File to remove.
     * @throws IOException If file not found or can't be removed.
     */
    public static void removeFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found " + file + '.');
        }

        if (!file.delete()) {
            throw new IOException("Can't delete " + file + '.');
        }
    }

    public static byte[] _getBytes(File file) throws IOException {
        int size = (int) file.length();
        InputStream stream = new FileInputStream(file);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(size);
        byte[] chunk = new byte[1048576];
        while (true) {
            int byteCount = stream.read(chunk);
            if (byteCount < 0) {
                break;
            } else {
                byteArrayOutputStream.write(chunk, 0, byteCount);
            }
        }
        stream.close();
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * @param file File to be read.
     * @return File content as a byte array.
     * @throws IOException           if can't read file.
     * @throws FileNotFoundException if can't find file.
     */
    public static byte[] getBytes(File file) throws IOException {
        if (file.isFile()) {
            long size = file.length();
            FileInputStream stream = new FileInputStream(file);
            FileChannel channel = stream.getChannel();
            ByteBuffer bytes = ByteBuffer.allocate((int) size);
            channel.read(bytes);
            channel.close();
            stream.close();
            return bytes.array();
        } else {
            throw new FileNotFoundException("Can't find " + file + '.');
        }
    }

    /**
     * Returns first 255 bytes of the file. Returns smaller number of bytes it it contains less.
     *
     * @param file File to be read.
     * @return File content as a byte array.
     * @throws IOException           if can't read file.
     * @throws FileNotFoundException if can't find file.
     */
    public static FirstBytes getFirstBytes(File file) throws IOException {
        if (file.isFile()) {
            boolean truncated = false;
            long size = file.length();
            if (size > 255) {
                truncated = true;
                size = 255;
            }
            FileInputStream stream = new FileInputStream(file);
            FileChannel channel = stream.getChannel();
            ByteBuffer bytes = ByteBuffer.allocate((int) size);
            channel.read(bytes);
            channel.close();
            stream.close();
            return new FirstBytes(truncated, bytes.array());
        } else {
            throw new FileNotFoundException("Can't find " + file + '.');
        }
    }

    /** Accepts not fidden files. */
    public static class NotHiddenFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return !new File(dir, name).isHidden();
        }
    }

    /**
     * @param file Any file.
     * @return String Name part (simple name without extension).
     */
    public static String getName(File file) {
        String name = file.getName();
        if (name.contains(".")) {
            return name.substring(0, name.lastIndexOf("."));
        } else {
            return name;
        }
    }

    /**
     * @param file Any file.
     * @return String Extension with dot in lowercase. For example, ".cpp".
     */
    public static String getExt(File file) {
        String name = file.getName();
        if (name.contains(".")) {
            return name.substring(name.lastIndexOf(".")).toLowerCase();
        } else {
            return "";
        }
    }

    /**
     * @param directory Directory to be processed.
     * @return Total length of _all_ nested files in the directory.
     */
    public static long getDirectorySize(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Abstract path " + directory + " is not a directory.");
        }

        long result = 0;

        File[] files = directory.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                result += file.length();
            }

            if (file.isDirectory()) {
                result += getDirectorySize(file);
            }
        }

        return result;
    }

    public static class FirstBytes {
        private final boolean truncated;
        private final byte[] bytes;

        private FirstBytes(boolean truncated, byte[] bytes) {
            this.truncated = truncated;
            this.bytes = bytes;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
