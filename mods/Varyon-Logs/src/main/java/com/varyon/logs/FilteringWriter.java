package com.varyon.logs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

class FilteringWriter extends OutputStreamWriter {

    private static final OutputStream DUMMY = new OutputStream() {
        @Override public void write(int b) {}
        @Override public void write(byte[] b, int off, int len) {}
    };

    private final OutputStreamWriter delegate;
    private final StringBuilder oneCharBuf = new StringBuilder(1);


    FilteringWriter(OutputStreamWriter delegate) {
        super(DUMMY, StandardCharsets.UTF_8);
        this.delegate = delegate;
    }

    private boolean isFiltered(String msg) {
        for (Pattern p : LogFilters.PATTERNS) {
            if (p.matcher(msg).find()) return true;
        }
        return false;
    }

    @Override
    public void write(String str) throws IOException {
        if (!isFiltered(str)) delegate.write(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        if (!isFiltered(str.substring(off, off + len))) delegate.write(str, off, len);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (!isFiltered(new String(cbuf, off, len))) delegate.write(cbuf, off, len);
    }

    @Override
    public void write(int c) throws IOException {
        oneCharBuf.setLength(0);
        oneCharBuf.append((char) c);
        if (!isFiltered(oneCharBuf.toString())) delegate.write(c);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
