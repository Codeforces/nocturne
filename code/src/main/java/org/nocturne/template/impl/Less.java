package org.nocturne.template.impl;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.core.DefaultLessCompiler;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;

/**
 * @author MikeMirzayanov (mirzayanovmr@gmail.com)
 */
public class Less {
    private static final String CACHE_OPEN_TAG = "<cache>";
    private static final String CACHE_CLOSE_TAG = "</cache>";
    private static File tmpDir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String compile(@Nonnull Object source, @Nonnull String lessCode, @Nullable  File commonsFile) throws IOException {
        long commonsFileLastModified = commonsFile == null ? 0 : commonsFile.lastModified();
        long commonsFileLength = commonsFile == null ? 0 : commonsFile.length();
        String cacheKey = Long.toString(commonsFileLastModified) + "_" + commonsFileLength + "_" + hash(lessCode);
        File cacheDir = new File(tmpDir, "cache");
        if (!cacheDir.isDirectory()) {
            cacheDir.mkdirs();
            if (!cacheDir.isDirectory()) {
                throw new IOException("Can't create " + cacheDir + ".");
            }
        }
        File cacheFile = new File(cacheDir, cacheKey);
        if (cacheFile.exists() && cacheFile.isFile()) {
            String result = readFile(cacheFile);
            if (result.startsWith(CACHE_OPEN_TAG) && result.endsWith(CACHE_CLOSE_TAG)) {
                return result.substring(CACHE_OPEN_TAG.length(), result.length() - CACHE_CLOSE_TAG.length());
            }
        }
        if (cacheFile.exists()) {
            cacheFile.delete();
        }

        File workDir = new File(tmpDir, cacheKey);
        workDir.mkdirs();
        if (!workDir.isDirectory()) {
            throw new IOException("Can't create " + workDir + ".");
        }

        File workCommonsFile;
        if (commonsFile != null) {
            workCommonsFile = new File(workDir, commonsFile.getName());
            writeFile(workCommonsFile, readFile(commonsFile));
            lessCode = "@import \"" + commonsFile.getName() + "\";\n" + lessCode;
        } else {
            workCommonsFile = null;
        }

        File workMainFile = new File(workDir, "main.less");
        writeFile(workMainFile, lessCode);

        try {
            LessCompiler compiler = new DefaultLessCompiler();
            try {
                LessCompiler.CompilationResult compilationResult = compiler.compile(workMainFile);
                String cachedResult = CACHE_OPEN_TAG + compilationResult.getCss() + CACHE_CLOSE_TAG;
                writeFile(cacheFile, cachedResult);
                return compilationResult.getCss();
            } catch (Less4jException e) {
                throw new IOException("Can't compile less code in \"" + source + "\": " + e.getMessage(), e);
            }
        } finally {
            workMainFile.delete();
            if (workCommonsFile != null) {
                workCommonsFile.delete();
            }
            workDir.delete();
        }
    }

    private static long hash(String css) {
        long MUL_A = 1009L;
        long MUL_B = 1000000000000000003L;
        long a = 0;
        long b = 0;
        long mulA = 1;
        long mulB = 1;
        for (int i = 0; i < css.length(); i++) {
            a += mulA * css.charAt(i);
            mulA *= MUL_A;
            b += mulB * css.charAt(i);
            mulB *= MUL_B;
        }
        return a ^ b;
    }

    private static void writeFile(File file, String content) throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print(content);
        }
    }

    private static String readFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            StringBuilder result = new StringBuilder((int) file.length());
            char[] buffer = new char[65536];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                result.append(buffer, 0, len);
            }
            return result.toString();
        }
    }

    static {
        try {
            File tmpFile = File.createTempFile("Nocturne", "Less");
            if (!tmpFile.delete()) {
                throw new IOException("Can't delete " + tmpFile + ".");
            }
            tmpDir = new File(tmpFile.getParentFile(), "nocturne-less-tmp");
            if (!tmpDir.isDirectory()) {
                if (!tmpDir.mkdirs()) {
                    throw new IOException("Can't create " + tmpFile + ".");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

