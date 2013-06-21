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
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import static com.tagtraum.ffsampledsp.FFAudioFormat.FFEncoding.Codec.*;
import static com.tagtraum.ffsampledsp.FFGlobalLock.LOCK;

/**
 * Used by {@link FFFormatConversionProvider} to convert a {@link FFAudioInputStream} (not just
 * any {@link javax.sound.sampled.AudioInputStream}) to another {@link AudioFormat}.
 * <p>
 * Note that we take a shortcut:<br>
 * Instead of converting the data from the source stream, we re-configure the native underpinnings
 * of the source stream to produce the data we desire. In other words, we manipulate the
 * encoder and the <code>SwrContext</code> of the stream originally opened with FFmpeg.
 * This of course only works, if the stream to convert is also an {@link FFAudioInputStream}.
 * This needs to be checked in {@link FFFormatConversionProvider} using the {@link FFAudioFormat#PROVIDER}
 * property of the source format.
 *
 * @see FFFormatConversionProvider#isConversionSupported(javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioFormat)
 * @see FFFormatConversionProvider#isConversionSupported(javax.sound.sampled.AudioFormat.Encoding, javax.sound.sampled.AudioFormat)
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFCodecInputStream extends FFNativePeerInputStream {

    private FFNativePeerInputStream wrappedStream;

    public FFCodecInputStream(final AudioFormat targetFormat, final FFAudioInputStream stream) throws IOException, UnsupportedAudioFileException {

        this.wrappedStream = stream.getNativePeerInputStream();

        if (!isEncodingSupported(targetFormat)) {
            throw new UnsupportedEncodingException("This codec does not support the encoding \"" + targetFormat.getEncoding()
                    + "\". Supported codecs are: " + FFAudioFormat.FFEncoding.getSupportedEncodings());
        }
        if (!isSampleSizeFrameSizeChannelsSupported(targetFormat)) {
            throw new UnsupportedEncodingException("This codec does not support the desired frame size " + targetFormat.getFrameSize() + ".");
        }
        final FFAudioFormat.FFEncoding ffEncoding = FFAudioFormat.FFEncoding.getInstance(targetFormat);
        AudioFormat audioFormat = new AudioFormat(ffEncoding,
                    targetFormat.getSampleRate(), targetFormat.getSampleSizeInBits(), targetFormat.getChannels(),
                    targetFormat.getFrameSize(), targetFormat.getFrameRate(), targetFormat.isBigEndian());

        this.nativeBuffer.limit(0);
        this.pointer = lockedOpen(audioFormat, stream.getNativePeerInputStreamPointer());
    }

    /**
     * Indicates whether the combination of channels, frame size and sample size is supported.
     * In essence we only support formats that completely fill a frame.
     * As an example, mono 24bit audio has to have a framesize of 3 bytes, <em>not</em> 4 bytes.
     *
     * @param targetFormat desired target format
     * @return true, if supported
     */
    static boolean isSampleSizeFrameSizeChannelsSupported(final AudioFormat targetFormat) {
        boolean supported = true;

        if (targetFormat.getChannels() < 1 || targetFormat.getChannels() > 2) supported = false;
        // check valid sampleSize/frameSize/channels combinations
        switch (targetFormat.getSampleSizeInBits()) {
            case 8:
                if (targetFormat.getFrameSize() != targetFormat.getChannels()) supported = false;
                break;
            case 16:
                if (targetFormat.getFrameSize() != 2*targetFormat.getChannels()) supported = false;
                break;
            case 24:
                if (targetFormat.getFrameSize() != 3*targetFormat.getChannels()) supported = false;
                break;
            case 32:
                if (targetFormat.getFrameSize() != 4*targetFormat.getChannels()) supported = false;
                break;
            case 64:
                if (targetFormat.getFrameSize() != 8*targetFormat.getChannels()) supported = false;
                break;
            default:
                supported = false;
        }
        return supported;
    }

    /**
     * Indicates whether a desired {@link AudioFormat.Encoding} is supported as target encoding.
     * Essentially, only PCM_SIGNED, PCM_UNSIGNED and PCM_FLOAT are supported.
     *
     * @param targetFormat target format
     * @return true, if supported
     */
    static boolean isEncodingSupported(final AudioFormat targetFormat) {
        final FFAudioFormat.FFEncoding ffEncoding = FFAudioFormat.FFEncoding.getInstance(targetFormat.getEncoding().toString());
        boolean supported;
        if (PCM_UNSIGNED.getEncoding().equals(ffEncoding) && targetFormat.getSampleSizeInBits() >= 8 && targetFormat.getSampleSizeInBits() <= 32) {
            supported = true;
        } else if (PCM_FLOAT.getEncoding().equals(ffEncoding) && (targetFormat.getSampleSizeInBits() == 32 || targetFormat.getSampleSizeInBits() == 64)) {
            supported = true;
        } else if (PCM_SIGNED.getEncoding().equals(ffEncoding) && targetFormat.getSampleSizeInBits() >= 8 && targetFormat.getSampleSizeInBits() <= 32) {
            supported = true;
        } else {
            supported = false;
        }
        return supported;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return wrappedStream.read(b, off, len);
    }

    @Override
    public int read() throws IOException {
        return wrappedStream.read();
    }

    @Override
    protected boolean isOpen() {
        return wrappedStream.isOpen();
    }

    @Override
    public void close() throws IOException {
        wrappedStream.close();
    }

    @Override
    protected void close(final long pointer) throws IOException {
        wrappedStream.close();
    }

    @Override
    protected void fillNativeBuffer() throws IOException {
        wrappedStream.fillNativeBuffer();
    }

    @Override
    public boolean isSeekable() {
        return wrappedStream.isSeekable();
    }

    @Override
    public void seek(final long time, final TimeUnit timeUnit) throws UnsupportedOperationException, IOException {
        wrappedStream.seek(time, timeUnit);
    }

    private long lockedOpen(final AudioFormat target, final long pointer) throws IOException {
        LOCK.lock();
        try {
            return open(target, pointer);
        } finally {
            LOCK.unlock();
        }
    }

    private native long open(final AudioFormat target, final long pointer) throws IOException;

}
