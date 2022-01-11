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

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

import static com.tagtraum.ffsampledsp.TestFFURLInputStream.extractFile;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TestAudioSystemIntegration.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestAudioSystemIntegration {


    @Test
    public void testAudioFileFormat() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testAudioFileFormat", filename);
        extractFile(filename, file);
        try {
            AudioSystem.getAudioFileFormat(file);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testAudioFileReader() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testAudioFileReader", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        try (final AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            file.delete();
        }
        System.out.println("Bytes read: " + bytesRead);
    }

    @Test
    public void testAudioFileReader2() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testAudioFileReader2", filename);
        extractFile(filename, file);
        final AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(file);
        final long duration = (Long)audioFileFormat.getProperty("duration");
        final AudioFormat targetFormat = new AudioFormat(PCM_SIGNED, 44100f, 16, 2, 4, 44100f, true);

        int bytesRead = 0;
        try (final AudioInputStream in = AudioSystem.getAudioInputStream(PCM_SIGNED, AudioSystem.getAudioInputStream(file))) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            file.delete();
        }
        System.out.println("Bytes read: " + bytesRead);
        final int expectedBytes = (int)Math.ceil(targetFormat.getFrameRate() * duration / 1000L / 1000L * targetFormat.getFrameSize());
        assertEquals(expectedBytes, bytesRead);
    }

    @Test
    public void testAudioFileReader3() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testAudioFileReader3", filename);
        extractFile(filename, file);
        final AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(file);
        final long duration = (Long)audioFileFormat.getProperty("duration");

        final AudioFormat targetFormat = new AudioFormat(PCM_SIGNED, 44100f, 16, 2, 4, 44100f, true);
        int bytesRead = 0;
        try (final AudioInputStream in = AudioSystem.getAudioInputStream(targetFormat, AudioSystem.getAudioInputStream(file))) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            file.delete();
        }
        System.out.println("Bytes read: " + bytesRead);
        final int expectedBytes = (int)Math.ceil(targetFormat.getFrameRate() * duration / 1000L / 1000L * targetFormat.getFrameSize());
        assertEquals(expectedBytes, bytesRead);
    }
}
