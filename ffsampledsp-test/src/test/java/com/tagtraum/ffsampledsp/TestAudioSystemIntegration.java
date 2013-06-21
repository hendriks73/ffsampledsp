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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;

import static org.junit.Assert.*;

/**
 * TestAudioSystemIntegration.
 * <p/>
 * Date: 8/25/11
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
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(file);
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Bytes read: " + bytesRead);
    }

    @Test
    public void testAudioFileReader2() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testAudioFileReader", filename);
        extractFile(filename, file);

        int bytesRead = 0;
        AudioInputStream in = null;
        try {
            final AudioInputStream flacStream = AudioSystem.getAudioInputStream(file);
            in = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, flacStream);
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Bytes read: " + bytesRead);
    }

    private void extractFile(final String filename, final File file) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getClass().getResourceAsStream(filename);
            out = new FileOutputStream(file);
            final byte[] buf = new byte[1024*64];
            int justRead;
            while ((justRead = in.read(buf)) != -1) {
                out.write(buf, 0, justRead);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
