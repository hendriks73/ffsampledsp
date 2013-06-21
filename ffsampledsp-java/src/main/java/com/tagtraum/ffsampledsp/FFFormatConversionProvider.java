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
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;
import java.nio.ByteOrder;
import java.util.Set;

import static com.tagtraum.ffsampledsp.FFAudioFormat.FFEncoding.Codec.*;

/**
 * {@link FormatConversionProvider} for FFSampledSP.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFFormatConversionProvider extends FormatConversionProvider {

    private static final boolean nativeLibraryLoaded;

    static {
        // Ensure JNI library is loaded
        nativeLibraryLoaded = FFNativeLibraryLoader.loadLibrary();
    }

    public static final boolean NATIVE_ORDER = ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder());
    public static final int MONO = 1;
    public static final int STEREO = 2;

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        if (!nativeLibraryLoaded) return new AudioFormat.Encoding[0];
        final Set<FFAudioFormat.FFEncoding> supportedAudioFormats = FFAudioFormat.FFEncoding.getSupportedEncodings();
        return supportedAudioFormats.toArray(new AudioFormat.Encoding[supportedAudioFormats.size()]);
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        if (!nativeLibraryLoaded) return new AudioFormat.Encoding[0];
        return new AudioFormat.Encoding[] {
                PCM_SIGNED.getEncoding(),
                PCM_UNSIGNED.getEncoding(),
                PCM_FLOAT.getEncoding()
        };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(final AudioFormat sourceFormat) {
        if (!nativeLibraryLoaded) return new AudioFormat.Encoding[0];
        return new AudioFormat.Encoding[] {
                PCM_SIGNED.getEncoding(),
                PCM_UNSIGNED.getEncoding(),
                PCM_FLOAT.getEncoding()
        };
    }

    @Override
    public boolean isConversionSupported(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat){
        if (!nativeLibraryLoaded) return false;
        // because we only support conversions from FFAudioInputStreams we have to check whether the source format
        // was created by us. All source formats created by us, have the property "provider" set to "ffsampledsp".
        if (!FFAudioFormat.FFSAMPLEDSP.equals(sourceFormat.properties().get(FFAudioFormat.PROVIDER))) return false;
        if (super.isConversionSupported(targetEncoding, sourceFormat)) return true;
        final FFAudioFormat.FFEncoding ffEncoding = FFAudioFormat.FFEncoding.getInstance(targetEncoding.toString());
        // for now we only decode to signed linear pcm or float pcm
        return PCM_SIGNED.getEncoding().equals(ffEncoding)
                || PCM_UNSIGNED.getEncoding().equals(ffEncoding)
                || PCM_FLOAT.getEncoding().equals(ffEncoding);
    }

    @Override
    public boolean isConversionSupported(final AudioFormat targetFormat, final AudioFormat sourceFormat) {
        if (!nativeLibraryLoaded) return false;
        // because we only support conversions from FFAudioInputStreams we have to check whether the source format
        // was created by us. All source formats created by us, have the property "provider" set to "ffsampledsp".
        if (!FFAudioFormat.FFSAMPLEDSP.equals(sourceFormat.properties().get(FFAudioFormat.PROVIDER))) return false;
        if (super.isConversionSupported(targetFormat, sourceFormat)) return true;
        if (!FFCodecInputStream.isEncodingSupported(targetFormat)) return false;
        return FFCodecInputStream.isSampleSizeFrameSizeChannelsSupported(targetFormat);
    }


    @Override
    public AudioFormat[] getTargetFormats(final AudioFormat.Encoding targetEncoding, final AudioFormat sourceFormat) {
        if (!nativeLibraryLoaded) return new AudioFormat[0];

        final FFAudioFormat.FFEncoding ffEncoding = FFAudioFormat.FFEncoding.getInstance(targetEncoding.toString());

        // 8 bit signed is not OK
        if (PCM_UNSIGNED.getEncoding().equals(ffEncoding) || PCM_SIGNED.getEncoding().equals(ffEncoding)) return new AudioFormat[] {
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 8,  MONO,      1, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 8,  STEREO,    2, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 16, MONO,      2, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 16, STEREO,    4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 24, MONO,      4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 24, STEREO,    8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 32, MONO,      4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 32, STEREO,    8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)
        };

        // 32 and 64 bit float
        if (PCM_FLOAT.getEncoding().equals(ffEncoding)) return new AudioFormat[] {
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 32, MONO,   4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 32, STEREO, 8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 64, MONO,   8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER),
                new AudioFormat(ffEncoding, AudioSystem.NOT_SPECIFIED, 64, STEREO, 16, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)
        };

        return new AudioFormat[0];
    }

    @Override
    public AudioInputStream getAudioInputStream(final AudioFormat targetFormat, final AudioInputStream sourceStream) {
        if (!nativeLibraryLoaded) throw new IllegalArgumentException("Native library ffsampledsp not loaded.");
        try {
            return new FFAudioInputStream(new FFCodecInputStream(targetFormat, (FFAudioInputStream)sourceStream), targetFormat, AudioSystem.NOT_SPECIFIED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create AudioInputStream with format " + targetFormat + " from " + sourceStream, e);
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(final AudioFormat.Encoding targetEncoding, final AudioInputStream sourceStream) {
        final AudioFormat sourceFormat = sourceStream.getFormat();
        // we assume some defaults...
        final int sampleSizeInBits = sourceFormat.getSampleSizeInBits() > 0 ? sourceFormat.getSampleSizeInBits() : 16;
        final int frameSize = sourceFormat.getFrameSize() > 0 ? sourceFormat.getFrameSize() : sampleSizeInBits * sourceFormat.getChannels() / 8;
        final AudioFormat targetFormat = new AudioFormat(targetEncoding, sourceFormat.getSampleRate(),
                sampleSizeInBits, sourceFormat.getChannels(), frameSize,
                sourceFormat.getSampleRate(), sourceFormat.isBigEndian());
        return getAudioInputStream(targetFormat, sourceStream);
    }


}
