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
import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * TestFFURLInputStream.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFURLInputStream {


    @Test
    public void testReadThroughMP3File() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThroughMP3File", "test.mp3");
        assertEquals(1078272, bytesRead);
    }

    @Test
    public void testSkipThroughMP3File() throws IOException, UnsupportedAudioFileException {
        final int bytesSkipped = skipThroughFile("testSkipThroughMP3File", "test.mp3");
        assertEquals(1078272, bytesSkipped);
    }

    @Test
    public void testReadThroughVBRMP3File() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThroughVBRMP3File", "test_vbr130.mp3");
        assertEquals(1078272, bytesRead);
    }

    @Test
    public void testReadThroughCBRMP3File() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThroughCBRMP3File", "test_cbr256.mp3");
        assertEquals(1078272, bytesRead);
    }

    @Test
    public void testReadThroughM4AFile() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThroughM4AFile", "test.m4a");
        assertEquals(534528, bytesRead);
    }

    @Test
    public void testReadThroughCBRM4AFile() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThroughCBRM4AFile", "test_cbr.m4a");
        assertEquals(1073152, bytesRead);
    }

    @Test
    public void testReadThroughVBRM4AFile() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThroughVBRM4AFile", "test_vbr.m4a");
        assertEquals(1073152, bytesRead);
    }

    @Test
    public void testReadThrough48kCBRM4AFile() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThrough48kCBRM4AFile", "test_48k_cbr.m4a");
        assertEquals(1171456, bytesRead);
    }

    @Test
    public void testReadThrough48kVBRM4AFile() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThrough48kVBRM4AFile", "test_48k_vbr.m4a");
        assertEquals(1171456, bytesRead);
    }

    @Test
    public void testReadThrough48kWavFile() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThrough48kWavFile", "test_48k.wav");
        assertEquals(581800, bytesRead);
    }

    @Test
    public void testSkipThrough48kWavFile() throws IOException, UnsupportedAudioFileException {
        final int bytesSkipped = skipThroughFile("testSkipThrough48kWavFile", "test_48k.wav");
        assertEquals(581800, bytesSkipped);
    }

    @Test
    public void testReadThrough48kAppleLosslessFile() throws IOException, UnsupportedAudioFileException {
        final int bytesRead = readThroughFile("testReadThrough48kAppleLosslessFile", "test_48k_alac.m4a");
        assertEquals(581800, bytesRead);
    }

    @Test(expected = UnsupportedAudioFileException.class)
    public void testReadThroughM4PFile() throws IOException, UnsupportedAudioFileException {
        new FFURLInputStream(new URL("file://somefile.m4p"));
    }

    @Test
    public void testSeekAfterEOF() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testSeekAfterEOF", filename);
        extractFile(filename, file);

        final byte[] buf = new byte[534528]; // 10sec
        try (final FFURLInputStream in = new FFURLInputStream(file.toURI().toURL())) {
            int justRead = in.read(buf);
            assertEquals(534528, justRead);
            justRead = in.read(buf);
            assertEquals(-1, justRead);
            in.seek(2, TimeUnit.SECONDS);
            justRead = in.read(buf);
            assertTrue(justRead > 0);
            final int twoSecondsInBytes = 44100 * 2 * 4;
            assertEquals(buf.length - twoSecondsInBytes, justRead);
            in.seek(1, TimeUnit.SECONDS);
        } finally {
            file.delete();
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBadStreamIndex() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.stem.mp4";
        final File file = File.createTempFile("testReadThroughStemMP4File", filename);
        extractFile(filename, file);
        try {
            final URL url = file.toURI().toURL();
            final AudioFileFormat[] audioFileFormats = new FFAudioFileReader().getAudioFileFormats(url);
            System.out.println("Found " + audioFileFormats.length + " streams.");
            new FFURLInputStream(url, audioFileFormats.length);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testReadThroughStemMP4File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.stem.mp4";
        final File file = File.createTempFile("testReadThroughStemMP4File", filename);
        extractFile(filename, file);
        try {
            final URL url = file.toURI().toURL();
            final AudioFileFormat[] audioFileFormats = new FFAudioFileReader().getAudioFileFormats(url);

            System.out.println("Found " + audioFileFormats.length + " streams.");
            for (int i = 0; i < audioFileFormats.length; i++) {
                System.out.println("Reading stream " + i + " ...");
                int bytesRead = 0;
                FFURLInputStream in = null;
                try {
                    in = new FFURLInputStream(url, i);
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
                }
                System.out.println("Read " + bytesRead + " bytes.");
            }
        } finally {
            file.delete();
        }
    }

    @Test
    public void testSplitStemFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.stem.mp4";
        final File file = File.createTempFile("testSplitStemFile", filename);
        extractFile(filename, file);

        final String[] stemNames = {"master", "drums", "bass", "synths", "vox"};
        final File[] stems = new File[stemNames.length];
        try {
            final FFAudioFileReader ffAudioFileReader = new FFAudioFileReader();
            final int stemCount = ffAudioFileReader.getAudioFileFormats(file).length;
            System.out.println("Found " + stemCount + " stems.");
            for (int i = 0; i < stemCount; i++) {
                System.out.println("Reading stem " + i + " (" + stemNames[i] + ").");
                AudioInputStream in = null;
                try {
                    in = ffAudioFileReader.getAudioInputStream(file, i);
                    System.out.println("encoding = " + in.getFormat().getEncoding());
                    final AudioInputStream pcmIn = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, in);
                    stems[i] = File.createTempFile("testSplitStemFile", filename.replace(".stem.mp4", "." + stemNames[i] + ".wav"));
                    AudioSystem.write(pcmIn, AudioFileFormat.Type.WAVE, stems[i]);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } finally {
            System.out.println("Done.");

            file.delete();
            for (int i=0; i<stems.length; i++){
                if (stems[i] != null) stems[i].delete();
            }
        }
    }

    @Test
    public void testReadThroughOggFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.ogg";
        final File file = File.createTempFile("testReadThroughOggFile", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
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
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    public void testReadThroughFLACFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testReadThroughFLACFile", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
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
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    public void test24Bit() throws IOException, UnsupportedAudioFileException {
        final int pattern = Integer.parseInt("10101010", 2);
        final int first = Integer.parseInt("11111010", 2);
        final File file = File.createTempFile("special24bit", ".wav");
        int bytesRead = 0;
        FFURLInputStream in = null;
        try {
            // create file with exactly one sample
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(new byte[]{(byte) first, (byte) pattern, (byte) pattern,}),
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 24, 1, 3, 44100, false), 1), AudioFileFormat.Type.WAVE, file);
            in = new FFURLInputStream(file.toURI().toURL());
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;

                final int one = buf[0] & 0xFF;
                final int two = buf[1] & 0xFF;
                final int three = buf[2] & 0xFF;

                assertEquals("11111010", Integer.toBinaryString(one));
                assertEquals("10101010", Integer.toBinaryString(two));
                assertEquals("10101010", Integer.toBinaryString(three));
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
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(3, bytesRead);
    }

    @Test
    public void testReadThrough24bitWave() throws IOException, UnsupportedAudioFileException {
        final String filename = "test24bit.wav";
        final File file = File.createTempFile("testReadThrough24bitWave", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
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
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(801792, bytesRead);
    }

    @Test
    public void testReadThroughLong24bitWave() throws IOException, UnsupportedAudioFileException {
        final String filename = "test_long24bit.wav";
        final File file = File.createTempFile("testReadThroughLong24bitWave", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
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
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(16084608, bytesRead);
    }

    @Test
    public void testReadThroughWaveFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testReadThroughWaveFile", filename);
        extractFile(filename, file);

        // pre-computed reference values index 1024-50 to 1024 (excl.)
        final int[] referenceValues = new int[]{240, 255, 230, 255, 230, 255, 232, 255, 232, 255, 247, 255, 247, 255, 246, 255, 246, 255, 235, 255, 235, 255, 250, 255, 250, 255, 13, 0, 13, 0, 15, 0, 15, 0, 39, 0, 39, 0, 87, 0, 87, 0, 90, 0, 90, 0, 31, 0, 31, 0};

        int bytesRead = 0;
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
                if (bytesRead == 1024) {
                    for (int i=0; i<50; i++) {
                        System.out.println(referenceValues[i] + "\t=\t" + (buf[i + (1024 - 50)] & 0xFF));
                        //assertEquals(referenceValues[i], buf[i+(1024-50)] & 0xFF);
                    }
                }
                assertTrue((bytesRead / 4) <= 133632);
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
        assertEquals(133632, (bytesRead / 4));
    }

    @Test
    public void testReadThroughADPCMWave() throws IOException, UnsupportedAudioFileException {
        final String filename = "test_adpcm.wav";
        final File file = File.createTempFile("testReadThroughADPCMWave", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
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
        System.out.println("Read " + bytesRead + " bytes.");
        assertEquals(2657728, bytesRead);
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
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
            in.read(new byte[1024]);
            fail("Expected UnsupportedAudioFileException");
        } catch (UnsupportedAudioFileException e) {
            // expected this
            e.printStackTrace();
            assertTrue(e.toString().endsWith("(Operation not permitted)")
                    || e.toString().endsWith("(Invalid data found when processing input)")
                    || e.toString().endsWith("(End of file)")
                    || e.toString().endsWith("(Invalid argument)")
                    || e.toString().endsWith("(Invalid data found when processing input)")
                    || e.toString().contains("Probe score too low")
            );
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
    }


    @Test(expected = FileNotFoundException.class)
    public void testNonExistingFile() throws IOException, UnsupportedAudioFileException {
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(new File("/Users/hendrik/bcisdbvigfeir.wav").toURI().toURL());
            in.read(new byte[1024]);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Test(expected = IOException.class)
    public void testNonExistingURL() throws UnsupportedAudioFileException, IOException {
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(new URL("https://www.tagtraum.com/hendrik/bcisdbvigfeir.wav"));
            in.read(new byte[1024]);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testSeekBackwards() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testSeekBackwards", filename);
        extractFile(filename, file);
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.seek(0, TimeUnit.MICROSECONDS);
            in.read(new byte[1024 * 4]);
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
    }

    @Test
    public void testSeekForwards() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testSeekForwards", filename);
        extractFile(filename, file);
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.seek(1, TimeUnit.SECONDS);
            in.read(new byte[1024 * 4]);
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
    }

    @Test(expected = IOException.class)
    public void testSeekAfterClose() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.flac";
        final File file = File.createTempFile("testSeekAfterClose", filename);
        extractFile(filename, file);
        FFURLInputStream in = null;
        try {
            in = new FFURLInputStream(file.toURI().toURL());
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.close();
            in.seek(1, TimeUnit.SECONDS);
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
    }

    static void extractFile(final String filename, final File file) throws IOException {
        try (final InputStream in = TestFFURLInputStream.class.getResourceAsStream(filename);
             OutputStream out = new FileOutputStream(file)) {
            final byte[] buf = new byte[1024*64];
            int justRead;
            while ((justRead = in.read(buf)) != -1) {
                out.write(buf, 0, justRead);
            }
        }
    }


    private int readThroughFile(final String prefix, final String filename) throws IOException, UnsupportedAudioFileException {
        final File file = File.createTempFile(prefix, filename);
        extractFile(filename, file);
        int bytesRead = 0;

        final AudioFileFormat audioFileFormat = new FFAudioFileReader().getAudioFileFormat(file);

        try (final FFAudioInputStream in = new FFAudioInputStream(new FFURLInputStream(file.toURI().toURL()), audioFileFormat.getFormat(), audioFileFormat.getFrameLength())) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            file.delete();
        }
        System.out.println("Read " + bytesRead + " bytes.");
        return bytesRead;
    }

    private int skipThroughFile(final String prefix, final String filename) throws IOException, UnsupportedAudioFileException {
        final File file = File.createTempFile(prefix, filename);
        extractFile(filename, file);
        int bytesSkipped = 0;

        final AudioFileFormat audioFileFormat = new FFAudioFileReader().getAudioFileFormat(file);
        long bytesToSkip = 1024 * 32;
        if (audioFileFormat.getFormat().getFrameSize() > 0) {
            bytesToSkip = audioFileFormat.getFormat().getFrameSize() * 100L;
        }

        try (final FFAudioInputStream in = new FFAudioInputStream(new FFURLInputStream(file.toURI().toURL()), audioFileFormat.getFormat(), audioFileFormat.getFrameLength())) {
            long justSkipped;
            while ((justSkipped = in.skip(bytesToSkip)) != 0) {
                bytesSkipped += justSkipped;
            }
        } finally {
            file.delete();
        }
        System.out.println("Skipped " + bytesSkipped + " bytes.");
        return bytesSkipped;
    }
}
