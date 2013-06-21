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

import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.tagtraum.ffsampledsp.FFFormatConversionProvider.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TestFFFormatConversionProvider.
 * <p/>
 * Date: 8/25/11
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFFormatConversionProvider {

    @Test
    public void testGetTargetFormatsSigned() {
        final AudioFormat[] targetFormats = new FFFormatConversionProvider().getTargetFormats(AudioFormat.Encoding.PCM_SIGNED, new AudioFormat(FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(), 22050f, 16, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true));
        System.out.println("Formats: " + targetFormats.length);
        System.out.println(Arrays.asList(targetFormats));

        final Set<AudioFormat> audioFormats = new HashSet<AudioFormat>(Arrays.asList(targetFormats));

        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 8,  MONO,      1, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 8,  STEREO,    2, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, MONO,      2, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, STEREO,    4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 24, MONO,      4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 24, STEREO,    8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 32, MONO,      4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 32, STEREO,    8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));

        assertEquals(8, targetFormats.length);
    }

    @Test
    public void testGetTargetFormatsUnsigned() {
        final AudioFormat[] targetFormats = new FFFormatConversionProvider().getTargetFormats(AudioFormat.Encoding.PCM_UNSIGNED, new AudioFormat(FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(), 22050f, 16, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true));
        System.out.println("Formats: " + targetFormats.length);
        System.out.println(Arrays.asList(targetFormats));

        final Set<AudioFormat> audioFormats = new HashSet<AudioFormat>(Arrays.asList(targetFormats));

        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 8,  MONO,      1, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 8,  STEREO,    2, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 16, MONO,      2, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 16, STEREO,    4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 24, MONO,      4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 24, STEREO,    8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 32, MONO,      4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, AudioSystem.NOT_SPECIFIED, 32, STEREO,    8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));

        assertEquals(8, targetFormats.length);
    }

    @Test
    public void testGetTargetFormatsFloats() {
        final AudioFormat[] targetFormats = new FFFormatConversionProvider().getTargetFormats(FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(), new AudioFormat(FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(), 22050f, 16, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true));
        System.out.println("Formats: " + targetFormats.length);
        System.out.println(Arrays.asList(targetFormats));

        final Set<AudioFormat> audioFormats = new HashSet<AudioFormat>(Arrays.asList(targetFormats));

        assertTrue(containsWithMatches(audioFormats, new AudioFormat(FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(), AudioSystem.NOT_SPECIFIED, 32, MONO,      4, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(), AudioSystem.NOT_SPECIFIED, 32, STEREO,    8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(), AudioSystem.NOT_SPECIFIED, 64, MONO,      8, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));
        assertTrue(containsWithMatches(audioFormats, new AudioFormat(FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(), AudioSystem.NOT_SPECIFIED, 64, STEREO,    16, AudioSystem.NOT_SPECIFIED, NATIVE_ORDER)));

        assertEquals(4, targetFormats.length);
    }

    @Test
    public void testGetUnsupportedTargetFormats() {
        final AudioFormat[] targetFormatsEncodingAAC = new FFFormatConversionProvider().getTargetFormats(FFAudioFormat.FFEncoding.Codec.MPEG4_AAC.getEncoding(), new AudioFormat(FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(), 22050f, 16, STEREO, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true));
        assertEquals(0, targetFormatsEncodingAAC.length);
    }

    /**
     * AudioFormat uses {@link AudioFormat#matches(javax.sound.sampled.AudioFormat)} instead of
     * {@link Object#equals(Object)} for comparison - therefore we need a special <code>contains()</code>
     * method. This is ineffecient, but good enough for tests.
     *
     * @param set audioformats
     * @param audioFormat potential member
     * @return true or false
     */
    private boolean containsWithMatches(final Set<AudioFormat> set, final AudioFormat audioFormat) {
        for (final AudioFormat af : set) {
            if (af.matches(audioFormat)) return true;
        }
        return false;
    }
}
