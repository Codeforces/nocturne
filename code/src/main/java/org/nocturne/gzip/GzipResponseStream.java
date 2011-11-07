package org.nocturne.gzip;

import java.io.*;
import java.util.zip.GZIPOutputStream;
import javax.servlet.*;
import javax.servlet.http.*;

public class GzipResponseStream extends ServletOutputStream {
    private ByteArrayOutputStream baos;
    private GZIPOutputStream gzipStream;
    private boolean closed;
    private HttpServletResponse response;
    private ServletOutputStream output;

    public GzipResponseStream(HttpServletResponse response) throws IOException {
        super();
        closed = false;
        this.response = response;
        this.output = response.getOutputStream();
        baos = new ByteArrayOutputStream();
        gzipStream = new GZIPOutputStream(baos);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("This output stream has already been closed");
        }

        gzipStream.finish();
        byte[] bytes = baos.toByteArray();

        response.addHeader("Content-Length", Integer.toString(bytes.length));
        response.addHeader("Content-Encoding", "gzip");

        output.write(bytes);
        output.flush();
        output.close();

        closed = true;
    }

    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Cannot flush a closed output stream");
        }

        gzipStream.flush();
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }

        gzipStream.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }

        gzipStream.write(b, off, len);
    }
}
