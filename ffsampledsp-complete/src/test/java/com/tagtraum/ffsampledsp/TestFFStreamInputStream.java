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

import static org.junit.Assert.*;

import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.Test;

/**
 * TestFFStreamInputStream.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFStreamInputStream {

  @Test
  public void testReadThroughMP3File() throws IOException, UnsupportedAudioFileException {
    final String filename = "test.mp3";
    final File file = File.createTempFile("testReadThroughMP3File", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new FileInputStream(file));
      int justRead;
      final byte[] buf = new byte[1024 * 8];
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
    assertTrue(bytesRead != 0);
  }

  @Test
  public void testReadThroughOggFile() throws IOException, UnsupportedAudioFileException {
    final String filename = "test.ogg";
    final File file = File.createTempFile("testReadThroughOggFile", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new FileInputStream(file));
      int justRead;
      final byte[] buf = new byte[1024 * 8];
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
    assertTrue(bytesRead != 0);
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
      new FFStreamInputStream(new FileInputStream(file), audioFileFormats.length);
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
        FFStreamInputStream in = null;
        try {
          in = new FFStreamInputStream(new FileInputStream(file), i);
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
  public void testReadThroughM4AFile() throws IOException, UnsupportedAudioFileException {
    final String filename = "test.m4a";
    final File file = File.createTempFile("testReadThroughM4AFile", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new FileInputStream(file));
      int justRead;
      final byte[] buf = new byte[1024 * 8];
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
    assertTrue(bytesRead != 0);
  }

  @Test(expected = UnsupportedAudioFileException.class)
  public void testReadThroughDRMM4AFile() throws IOException, UnsupportedAudioFileException {
    final String filename = "test_drms.m4a";
    final File file = File.createTempFile("testReadThroughDRMM4AFile", filename);
    extractFile(filename, file);
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new FileInputStream(file));
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
  public void testReadThroughFLACFile() throws IOException, UnsupportedAudioFileException {
    final String filename = "test.flac";
    final File file = File.createTempFile("testReadThroughFLACFile", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new FileInputStream(file));
      int justRead;
      final byte[] buf = new byte[1024 * 8];
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
    assertTrue(bytesRead != 0);
  }

  @Test
  public void testReadThroughWaveFile() throws IOException, UnsupportedAudioFileException {
    final String filename = "test.wav";
    final File file = File.createTempFile("testReadThroughWaveFile", filename);
    extractFile(filename, file);

    // pre-computed reference values index 1024-50 to 1024 (excl.)
    final int[] referenceValues =
        new int[] {
          240, 255, 230, 255, 230, 255, 232, 255, 232, 255, 247, 255, 247, 255, 246, 255, 246, 255,
          235, 255, 235, 255, 250, 255, 250, 255, 13, 0, 13, 0, 15, 0, 15, 0, 39, 0, 39, 0, 87, 0,
          87, 0, 90, 0, 90, 0, 31, 0, 31, 0
        };

    int bytesRead = 0;
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new FileInputStream(file));
      int justRead;
      final byte[] buf = new byte[1024];
      while ((justRead = in.read(buf)) != -1) {
        assertTrue(justRead > 0);
        bytesRead += justRead;
        if (bytesRead == 1024) {
          for (int i = 0; i < 50; i++) {
            assertEquals(referenceValues[i], buf[i + (1024 - 50)] & 0xFF);
          }
        }
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
  public void testReadThroughWMAFile() throws IOException, UnsupportedAudioFileException {
    final String filename = "test.wma";
    final File file = File.createTempFile("testReadThroughWMAFile", filename);
    extractFile(filename, file);

    int bytesRead = 0;
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new FileInputStream(file));
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
    assertEquals(14583576, (bytesRead / 4));
  }

  @Test
  public void testBogusStream() throws IOException {
    final String filename = "test.wav";
    final File file = File.createTempFile("testBogusFile", filename);
    FileOutputStream out = new FileOutputStream(file);
    final Random random = new Random();
    for (int i = 0; i < 8 * 1024; i++) {
      out.write(random.nextInt());
    }
    out.close();
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new BufferedInputStream(new FileInputStream(file)));
      in.read(new byte[1024]);
      fail("Expected UnsupportedAudioFileException");
    } catch (UnsupportedAudioFileException e) {
      // expected this
      e.printStackTrace();
      assertTrue(
          e.toString().endsWith("(Operation not permitted)")
              || e.toString().endsWith("(Invalid data found when processing input)")
              || e.toString().endsWith("(End of file)")
              || e.toString().endsWith("(Invalid argument)")
              || e.toString().endsWith("(Invalid data found when processing input)")
              || e.toString().contains("Probe score too low"));
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
  public void testNotSeekable() throws IOException, UnsupportedAudioFileException {
    final String filename = "test.wav";
    final File file = File.createTempFile("testNotSeekable", filename);
    extractFile(filename, file);
    FFStreamInputStream in = null;
    try {
      in = new FFStreamInputStream(new BufferedInputStream(new FileInputStream(file)));
      assertFalse(in.isSeekable());
      in.read(new byte[1024 * 4]);
      in.seek(0, TimeUnit.MICROSECONDS);

    } catch (UnsupportedOperationException e) {
      // expected this
      e.printStackTrace();
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
  public void testIOExceptionWhileOpening() throws UnsupportedAudioFileException {
    assertIOExceptionReachesCaller("test.wav", 16);
  }

  @Test
  public void testIOExceptionWhileReading() throws UnsupportedAudioFileException {
    assertIOExceptionReachesCaller("test.wav", 128 * 1024);
  }

  @Test
  public void testIOExceptionWhileReadingMKV() throws UnsupportedAudioFileException {
    // the Matroska demuxer tries to resync after a read error and calls the
    // native read callback again, while the exception is still pending
    assertIOExceptionReachesCaller("test_pcm_vorbis.mkv", 40000);
  }

  /**
   * Decodes the given resource through a stream that fails after {@code failAfter} bytes and checks
   * that the original exception thrown by the stream reaches the caller.
   */
  private static void assertIOExceptionReachesCaller(final String resource, final long failAfter)
      throws UnsupportedAudioFileException {
    try (final FailingInputStream failing = new FailingInputStream(resource, failAfter);
        final FFStreamInputStream in = new FFStreamInputStream(failing)) {
      final byte[] buf = new byte[1024 * 8];
      while (in.read(buf) != -1) {
        // keep reading
      }
      fail("Expected IOException");
    } catch (IOException e) {
      assertEquals(FailingInputStream.MESSAGE, e.getMessage());
    }
  }

  /** Serves a test resource, but throws an {@link IOException} after a given number of bytes. */
  private static class FailingInputStream extends FilterInputStream {
    static final String MESSAGE = "Simulated read failure";
    private long remaining;

    FailingInputStream(final String resource, final long failAfter) {
      super(TestFFStreamInputStream.class.getResourceAsStream(resource));
      this.remaining = failAfter;
    }

    @Override
    public int read() throws IOException {
      if (remaining <= 0) throw new IOException(MESSAGE);
      remaining--;
      return super.read();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      if (remaining <= 0) throw new IOException(MESSAGE);
      final int justRead = super.read(b, off, (int) Math.min(len, remaining));
      if (justRead > 0) remaining -= justRead;
      return justRead;
    }
  }

  static void extractFile(final String filename, final File file) throws IOException {
    try (final InputStream in = TestFFStreamInputStream.class.getResourceAsStream(filename);
        OutputStream out = new FileOutputStream(file)) {
      final byte[] buf = new byte[1024 * 64];
      int justRead;
      while ((justRead = in.read(buf)) != -1) {
        out.write(buf, 0, justRead);
      }
    }
  }
}
