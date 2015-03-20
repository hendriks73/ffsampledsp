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

import javax.sound.sampled.*;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TestFFCodecInputStream.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFCodecInputStream {

    @Test
    public void testReadConvert24to16Bit() throws IOException, UnsupportedAudioFileException {
        final String filename = "test24bit.wav";
        final File file = File.createTempFile("testReadConvert24to16Bit", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream wav24bitStream = null;
        FFCodecInputStream wav16bitStream = null;
        try {
            wav24bitStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (wav24bitStream != null) {
                try {
                    wav24bitStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    @Ignore("Can only work with FFmpeg package that supports mp3.")
    public void testReadConvertMP3FileToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadConvertMP3FileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream mp3Stream = null;
        FFCodecInputStream pcmStream = null;
        try {
            mp3Stream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (mp3Stream != null) {
                try {
                    mp3Stream.close();
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
        AudioInputStream m4aStream = null;
        FFCodecInputStream pcmStream = null;
        try {
            m4aStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (m4aStream != null) {
                try {
                    m4aStream.close();
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
        AudioInputStream flacStream = null;
        FFCodecInputStream pcmStream = null;
        try {
            flacStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (flacStream != null) {
                try {
                    flacStream.close();
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
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
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
    @Ignore("Can only work with FFmpeg package that supports mp3.")
    public void testDownsampleMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testDownsampleMP3File", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
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
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
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
    @Ignore("Can only work with FFmpeg package that supports mp3.")
    public void testDownsampleMP3File2() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testDownsampleMP3File", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
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
    public void testDownsampleOggFile2() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.ogg";
        final File file = File.createTempFile("testDownsampleOggFile", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
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
    @Ignore("Can only work with FFmpeg package that supports mp3.")
    public void testReadConvertMP3StreamToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadConvertMP3FileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream mp3Stream = null;
        FFCodecInputStream pcmStream = null;
        try {
            mp3Stream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (mp3Stream != null) {
                try {
                    mp3Stream.close();
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
    public void testReadConvertOggStreamToPCM() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.ogg";
        final File file = File.createTempFile("testReadConvertOggFileToPCM", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream oggStream = null;
        FFCodecInputStream pcmStream = null;
        try {
            oggStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (oggStream != null) {
                try {
                    oggStream.close();
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
        AudioInputStream m4aStream = null;
        FFCodecInputStream pcmStream = null;
        FFCodecInputStream downStream = null;
        try {
            m4aStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (m4aStream != null) {
                try {
                    m4aStream.close();
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
        AudioInputStream flacStream = null;
        FFCodecInputStream pcmStream = null;
        FFCodecInputStream downStream = null;
        try {
            flacStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (flacStream != null) {
                try {
                    flacStream.close();
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
        AudioInputStream m4aStream = null;
        FFCodecInputStream pcmStream = null;
        try {
            m4aStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (m4aStream != null) {
                try {
                    m4aStream.close();
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
        AudioInputStream flacStream = null;
        FFCodecInputStream pcmStream = null;
        try {
            flacStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (flacStream != null) {
                try {
                    flacStream.close();
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
        AudioInputStream mp3Stream = null;
        FFCodecInputStream pcmStream = null;
        try {
            mp3Stream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (mp3Stream != null) {
                try {
                    mp3Stream.close();
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
    public void testConvertWaveFileTo24bit() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testConvertWaveFileTo24bit", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(file);
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
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
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
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
        AudioInputStream sourceStream = null;
        FFCodecInputStream targetStream = null;
        try {
            sourceStream = new FFAudioFileReader().getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
            final AudioFormat targetFormat = new AudioFormat(FFAudioFormat.FFEncoding.PCM_SIGNED, 48000, 16, 2, 4, 48000, false);
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
            if (sourceStream != null) {
                try {
                    sourceStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(581792, bytesRead);
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
