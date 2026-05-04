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

import static com.tagtraum.ffsampledsp.FFFormatConversionProvider.*;
import static com.tagtraum.ffsampledsp.TestFFURLInputStream.extractFile;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.Test;

/**
 * TestFFFormatConversionProvider.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFFormatConversionProvider {

  @Test
  public void testGetSourceEncodings() {
    final Set<AudioFormat.Encoding> sourceEncodings =
        new HashSet<>(Arrays.asList(new FFFormatConversionProvider().getSourceEncodings()));
    assertTrue(sourceEncodings.contains(FFAudioFormat.FFEncoding.getInstance("GSM")));
  }

  @Test
  public void testGetTargetEncodings() {
    final Set<AudioFormat.Encoding> targetEncodings =
        new HashSet<>(Arrays.asList(new FFFormatConversionProvider().getTargetEncodings()));
    assertFalse(targetEncodings.contains(FFAudioFormat.FFEncoding.getInstance("GSM")));
    assertTrue(targetEncodings.contains(FFAudioFormat.FFEncoding.getInstance("PCM_UNSIGNED")));
    assertTrue(targetEncodings.contains(FFAudioFormat.FFEncoding.getInstance("PCM_SIGNED")));
    assertTrue(targetEncodings.contains(FFAudioFormat.FFEncoding.getInstance("PCM_FLOAT")));
  }

  @Test
  public void testGetTargetFormatsSigned() {
    final AudioFormat[] targetFormats =
        new FFFormatConversionProvider()
            .getTargetFormats(
                AudioFormat.Encoding.PCM_SIGNED,
                new AudioFormat(
                    FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(),
                    22050f,
                    16,
                    2,
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    true));
    System.out.println("Formats: " + targetFormats.length);
    System.out.println(Arrays.asList(targetFormats));

    final Set<AudioFormat> audioFormats = new HashSet<AudioFormat>(Arrays.asList(targetFormats));

    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                8,
                MONO,
                1,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                8,
                STEREO,
                2,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                16,
                MONO,
                2,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                16,
                STEREO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                24,
                MONO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                24,
                STEREO,
                8,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                32,
                MONO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                32,
                STEREO,
                8,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));

    assertEquals(8, targetFormats.length);
  }

  @Test
  public void testGetTargetFormatsUnsigned() {
    final AudioFormat[] targetFormats =
        new FFFormatConversionProvider()
            .getTargetFormats(
                AudioFormat.Encoding.PCM_UNSIGNED,
                new AudioFormat(
                    FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(),
                    22050f,
                    16,
                    2,
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    true));
    System.out.println("Formats: " + targetFormats.length);
    System.out.println(Arrays.asList(targetFormats));

    final Set<AudioFormat> audioFormats = new HashSet<AudioFormat>(Arrays.asList(targetFormats));

    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                8,
                MONO,
                1,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                8,
                STEREO,
                2,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                16,
                MONO,
                2,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                16,
                STEREO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                24,
                MONO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                24,
                STEREO,
                8,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                32,
                MONO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                AudioSystem.NOT_SPECIFIED,
                32,
                STEREO,
                8,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));

    assertEquals(8, targetFormats.length);
  }

  @Test
  public void testGetTargetFormatsFloats() {
    final AudioFormat[] targetFormats =
        new FFFormatConversionProvider()
            .getTargetFormats(
                FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(),
                new AudioFormat(
                    FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(),
                    22050f,
                    16,
                    2,
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    true));
    System.out.println("Formats: " + targetFormats.length);
    System.out.println(Arrays.asList(targetFormats));

    final Set<AudioFormat> audioFormats = new HashSet<AudioFormat>(Arrays.asList(targetFormats));

    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(),
                AudioSystem.NOT_SPECIFIED,
                32,
                MONO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(),
                AudioSystem.NOT_SPECIFIED,
                32,
                STEREO,
                8,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(),
                AudioSystem.NOT_SPECIFIED,
                64,
                MONO,
                8,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                FFAudioFormat.FFEncoding.Codec.PCM_FLOAT.getEncoding(),
                AudioSystem.NOT_SPECIFIED,
                64,
                STEREO,
                16,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));

    assertEquals(4, targetFormats.length);
  }

  @Test
  public void testGetUnsupportedTargetFormats() {
    final AudioFormat[] targetFormatsEncodingAAC =
        new FFFormatConversionProvider()
            .getTargetFormats(
                FFAudioFormat.FFEncoding.Codec.MPEG4_AAC.getEncoding(),
                new AudioFormat(
                    FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(),
                    22050f,
                    16,
                    STEREO,
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    true));
    assertEquals(0, targetFormatsEncodingAAC.length);
  }

  @Test
  public void testIsConversionSupportedStandardPCMFloat()
      throws IOException, UnsupportedAudioFileException {
    final String filename = "test.wav";
    final File file = File.createTempFile("testIsConversionSupportedStandardPCMFloat", filename);
    extractFile(filename, file);
    try {
      final AudioFormat sourceFormat =
          new FFAudioFileReader().getAudioInputStream(file).getFormat();
      assertTrue(
          new FFFormatConversionProvider()
              .isConversionSupported(AudioFormat.Encoding.PCM_FLOAT, sourceFormat));
    } finally {
      file.delete();
    }
  }

  @Test
  public void testGetTargetFormatsStandardPCMFloat() {
    final AudioFormat[] targetFormats =
        new FFFormatConversionProvider()
            .getTargetFormats(
                AudioFormat.Encoding.PCM_FLOAT,
                new AudioFormat(
                    FFAudioFormat.FFEncoding.Codec.MP3.getEncoding(),
                    22050f,
                    16,
                    STEREO,
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    true));
    assertEquals(4, targetFormats.length);
    final Set<AudioFormat> audioFormats = new HashSet<>(Arrays.asList(targetFormats));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_FLOAT,
                AudioSystem.NOT_SPECIFIED,
                32,
                MONO,
                4,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
    assertTrue(
        containsWithMatches(
            audioFormats,
            new AudioFormat(
                AudioFormat.Encoding.PCM_FLOAT,
                AudioSystem.NOT_SPECIFIED,
                32,
                STEREO,
                8,
                AudioSystem.NOT_SPECIFIED,
                NATIVE_ORDER)));
  }

  @Test
  public void testConvertWavToFloat32ViaStandardEncoding()
      throws IOException, UnsupportedAudioFileException {
    final String filename = "test.wav";
    final File file = File.createTempFile("testConvertWavToFloat32ViaStandardEncoding", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    boolean hasNonZero = false;
    AudioInputStream in = null;
    AudioInputStream convertedIn = null;
    try {
      in = new FFAudioFileReader().getAudioInputStream(file);
      convertedIn =
          new FFFormatConversionProvider().getAudioInputStream(AudioFormat.Encoding.PCM_FLOAT, in);
      final AudioFormat fmt = convertedIn.getFormat();
      assertEquals(AudioFormat.Encoding.PCM_FLOAT, fmt.getEncoding());
      assertEquals(32, fmt.getSampleSizeInBits());
      final ByteOrder byteOrder =
          fmt.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
      int justRead;
      final byte[] buf = new byte[4096];
      while ((justRead = convertedIn.read(buf)) != -1) {
        assertTrue(justRead > 0);
        bytesRead += justRead;
        assertEquals("reads must be 4-byte aligned", 0, justRead % 4);
        final FloatBuffer floats =
            ByteBuffer.wrap(buf, 0, justRead).order(byteOrder).asFloatBuffer();
        while (floats.hasRemaining()) {
          final float s = floats.get();
          assertTrue("Sample out of range [-1,1]: " + s, s >= -1.0f && s <= 1.0f);
          if (s != 0.0f) hasNonZero = true;
        }
      }
    } finally {
      if (convertedIn != null) {
        try {
          convertedIn.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      file.delete();
    }
    assertTrue("Expected non-silent audio", hasNonZero);
    assertEquals(1069056, bytesRead);
  }

  @Test
  public void testConvertWavToFloat64ViaProvider()
      throws IOException, UnsupportedAudioFileException {
    final String filename = "test.wav";
    final File file = File.createTempFile("testConvertWavToFloat64ViaProvider", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    boolean hasNonZero = false;
    AudioInputStream in = null;
    AudioInputStream convertedIn = null;
    try {
      in = new FFAudioFileReader().getAudioInputStream(file);
      final AudioFormat targetFormat =
          new AudioFormat(AudioFormat.Encoding.PCM_FLOAT, 44100, 64, 2, 16, 44100, false);
      convertedIn = new FFFormatConversionProvider().getAudioInputStream(targetFormat, in);
      final AudioFormat fmt = convertedIn.getFormat();
      assertEquals(AudioFormat.Encoding.PCM_FLOAT, fmt.getEncoding());
      assertEquals(64, fmt.getSampleSizeInBits());
      final ByteOrder byteOrder =
          fmt.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
      int justRead;
      final byte[] buf = new byte[4096];
      while ((justRead = convertedIn.read(buf)) != -1) {
        assertTrue(justRead > 0);
        bytesRead += justRead;
        assertEquals("reads must be 8-byte aligned", 0, justRead % 8);
        final DoubleBuffer doubles =
            ByteBuffer.wrap(buf, 0, justRead).order(byteOrder).asDoubleBuffer();
        while (doubles.hasRemaining()) {
          final double d = doubles.get();
          assertTrue("Sample out of range [-1,1]: " + d, d >= -1.0 && d <= 1.0);
          if (d != 0.0) hasNonZero = true;
        }
      }
    } finally {
      if (convertedIn != null) {
        try {
          convertedIn.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      file.delete();
    }
    assertTrue("Expected non-silent audio", hasNonZero);
    assertEquals(2138112, bytesRead);
  }

  @Test
  public void testReadW64ToFloat64ViaStandardEncoding()
      throws IOException, UnsupportedAudioFileException {
    // test.w64 is natively PCM_FLOAT 64-bit; the encoding-only path should inherit the 64-bit
    // depth and produce a 64-bit output stream. The file's float samples are not normalized to
    // [-1,1] (valid per the IEEE float PCM convention), so we assert finiteness rather than range.
    final String filename = "test.w64";
    final File file = File.createTempFile("testReadW64ToFloat64ViaStandardEncoding", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    boolean hasNonZero = false;
    AudioInputStream in = null;
    AudioInputStream convertedIn = null;
    try {
      in = new FFAudioFileReader().getAudioInputStream(file);
      convertedIn =
          new FFFormatConversionProvider().getAudioInputStream(AudioFormat.Encoding.PCM_FLOAT, in);
      final AudioFormat fmt = convertedIn.getFormat();
      assertEquals(AudioFormat.Encoding.PCM_FLOAT, fmt.getEncoding());
      assertEquals(64, fmt.getSampleSizeInBits());
      final ByteOrder byteOrder =
          fmt.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
      int justRead;
      final byte[] buf = new byte[4096];
      while ((justRead = convertedIn.read(buf)) != -1) {
        assertTrue(justRead > 0);
        bytesRead += justRead;
        assertEquals("reads must be 8-byte aligned", 0, justRead % 8);
        final DoubleBuffer doubles =
            ByteBuffer.wrap(buf, 0, justRead).order(byteOrder).asDoubleBuffer();
        while (doubles.hasRemaining()) {
          final double d = doubles.get();
          assertTrue("Sample must be finite: " + d, Double.isFinite(d));
          if (d != 0.0) hasNonZero = true;
        }
      }
    } finally {
      if (convertedIn != null) {
        try {
          convertedIn.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      file.delete();
    }
    assertTrue("Expected non-silent audio", hasNonZero);
    assertEquals(2138112, bytesRead);
  }

  /**
   * AudioFormat uses {@link AudioFormat#matches(javax.sound.sampled.AudioFormat)} instead of {@link
   * Object#equals(Object)} for comparison - therefore we need a special <code>contains()</code>
   * method. This is inefficient, but good enough for tests.
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

  @Test
  public void testConvertTo16Bit() throws IOException, UnsupportedAudioFileException {
    final String filename = "test_long24bit.wav";
    final File file = File.createTempFile("testConvertTo16Bit", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    AudioInputStream in = null;
    AudioInputStream convertedIn = null;
    try {
      in = new FFAudioFileReader().getAudioInputStream(file);
      final AudioFormat streamFormat = in.getFormat();
      final AudioFormat targetFormat =
          new AudioFormat(
              streamFormat.getEncoding(),
              streamFormat.getSampleRate(),
              16,
              streamFormat.getChannels(),
              2 * streamFormat.getChannels(),
              streamFormat.getFrameRate(),
              streamFormat.isBigEndian(),
              streamFormat.properties());
      convertedIn = new FFFormatConversionProvider().getAudioInputStream(targetFormat, in);
      int justRead;
      final byte[] buf = new byte[1024];
      while ((justRead = in.read(buf)) != -1) {
        assertTrue(justRead > 0);
        bytesRead += justRead;
      }
    } finally {
      if (convertedIn != null) {
        try {
          convertedIn.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
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
    assertEquals(10723068, bytesRead);
  }

  @Test
  public void testConvertADPCMWaveTo8Bit() throws IOException, UnsupportedAudioFileException {
    final String filename = "test_adpcm.wav";
    final File file = File.createTempFile("testConvertADPCMWaveTo8Bit", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    AudioInputStream in = null;
    AudioInputStream convertedIn = null;
    try {
      in = new FFAudioFileReader().getAudioInputStream(file);
      final AudioFormat streamFormat = in.getFormat();
      final AudioFormat targetFormat =
          new AudioFormat(
              AudioFormat.Encoding.PCM_SIGNED,
              streamFormat.getSampleRate(),
              8,
              streamFormat.getChannels(),
              streamFormat.getChannels(),
              streamFormat.getSampleRate(),
              streamFormat.isBigEndian(),
              streamFormat.properties());
      convertedIn = new FFFormatConversionProvider().getAudioInputStream(targetFormat, in);
      int justRead;
      final byte[] buf = new byte[1024];
      while ((justRead = convertedIn.read(buf)) != -1) {
        assertTrue(justRead > 0);
        bytesRead += justRead;
      }
    } finally {
      if (convertedIn != null) {
        try {
          convertedIn.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      file.delete();
    }
    System.out.println("Read " + bytesRead + " bytes.");
    assertEquals(1328864, bytesRead);
  }
}
