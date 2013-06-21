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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static com.tagtraum.ffsampledsp.FFGlobalLock.LOCK;

/**
 * Audio stream backed by FFmpeg.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public abstract class FFNativePeerInputStream extends InputStream {

    static {
        // Ensure JNI library is loaded
        FFNativeLibraryLoader.loadLibrary();
    }

    private static final int DEFAULT_NATIVE_BUFFER_SIZE = 32 * 1024;

    /**
     * Pointer to the native peer struct.
     */
    protected long pointer;

    /**
     * Buffer the native side copies audio data into.
     */
    protected ByteBuffer nativeBuffer;

    protected FFNativePeerInputStream() throws IOException, UnsupportedAudioFileException {
        setNativeBufferCapacity(DEFAULT_NATIVE_BUFFER_SIZE);
    }

    /**
     * Replace the old direct buffer with a newly allocated direct buffer, if the specified <code>minimumCapacity</code>
     * is larger than the current <code>capacity</code> of the already allocated buffer.
     * In other words, we never shrink the buffer, but only grow.
     *
     * @param minimumCapacity desired capacity of the new buffer
     * @return actual current capacity
     */
    private int setNativeBufferCapacity(final int minimumCapacity) {
        if (nativeBuffer != null && nativeBuffer.hasRemaining()) {
            throw new IllegalStateException("Can't replace native buffer while the old buffer still has data remaining.");
        }
        if (nativeBuffer == null || nativeBuffer.capacity() < minimumCapacity) {
            nativeBuffer = ByteBuffer.allocateDirect(minimumCapacity);
        }
        return nativeBuffer.capacity();
    }

    @Override
    public synchronized int read() throws IOException {
        if (!nativeBuffer.hasRemaining()) {
            fillNativeBuffer();
        }
        // we're at the end
        if (!nativeBuffer.hasRemaining()) {
            close();
            return -1;
        }
        return nativeBuffer.get() & 0xff;
    }

    @Override
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) return 0;
        if (len < 0) throw new IllegalArgumentException("Length must be greater than or equal to 0: " + len);
        if (off < 0) throw new IllegalArgumentException("Offset must be greater than or equal to 0: " + off);
        if (b.length - off < len) throw new IllegalArgumentException("There must be more space than "  + len + " bytes left in the buffer. Offset is " + off);

        int bytesRead = 0;
        while (bytesRead < len) {
            if (!nativeBuffer.hasRemaining()) {
                fillNativeBuffer();
                if (!nativeBuffer.hasRemaining()) {
                    // we're at the end
                    close();
                    break;
                }
            }
            final int chunkSize = Math.min(len-bytesRead, nativeBuffer.remaining());
            nativeBuffer.get(b, off+bytesRead, chunkSize);
            bytesRead += chunkSize;
        }
        return bytesRead == 0 ? -1 : bytesRead;
    }

    /**
     * @see com.tagtraum.ffsampledsp.FFAudioInputStream#isSeekable()
     */
    public abstract boolean isSeekable();

    /**
     * @see com.tagtraum.ffsampledsp.FFAudioInputStream#seek(long, java.util.concurrent.TimeUnit)
     */
    public abstract void seek(final long time, final TimeUnit timeUnit) throws UnsupportedOperationException, IOException;

    /**
     * Indicates whether the underlying native peer is still available.
     *
     * @return true, if open
     */
    protected boolean isOpen() {
        return pointer != 0;
    }

    @Override
    public synchronized void close() throws IOException {
        if (isOpen()) {
            try {
                lockedClose(pointer);
            } finally {
                pointer = 0;
            }
        }
    }

    /**
     * Synchronizes call to {@link #close(long)} via {@link FFGlobalLock#LOCK}.
     *
     * @param pointer pointer
     * @throws IOException
     */
    private void lockedClose(final long pointer) throws IOException {
        LOCK.lock();
        try {
            close(pointer);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Fills {@link #nativeBuffer} with new data.
     *
     * @throws IOException
     */
    protected abstract void fillNativeBuffer() throws IOException;

    /**
     * Closes the native peer and releases all resources held by it.
     *
     * @param pointer pointer
     * @throws IOException
     */
    protected abstract void close(final long pointer) throws IOException;

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.finalize();
    }

}
