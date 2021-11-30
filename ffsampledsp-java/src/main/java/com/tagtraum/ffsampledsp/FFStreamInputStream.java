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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.TimeUnit;

import static com.tagtraum.ffsampledsp.FFGlobalLock.LOCK;

/**
 * Audio stream capable of decoding a stream via FFmpeg.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFStreamInputStream extends FFNativePeerInputStream {

    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(32 * 1024);
    private final ReadableByteChannel channel;

    public FFStreamInputStream(final InputStream stream) throws IOException, UnsupportedAudioFileException {
        this(stream, 0);
    }

    public FFStreamInputStream(final InputStream stream, final int streamIndex) throws IOException, UnsupportedAudioFileException {
        // workaround covariant return type introduced in Java 9
        // ensure limit(int) is called on Buffer, not ByteBuffer
        ((Buffer)this.nativeBuffer).limit(0);
        this.channel = Channels.newChannel(stream);
        this.pointer = lockedOpen(streamIndex);
    }

    /**
     * Always returns <code>false</code>.
     * Stream based {@link FFNativePeerInputStream}s are not seekable.
     *
     * @return false
     */
    @Override
    public boolean isSeekable() {
        return false;
    }

    /**
     * Always throws {@link UnsupportedOperationException}, because stream based
     * {@link FFNativePeerInputStream}s are not seekable
     *
     * @param time time
     * @param timeUnit time unit
     * @throws UnsupportedOperationException always throws UnsupportedOperationException
     * @throws IOException if an IO error occurs
     */
    @Override
    public void seek(final long time, final TimeUnit timeUnit) throws UnsupportedOperationException, IOException {
        throw new UnsupportedOperationException("Seeking is not supported.");
    }

    /**
     * Calls {@link #fillNativeBuffer(long)}.
     *
     * @throws IOException if an IO error occurs
     */
     @Override
    protected void fillNativeBuffer() throws IOException {
        if (isOpen()) {
            // make sure we are at the start of the native buffer, before we fill it

            // workaround covariant return type introduced in Java 9
            // ensure limit(int) is called on Buffer, not ByteBuffer
            ((Buffer)this.nativeBuffer).limit(0);
            // read data, until we have a new limit or we reached the end of the file
            fillNativeBuffer(pointer);
            if (!nativeBuffer.hasRemaining()) {
                close();
            }
        }
    }

    /**
     * Is called by native code to fill the {@link #readBuffer} with data from the stream.
     *
     * @throws IOException if an IO error occurs
     */
    private int fillReadBuffer() throws IOException {
        readBuffer.clear();
        final int justRead = channel.read(readBuffer);
        readBuffer.flip();
        return justRead;
    }

    /**
     * Synchronizes calls to {@link #open(int)}.
     *
     * @return pointer to native peer
     * @throws IOException if an IO error occurs
     */
    private long lockedOpen(final int streamIndex) throws IOException {
        LOCK.lock();
        try {
            return open(streamIndex);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Tells native code to fill the {@link #nativeBuffer} with decoded audio.
     * It does so by first calling {@link #fillReadBuffer()} to read encoded audio, decodes it,
     * and then places the decoded data into {@link #nativeBuffer}.
     *
     * @param pointer pointer to native peer
     * @throws IOException if an IO error occurs
     */
    private native void fillNativeBuffer(final long pointer) throws IOException;

    /**
     * Allocates native peer.
     *
     * @param streamIndex index of the desired audio stream (if there are multiple ones)
     * @return pointer to native peer
     * @throws IOException if an IO error occurs
     */
    private native long open(final int streamIndex) throws IOException;

    @Override
    protected native void close(final long pointer) throws IOException;


}
