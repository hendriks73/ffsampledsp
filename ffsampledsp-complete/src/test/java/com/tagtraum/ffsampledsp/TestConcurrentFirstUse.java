/*
 * =================================================
 * Copyright 2026 tagtraum industries incorporated
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import org.junit.Test;

/**
 * Decodes from several threads at once in a fresh JVM, so that the very first native calls (which
 * set up cached JNI method/field IDs) happen concurrently. Before JNI IDs were initialized in the
 * classes' static initializers, this could crash the JVM with a {@code NULL} method ID.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestConcurrentFirstUse {

  private static final int JVM_RUNS = 20;
  private static final int THREADS = 8;
  private static final int CHILD_TIMEOUT_SECONDS = 60;
  private static final int BARRIER_TIMEOUT_SECONDS = 30;
  private static final String[] FILES = {"test.m4a", "test.mp3", "test.flac", "test.wav"};

  @Test
  public void testConcurrentFirstUseInFreshJVM() throws Exception {
    final File dir = Files.createTempDirectory("TestConcurrentFirstUse").toFile();
    try {
      for (final String filename : FILES) {
        try (final InputStream in = getClass().getResourceAsStream(filename)) {
          Files.copy(in, new File(dir, filename).toPath());
        }
      }
      final String java =
          System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      final File output = new File(dir, "output.txt");
      for (int run = 0; run < JVM_RUNS; run++) {
        final Process process =
            new ProcessBuilder(
                    java,
                    "-Djava.io.tmpdir=" + dir,
                    "-XX:ErrorFile=" + new File(dir, "hs_err_pid%p.log"),
                    "-cp",
                    System.getProperty("java.class.path"),
                    TestConcurrentFirstUse.class.getName(),
                    dir.toString())
                .redirectErrorStream(true)
                .redirectOutput(output)
                .start();
        try {
          final boolean exited = process.waitFor(CHILD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
          final String message = new String(Files.readAllBytes(output.toPath()), "UTF-8");
          assertTrue("Child JVM timed out in run " + run + ":\n" + message, exited);
          assertEquals("Child JVM failed in run " + run + ":\n" + message, 0, process.exitValue());
        } finally {
          process.destroyForcibly();
        }
      }
    } finally {
      final File[] files = dir.listFiles();
      if (files != null) {
        for (final File f : files) {
          f.delete();
        }
      }
      dir.delete();
    }
  }

  /**
   * Child JVM entry point: start {@link #THREADS} threads that simultaneously open, convert and
   * fully read audio files. Threads are lined up twice: before opening and before the first read,
   * so that the first native calls of each kind overlap as much as possible. Every other thread
   * reads via an {@link InputStream} (i.e. {@link FFStreamInputStream}) instead of a file (i.e.
   * {@link FFURLInputStream}).
   *
   * @param args directory containing the test files
   */
  public static void main(final String[] args) throws Exception {
    final File dir = new File(args[0]);
    final CyclicBarrier barrier = new CyclicBarrier(THREADS);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < THREADS; i++) {
      final File file = new File(dir, FILES[i % FILES.length]);
      final boolean viaStream = i % 2 == 1;
      final Thread thread =
          new Thread(
              () -> {
                try {
                  decode(file, viaStream, barrier);
                } catch (Throwable t) {
                  failure.compareAndSet(null, t);
                  // release the other threads instead of leaving them waiting forever
                  barrier.reset();
                }
              });
      thread.start();
      threads.add(thread);
    }
    for (final Thread thread : threads) {
      thread.join();
    }
    if (failure.get() != null) {
      failure.get().printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }

  private static void decode(final File file, final boolean viaStream, final CyclicBarrier barrier)
      throws Exception {
    final AudioFormat targetFormat =
        new AudioFormat(AudioFormat.Encoding.PCM_FLOAT, 44100f, 32, 1, 4, 44100f, false);
    barrier.await(BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    try (final AudioInputStream source =
            viaStream
                ? new FFAudioFileReader()
                    .getAudioInputStream(new BufferedInputStream(new FileInputStream(file)))
                : new FFAudioFileReader().getAudioInputStream(file);
        final AudioInputStream target =
            new FFFormatConversionProvider().getAudioInputStream(targetFormat, source)) {
      barrier.await(BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      final byte[] buf = new byte[8192];
      long total = 0;
      int justRead;
      while ((justRead = target.read(buf)) != -1) {
        total += justRead;
      }
      if (total == 0) {
        throw new IOException("No audio decoded from " + file);
      }
    }
  }
}
