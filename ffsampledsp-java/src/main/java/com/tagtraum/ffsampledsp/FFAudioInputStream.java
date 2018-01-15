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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Effectively acts as a wrapper around our own {@link FFNativePeerInputStream}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFAudioInputStream extends AudioInputStream {

    private FFNativePeerInputStream nativePeerInputStream;

    public FFAudioInputStream(final FFNativePeerInputStream nativePeerInputStream, final AudioFormat format, final long length) {
        super(nativePeerInputStream, new AudioFormat(
                format.getEncoding(),
                format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels(),
                format.getFrameSize(),
                format.getFrameRate(),
                format.isBigEndian(),
                createProperties(format.properties())
                ), length);
        this.nativePeerInputStream = nativePeerInputStream;
    }

    private static Map<String, Object> createProperties(final Map<String, Object> p) {
        final Map<String, Object> properties = new HashMap<String, Object>(p);
        properties.put(FFAudioFormat.PROVIDER, FFAudioFormat.FFSAMPLEDSP);
        return properties;
    }

    /**
     * Wrapped {@link FFNativePeerInputStream}.
     *
     * @return wrapped stream
     */
    FFNativePeerInputStream getNativePeerInputStream() {
        return nativePeerInputStream;
    }

    /**
     * Pointer to the wrapped {@link FFNativePeerInputStream}.
     *
     * @return pointer
     */
    long getNativePeerInputStreamPointer() {
        return nativePeerInputStream.pointer;
    }

    /**
     * Indicates whether this stream is seekable.
     * Typically, stream based streams (as opposed to file-based streams)
     * are not seekable.
     *
     * @return true or false
     */
    public boolean isSeekable() {
        return nativePeerInputStream.isSeekable();
    }

    /**
     * Positions the stream at the desired timestamp.
     *
     * @param time time
     * @param timeUnit time unit
     * @throws UnsupportedOperationException if the operation is not supported
     * @throws IOException if something goes wrong
     */
    public void seek(final long time, final TimeUnit timeUnit) throws UnsupportedOperationException, IOException {
        nativePeerInputStream.seek(time, timeUnit);
        final long microSeconds = timeUnit.toMicros(time);
        framePos = (long)((getFormat().getFrameRate() * microSeconds) / 1000L);
    }
}
