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
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * TestFFCodecInputStream.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFCodecInputStream {

    @Test
    public void testSeek() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testSeek", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100/2f, 16, 2, 4, 44100/2f, false);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            if (targetStream.isSeekable()) {
                targetStream.seek(0, TimeUnit.MICROSECONDS);
                targetStream.seek(1, TimeUnit.SECONDS);
            }

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
            // at the end read another byte
            assertEquals(-1, targetStream.read());

        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(179064, bytesRead);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAlawEncoding() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.ALAW, 44100f, 16, 2, 4, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPCMUnsignedIllegalSampleSize() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 44100f, 9, 2, 4, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPCMSignedIllegalSampleSize() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 9, 2, 4, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPCMFloatIllegalSampleSize() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_FLOAT, 44100f, 9, 2, 4, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegal0Channels() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 8, 0, 2, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegal3Channels() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 8, 3, 2, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFrameSizeSample8() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 8, 2, 5, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFrameSizeSample16() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 5, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFrameSizeSample24() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 24, 2, 5, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFrameSizeSample32() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 32, 2, 5, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFrameSizeSample64() throws IOException, UnsupportedAudioFileException {
        final AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 64, 2, 5, 44100f, false);
        new FFCodecInputStream(targetFormat, null);
    }

    @Test
    public void testReadConvert24to16Bit() throws IOException, UnsupportedAudioFileException {
        final String filename = "test24bit.wav";
        final File file = File.createTempFile("testReadConvert24to16Bit", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream wav16bitStream = null;
        try (final AudioInputStream wav24bitStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 96000, 16, 2, 4, 96000, false);
            System.err.println("24bit: " + wav24bitStream.getFormat());
            System.err.println("16bit: " + targetFormat);
            wav16bitStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) wav24bitStream);

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = wav16bitStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }

        } finally {
            if (wav16bitStream != null) {
                try {
                    wav16bitStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    public void testReadConvertMP3FileToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadConvertMP3FileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream mp3Stream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("mp3: " + mp3Stream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) mp3Stream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }

        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(539136, bytesRead);
    }

    @Test
    public void testReadConvertM4AFileToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.m4a"; // apple lossless
        final File file = File.createTempFile("testReadConvertM4AFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream m4aStream = new FFAudioFileReader().getAudioInputStream(file)) {
            ;
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("m4a: " + m4aStream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) m4aStream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentestm4a.wav"));

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(534528, bytesRead);
    }

    @Test
    public void testReadConvertFLACFileToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testReadConvertFLACFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream flacStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("flac: " + flacStream.getFormat());
            System.err.println("pcm : " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) flacStream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }

        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(534528, bytesRead);
    }

    @Test
    public void testDownsampleWaveFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testDownsampleWaveFile", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100/2, 16, 2, 4, 44100/2, false);
            System.err.println("44.100: " + sourceStream.getFormat());
            System.err.println("22.050: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(267264, bytesRead);
    }

    @Test
    public void testDownsampleMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testDownsampleMP3File", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100/2, 16, 2, 4, 44100/2, false);
            System.err.println("44.100: " + sourceStream.getFormat());
            System.err.println("22.050: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }

        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(269568, bytesRead);
    }

    @Test
    public void testDownsampleOggFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.ogg";
        final File file = File.createTempFile("testDownsampleOggFile", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100/2, 16, 2, 4, 44100/2, false);
            System.err.println("44.100: " + sourceStream.getFormat());
            System.err.println("22.050: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }

        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(268160, bytesRead);
    }

    @Test
    public void testDownsampleMP3File2() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testDownsampleMP3File", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100/4*3, 16, 2, 4, 44100/4*3, false);
            System.err.println("44.100: " + sourceStream.getFormat());
            System.err.println("33.075: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(404352, bytesRead);
    }

    @Test
    public void testDownsampleOggFile2() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.ogg";
        final File file = File.createTempFile("testDownsampleOggFile", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100/4*3, 16, 2, 4, 44100/4*3, false);
            System.err.println("44.100: " + sourceStream.getFormat());
            System.err.println("33.075: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));

            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(402240, bytesRead);
    }

    @Test
    public void testReadConvertMP3StreamToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadConvertMP3FileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream mp3Stream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("mp3: " + mp3Stream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) mp3Stream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(539136, bytesRead);
    }

    @Test
    public void testReadConvertOggStreamToPCM() throws IOException {
        final String filename = "test.ogg";
        final File file = File.createTempFile("testReadConvertOggFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream oggStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("ogg: " + oggStream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) oggStream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } catch (Exception e) {
            fail(e.toString());

        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(536320, bytesRead);
    }

    @Test
    public void testReadConvertM4AStreamToPCMAndDownsample() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.m4a";
        final File file = File.createTempFile("testReadConvertM4AFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        FFCodecInputStream downStream = null;
        try (final AudioInputStream m4aStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat pcmTargetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("m4a: " + m4aStream.getFormat());
            System.err.println("pcm: " + pcmTargetFormat);
            pcmStream = new FFCodecInputStream(pcmTargetFormat, (FFAudioInputStream) m4aStream);

            final AudioFormat downTargetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 22050, 16, 2, 4, 22050, false);
            System.err.println("dwn: " + downTargetFormat);
            final FFAudioInputStream pcmAudioInputStream = new FFAudioInputStream(pcmStream, pcmTargetFormat, -1);
            downStream = new FFCodecInputStream(downTargetFormat, pcmAudioInputStream);

            //AudioSystem.write(new FFAudioInputStream(downStream, downTargetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = downStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (downStream != null) {
                try {
                    downStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(267264, bytesRead);
    }

    @Test
    public void testReadConvertFLACStreamToPCMAndDownsample() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testReadConvertFLACFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        FFCodecInputStream downStream = null;
        try (final AudioInputStream flacStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat pcmTargetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("flac: " + flacStream.getFormat());
            System.err.println("pcm : " + pcmTargetFormat);
            pcmStream = new FFCodecInputStream(pcmTargetFormat, (FFAudioInputStream) flacStream);

            final AudioFormat downTargetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 22050, 16, 2, 4, 22050, false);
            System.err.println("down: " + downTargetFormat);
            final FFAudioInputStream pcmAudioInputStream = new FFAudioInputStream(pcmStream, pcmTargetFormat, -1);
            downStream = new FFCodecInputStream(downTargetFormat, pcmAudioInputStream);

            //AudioSystem.write(new FFAudioInputStream(downStream, downTargetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = downStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (downStream != null) {
                try {
                    downStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(267264, bytesRead);
    }

    @Test
    public void testReadConvertM4AStreamToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.m4a";
        final File file = File.createTempFile("testReadConvertM4AFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream m4aStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("m4a: " + m4aStream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) m4aStream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(534528, bytesRead);
    }

    @Test
    public void testReadConvertFLACStreamToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testReadConvertFLACFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream flacStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("flac: " + flacStream.getFormat());
            System.err.println("pcm : " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) flacStream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(534528, bytesRead);
    }

    @Test
    public void testReadConvertWaveStreamToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testReadConvertWaveStreamToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream mp3Stream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("wave: " + mp3Stream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) mp3Stream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(534528, bytesRead);
    }

    @Test
    public void testReadConvertWaveStreamToUnsignedPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testReadConvertWaveStreamToUnsignedPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream mp3Stream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_UNSIGNED, 44100, 16, 2, 4, 44100, false);
            System.err.println("wave: " + mp3Stream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) mp3Stream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(534528, bytesRead);
    }

    @Test
    public void testReadConvertWaveStreamToFloatPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testReadConvertWaveStreamToUnsignedPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream mp3Stream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_FLOAT, 44100, 32, 2, 8, 44100, false);
            System.err.println("wave: " + mp3Stream.getFormat());
            System.err.println("pcm: " + targetFormat);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) mp3Stream);

            //AudioSystem.write(new FFAudioInputStream(pcmStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = pcmStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(1069056, bytesRead);
    }

    @Test
    public void testConvertWaveFileTo24bit() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testConvertWaveFileTo24bit", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100, 24, 2, 6, 44100, false);
            System.err.println("16bit: " + sourceStream.getFormat());
            System.err.println("24bit: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(801792, bytesRead);
    }


    @Test
    public void testDownsampleWaveStream() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testDownsampleWaveStream", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100/2, 16, 2, 4, 44100/2, false);
            System.err.println("44.100: " + sourceStream.getFormat());
            System.err.println("22.050: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(267264, bytesRead);
    }

    @Test
    public void testUpsampleWaveStream() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testUpsampleWaveStream", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFCodecInputStream targetStream = null;
        try (final AudioInputStream sourceStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 48000f, 16, 2, 4, 48000, false);
            System.err.println("44.100: " + sourceStream.getFormat());
            System.err.println("48.000: " + targetFormat);
            targetStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) sourceStream);

            //AudioSystem.write(new FFAudioInputStream(targetStream, targetFormat, -1), AudioFileFormat.Type.WAVE, new File("writtentest.wav"));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = targetStream.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }


        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(581800, bytesRead);
    }

    @Test(expected = IOException.class)
    public void testSeekClosedStream() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testSeekClosedStream", filename);
        extractFile(filename, file);
        FFCodecInputStream pcmStream = null;
        try (final AudioInputStream mp3Stream = new FFAudioFileReader().getAudioInputStream(file)) {
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false);
            pcmStream = new FFCodecInputStream(targetFormat, (FFAudioInputStream) mp3Stream);
            assertTrue(pcmStream.isSeekable());
        } finally {
            if (pcmStream != null) {
                try {
                    pcmStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            file.delete();
        }
        // now seek in the already closed stream.
        pcmStream.seek(500, TimeUnit.SECONDS);
    }

    private void extractFile(final String filename, final File file) throws IOException {
        try (final InputStream in = getClass().getResourceAsStream(filename);
             final OutputStream out = new FileOutputStream(file)) {
            final byte[] buf = new byte[1024*64];
            int justRead;
            while ((justRead = in.read(buf)) != -1) {
                out.write(buf, 0, justRead);
            }
        }
    }
}
