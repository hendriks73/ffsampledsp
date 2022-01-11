/*
 * =================================================
 * Copyright 2013 tagtraum industries incorporated
 * This file is part of FFSampledSP.
 *
 * FFSampledSP is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFSampledSP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFSampledSP; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * =================================================
 */
package com.tagtraum.ffsampledsp;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.util.concurrent.TimeUnit;

import static com.tagtraum.ffsampledsp.FFGlobalLock.LOCK;

/**
 * Audio stream capable of decoding resources via FFmpeg.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFURLInputStream extends FFNativePeerInputStream {

    private final boolean seekable;
    private final URL url;

    public FFURLInputStream(final URL url) throws IOException, UnsupportedAudioFileException {
        this(url, 0);
    }

    public FFURLInputStream(final URL url, final int streamIndex) throws IOException, UnsupportedAudioFileException {
        // FFmpeg did not use to recognize DRM-crippled files.
        // Therefore we avoid decoding altogether.
        if (url.toString().toLowerCase().endsWith(".m4p")) {
            throw new UnsupportedAudioFileException("DRM encrypted file is unsupported: " + url);
        }
        this.url = url;
        // workaround covariant return type introduced in Java 9
        // ensure limit(int) is called on Buffer, not ByteBuffer
        ((Buffer)this.nativeBuffer).limit(0);
        this.pointer = lockedOpen(FFAudioFileReader.urlToString(url), streamIndex);
        this.seekable = isSeekable(pointer);
    }

    @Override
    public boolean isSeekable() {
        return seekable;
    }

    @Override
    public synchronized void seek(final long time, final TimeUnit timeUnit) throws UnsupportedOperationException, IOException {
        if (!isOpen()) throw new IOException("Stream is already closed: " + url);
        if (!isSeekable()) throw new UnsupportedOperationException("Seeking is not supported for " + url);
        final long microseconds = timeUnit.toMicros(time);
        seek(pointer, microseconds);
        // workaround covariant return type introduced in Java 9
        // ensure limit(int) is called on Buffer, not ByteBuffer
        ((Buffer)this.nativeBuffer).limit(0);
    }


    @Override
    protected void fillNativeBuffer() throws IOException {
        if (isOpen()) {
            fillNativeBuffer(pointer);
        }
    }

    @Override
    public String toString() {
        return "FFURLInputStream{" +
                "url=" + url +
                ", seekable=" + seekable +
                '}';
    }

    /**
     * Synchronizes calls to {@link #open(String, int)}.
     *
     * @param url url
     * @param streamIndex index of the stream in the file, typically 0, but may differ for STEMS
     * @return pointer to native peer
     * @throws IOException if something IO-related goes wrong
     * @throws UnsupportedAudioFileException if the file is not supported
     * @throws IndexOutOfBoundsException if the stream index is not valid
     */
    private long lockedOpen(final String url, final int streamIndex) throws IOException, UnsupportedAudioFileException {
        LOCK.lock();
        try {
            return open(url, streamIndex);
        } finally {
            LOCK.unlock();
        }
    }

    private native boolean isSeekable(final long pointer);
    private native void seek(final long pointer, final long microseconds) throws IOException;
    private native void fillNativeBuffer(final long pointer) throws IOException;
    private native long open(final String url, final int streamIndex) throws IOException, UnsupportedAudioFileException;
    protected native void close(final long pointer) throws IOException;

}
