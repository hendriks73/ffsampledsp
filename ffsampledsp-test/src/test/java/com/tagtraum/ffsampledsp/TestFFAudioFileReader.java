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

import org.junit.Ignore;
import org.junit.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * TestFFAudioFileReader.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFAudioFileReader {

    @Test
    public void testGetAudioFileFormatFileAIFF() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.aiff";
        final File file = File.createTempFile("testGetAudioFileFormatFileAIFF", filename);
        extractFile(filename, file);

        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("aiff", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());
            final AudioFormat format = fileFormat.getFormat();
            assertEquals(true, format.isBigEndian());
            assertEquals(2, format.getChannels());
            assertEquals(16, format.getSampleSizeInBits());
            assertEquals(44100f, format.getSampleRate(), 0.0001f);
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100f, format.getFrameRate(), 0.001f);
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull(bitrate);
            assertEquals(16*2*44100, (int)bitrate);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatM4AFile() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.m4a"; // apple lossless
        final File file = File.createTempFile("testGetAudioFileFormatM4AFile", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("m4a", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(-1, format.getFrameSize());
            assertEquals(16, format.getSampleSizeInBits());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long) duration);
            assertEquals(10.890356f, format.getFrameRate(), 0.001f);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatStemMP4File() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.stem.mp4"; // stem
        final File file = File.createTempFile("testGetAudioFileFormatStemMP4File", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat[] fileFormats = new FFAudioFileReader().getAudioFileFormats(file);
            assertEquals(5, fileFormats.length);
            for (final AudioFileFormat fileFormat : fileFormats) {
                System.out.println(fileFormat);

                assertEquals("mp4", fileFormat.getType().getExtension());
                assertEquals(file.length(), fileFormat.getByteLength());
                assertEquals(133632, fileFormat.getFrameLength());

                final AudioFormat format = fileFormat.getFormat();
                assertEquals(-1, format.getFrameSize());
                assertEquals(2, format.getChannels());
                final Long duration = (Long) fileFormat.getProperty("duration");
                assertNotNull(duration);
                assertEquals(3030204, (long) duration);
                assertEquals(10.890356f, format.getFrameRate(), 0.001f);
            }
        } finally {
            file.delete();
        }
    }

    @Test
    @Ignore("Can only work with FFmpeg package that supports mp3.")
    public void testGetAudioFileFormatMP3File() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.mp3";
        final File file = File.createTempFile("testGetAudioFileFormatMP3File", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("mp3", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(143251, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(-1, format.getFrameSize());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3248333, (long)duration);
            assertEquals(38.28125f, format.getFrameRate(), 0.001f);
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull(bitrate);
            assertEquals(192000, (int)bitrate);
        } finally {
            //file.delete();
        }
    }

    /**
     * Try to load a file with a format that is unsupported.
     * This test does not work properly, if mp3 is actually supported!
     *
     * @throws IOException if there is som IO error
     */
    @Test
    public void testGetAudioFileFormatLowProbeScoreFile() throws IOException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.mp3";
        final File file = File.createTempFile("testGetAudioFileFormatMP3File", filename);
        extractFile(filename, file);
        try {
            new FFAudioFileReader().getAudioFileFormat(file);
            fail("Expected UnsupportedAudioFileException as mp3 is unsupported.");
        } catch (UnsupportedAudioFileException e) {
            // we want to test for the specific error message
            assertTrue(e.toString().contains("Invalid data found"));
        } finally {
            file.delete();
        }
    }

    /**
     * Try to load a file with a format that is unsupported.
     * This test does not work properly, if mp3 is actually supported!
     *
     * @throws IOException if there is some IO error
     */
    @Test
    public void testGetAudioFileFormatLowProbeScoreFile2() throws IOException {
        // first copy the file from resources to actual location in temp
        final File file = new File("/Users/hendrik/downloads/The Early Access App [Questions #4688]/Oh My Love.abc");
        try {
            final AudioFileFormat audioFileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(audioFileFormat);
        } catch (UnsupportedAudioFileException e) {
            // we want to test for the specific error message
            e.printStackTrace();
            assertTrue(e.toString().contains("Probe score too low"));
        }
    }

    @Test
    public void testGetAudioFileFormatFLACFile() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.flac";
        final File file = File.createTempFile("testGetAudioFileFormatFLACFile", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("flac", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(-1, format.getFrameSize());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100.0f, format.getFrameRate(), 0.001f);
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNull("Expected bitrate to be missing, but it is not: " + bitrate, bitrate);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatOggFile() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.ogg";
        final File file = File.createTempFile("testGetAudioFileFormatOggCFile", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("ogg", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(-1, format.getFrameSize());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100.0f, format.getFrameRate(), 0.001f);
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull("Bitrate missing", bitrate);
            assertEquals(112000, (int)bitrate);
        } finally {
            file.delete();
        }
    }
    @Test
    public void testGetAudioFileFormatSpacesUmlautsPunctuation() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.ogg";
        final File file = File.createTempFile("testGetAudioFileFormatSpacesAndUmlauts-t\u00fcst file [;:&= @[]?]", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("ogg", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(-1, format.getFrameSize());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100.0f, format.getFrameRate(), 0.001f);
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull("Bitrate missing", bitrate);
            assertEquals(112000, (int)bitrate);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatURL() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.ogg";
        final File file = File.createTempFile("testGetAudioFileFormatURL", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file.toURI().toURL());
            System.out.println(fileFormat);

            assertEquals("ogg", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(-1, format.getFrameSize());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100.0f, format.getFrameRate(), 0.001f);
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull("Bitrate missing", bitrate);
            assertEquals(112000, (int) bitrate);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatInputStream() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.ogg";
        final File file = File.createTempFile("testGetAudioFileFormatInputStream", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(new BufferedInputStream(new FileInputStream(file)));
            System.out.println(fileFormat);

            assertEquals("ogg", fileFormat.getType().getExtension());
            assertEquals(AudioSystem.NOT_SPECIFIED, fileFormat.getByteLength());
            assertEquals(AudioSystem.NOT_SPECIFIED, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(2, format.getChannels());
            assertEquals(-1, format.getFrameSize());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNull(duration);
            assertEquals(44100.0f, format.getFrameRate(), 0.00001f);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testNotExistingURL() throws IOException, UnsupportedAudioFileException {
        final File file = new File("test" + System.currentTimeMillis()+ ".wav");
        try {
            new FFAudioFileReader().getAudioFileFormat(file.toURI().toURL());
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException e) {
            // expected this
        }
    }

    @Test
    public void testBogusFile() throws IOException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testBogusFile", filename);
        FileOutputStream out = new FileOutputStream(file);
        final Random random = new Random();
        for (int i=0; i<8*1024; i++) {
            out.write(random.nextInt());
        }
        out.close();
        try {
            new FFAudioFileReader().getAudioFileFormat(file.toURI().toURL());
            fail("Expected UnsupportedAudioFileException");
        } catch (UnsupportedAudioFileException e) {
            // expected this
            e.printStackTrace();
            assertTrue(e.toString().endsWith("(Operation not permitted)")
                    || e.toString().endsWith("(Invalid data found when processing input)")
                    || e.toString().endsWith("(End of file)")
                    || e.toString().endsWith("(Invalid data found when processing input)")
                    || e.toString().contains("Probe score too low")
            );
        } finally {
            file.delete();
        }
    }

    @Test
    public void testFileWithPunctuationToURL() throws MalformedURLException {
        final File file = new File("/someDir/;:&=+@[]?/name.txt");
        final URL url = FFAudioFileReader.fileToURL(file);
        assertEquals("file:/someDir/;:&=+@[]?/name.txt", url.toString());
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

    @Test
    public void testGetAudioFileFormatFile24bitWave() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test24bit.wav";
        final File file = File.createTempFile("testGetAudioFileFormatFile24bitWave", filename);
        extractFile(filename, file);

        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("wav", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());
            final AudioFormat format = fileFormat.getFormat();
            assertEquals("PCM_SIGNED", format.getEncoding().toString());
            assertEquals(6, format.getFrameSize());
            assertEquals(false, format.isBigEndian());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100f, format.getSampleRate(), 0.001f);
            assertEquals(44100f, format.getFrameRate(), 0.001f);
            assertEquals(24, format.getSampleSizeInBits());
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull(bitrate);
            assertEquals(2116800, (int)bitrate);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatFile24bitFLAC() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test24bit.flac";
        final File file = File.createTempFile("testGetAudioFileFormatFile24bitFLAC", filename);
        extractFile(filename, file);

        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("flac", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());
            final AudioFormat format = fileFormat.getFormat();
            assertEquals("FLAC", format.getEncoding().toString());
            assertEquals(-1, format.getFrameSize());
            assertEquals(false, format.isBigEndian());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100f, format.getSampleRate(), 0.001f);
            assertEquals(44100f, format.getFrameRate(), 0.001f);
            assertEquals(24, format.getSampleSizeInBits());
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNull("Expected bitrate to be missing, but it is not: " + bitrate, bitrate);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatFileW64() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.w64";
        final File file = File.createTempFile("testGetAudioFileFormatFileW64", filename);
        extractFile(filename, file);

        try {
            final AudioFileFormat fileFormat = new FFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("w64", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133632, fileFormat.getFrameLength());
            final AudioFormat format = fileFormat.getFormat();
            assertEquals("PCM_FLOAT", format.getEncoding().toString());
            assertEquals(false, format.isBigEndian());
            assertEquals(16, format.getFrameSize());
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030204, (long)duration);
            assertEquals(44100f, format.getSampleRate(), 0.001f);
            assertEquals(44100f, format.getFrameRate(), 0.001f);
            assertEquals(64, format.getSampleSizeInBits());
            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull(bitrate);
            assertEquals(5644800, (int)bitrate);
        } finally {
            file.delete();
        }
    }
}
