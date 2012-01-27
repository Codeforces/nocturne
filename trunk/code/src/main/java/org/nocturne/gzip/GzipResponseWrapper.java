package org.nocturne.gzip;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class GzipResponseWrapper extends HttpServletResponseWrapper {
    protected HttpServletResponse response;
    protected ServletOutputStream stream;
    protected PrintWriter writer;

    public GzipResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
    }

    public void finishResponse() throws IOException {
        if (writer != null) {
            writer.close();
        } else {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private ServletOutputStream createOutputStream() throws IOException {
        return new GzipResponseStream(response);
    }

    @Override
    public void flushBuffer() throws IOException {
        stream.flush();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called!");
        }

        if (stream == null)
            stream = createOutputStream();

        return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return writer;
        }

        if (stream == null) {
            stream = createOutputStream();
        }

        writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));

        return (writer);
    }

    @Override
    public void setContentLength(int length) {
        // No operations.
    }
}
