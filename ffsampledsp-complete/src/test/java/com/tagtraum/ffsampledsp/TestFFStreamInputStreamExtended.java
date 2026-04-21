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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.Test;

/**
 * Additional tests for FFStreamInputStream covering short files and concurrent reads.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFStreamInputStreamExtended {

  @Test
  public void testReadShortFile() throws IOException, UnsupportedAudioFileException {
    final String filename = "test_short.wav";
    final File file = File.createTempFile("testReadShortFile", filename);
    extractFile(filename, file);
    int bytesRead = 0;
    try (final AudioInputStream in =
        new FFAudioFileReader()
            .getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      int justRead;
      final byte[] buf = new byte[1024];
      while ((justRead = in.read(buf)) != -1) {
        assertTrue(justRead > 0);
        bytesRead += justRead;
      }
    } finally {
      file.delete();
    }
    System.out.println("Read " + bytesRead + " bytes from short file.");
    // stereo 16-bit 44100 Hz ~0.2s = 44100 * 2ch * 2bytes * 0.2s = 35280 bytes
    assertTrue("Expected some bytes from short file", bytesRead > 0);
    assertTrue("Short file should not produce many bytes", bytesRead < 100_000);
  }

  @Test
  public void testConcurrentRead() throws InterruptedException {
    final String filename = "test.wav";
    final int threadCount = 2;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                final File file = File.createTempFile("testConcurrentRead", filename);
                extractFile(filename, file);
                int bytesRead = 0;
                try (final AudioInputStream in =
                    new FFAudioFileReader()
                        .getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                  int justRead;
                  final byte[] buf = new byte[1024];
                  while ((justRead = in.read(buf)) != -1) {
                    bytesRead += justRead;
                  }
                } finally {
                  file.delete();
                }
                return bytesRead;
              }));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

    for (Future<Integer> future : futures) {
      try {
        final int bytesRead = future.get();
        assertTrue("Each thread should read > 0 bytes", bytesRead > 0);
        System.out.println("Thread read " + bytesRead + " bytes.");
      } catch (ExecutionException e) {
        fail("Concurrent read failed: " + e.getCause());
      }
    }
  }

  private static void extractFile(final String filename, final File file) throws IOException {
    try (final InputStream in =
            TestFFStreamInputStreamExtended.class.getResourceAsStream(filename);
        final OutputStream out = new FileOutputStream(file)) {
      final byte[] buf = new byte[1024 * 64];
      int justRead;
      while ((justRead = in.read(buf)) != -1) {
        out.write(buf, 0, justRead);
      }
    }
  }
}
