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

import static com.tagtraum.ffsampledsp.FFGlobalLock.LOCK;

import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Audio stream capable of decoding resources via FFmpeg.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFURLInputStream extends FFNativePeerInputStream {

  /**
   * Default I/O buffer size for {@code file:} URLs when no explicit value is configured: {@value}
   * bytes (1 MB).
   *
   * @see #FILE_BUFFER_SIZE_PROPERTY
   */
  public static final int DEFAULT_FILE_BUFFER_SIZE = 1024 * 1024;

  /**
   * Name of the system property that overrides {@link #DEFAULT_FILE_BUFFER_SIZE} for {@code file:}
   * URLs when no explicit buffer size is supplied to a constructor. The property value must be a
   * positive integer representing the desired buffer size in bytes.
   *
   * <p>Example: {@code -Dffsampledsp.fileBufferSize=524288} sets a 512 KB buffer.
   */
  public static final String FILE_BUFFER_SIZE_PROPERTY = "ffsampledsp.fileBufferSize";

  /**
   * Default I/O buffer size for non-{@code file:} URLs when no explicit value is configured:
   * {@value} bytes (64 KB). A smaller value is used here to reduce latency when streaming over a
   * slow network.
   *
   * @see #URL_BUFFER_SIZE_PROPERTY
   */
  public static final int DEFAULT_URL_BUFFER_SIZE = 64 * 1024;

  /**
   * Name of the system property that overrides {@link #DEFAULT_URL_BUFFER_SIZE} for non-{@code
   * file:} URLs when no explicit buffer size is supplied to a constructor. The property value must
   * be a positive integer representing the desired buffer size in bytes.
   *
   * <p>Example: {@code -Dffsampledsp.urlBufferSize=32768} sets a 32 KB buffer.
   */
  public static final String URL_BUFFER_SIZE_PROPERTY = "ffsampledsp.urlBufferSize";

  private final boolean seekable;
  private final URL url;
  private final int fileBufferSize;

  /**
   * Opens the first audio stream from the given URL using the default I/O buffer size.
   *
   * <p>For {@code file:} URLs the buffer size comes from {@value #FILE_BUFFER_SIZE_PROPERTY},
   * falling back to {@link #DEFAULT_FILE_BUFFER_SIZE}. For all other URLs it comes from {@value
   * #URL_BUFFER_SIZE_PROPERTY}, falling back to {@link #DEFAULT_URL_BUFFER_SIZE}.
   *
   * @param url URL of the audio resource to decode
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedAudioFileException if the URL points to an unsupported or DRM-protected file
   * @see #FILE_BUFFER_SIZE_PROPERTY
   * @see #URL_BUFFER_SIZE_PROPERTY
   */
  public FFURLInputStream(final URL url) throws IOException, UnsupportedAudioFileException {
    this(url, 0, getDefaultBufferSize(url));
  }

  /**
   * Opens the specified audio stream index from the given URL using the default I/O buffer size.
   *
   * <p>For {@code file:} URLs the buffer size comes from {@value #FILE_BUFFER_SIZE_PROPERTY},
   * falling back to {@link #DEFAULT_FILE_BUFFER_SIZE}. For all other URLs it comes from {@value
   * #URL_BUFFER_SIZE_PROPERTY}, falling back to {@link #DEFAULT_URL_BUFFER_SIZE}.
   *
   * @param url URL of the audio resource to decode
   * @param streamIndex zero-based index of the audio stream to open
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedAudioFileException if the URL points to an unsupported or DRM-protected file
   * @see #FILE_BUFFER_SIZE_PROPERTY
   * @see #URL_BUFFER_SIZE_PROPERTY
   */
  public FFURLInputStream(final URL url, final int streamIndex)
      throws IOException, UnsupportedAudioFileException {
    this(url, streamIndex, getDefaultBufferSize(url));
  }

  /**
   * Opens the specified audio stream index from the given URL with an explicit file I/O buffer
   * size.
   *
   * <p>The {@code fileBufferSize} is passed directly to FFmpeg as the {@code AVFormatContext}
   * {@code io_buffer_size}, controlling the size of the internal read buffer used when reading from
   * disk or network. Larger values reduce system-call overhead and improve throughput for bulk
   * transcoding or analysis at the cost of higher memory usage per stream. A value of {@link
   * #DEFAULT_FILE_BUFFER_SIZE} (1 MB) is suitable for most use cases.
   *
   * @param url URL of the audio resource to decode
   * @param streamIndex zero-based index of the audio stream to open
   * @param fileBufferSize FFmpeg file I/O buffer size in bytes; must be positive
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedAudioFileException if the URL points to an unsupported or DRM-protected file
   * @throws IllegalArgumentException if {@code fileBufferSize} is not positive
   */
  public FFURLInputStream(final URL url, final int streamIndex, final int fileBufferSize)
      throws IOException, UnsupportedAudioFileException {
    if (fileBufferSize <= 0)
      throw new IllegalArgumentException("fileBufferSize must be positive: " + fileBufferSize);
    // FFmpeg did not use to recognize DRM-crippled files.
    // Therefore we avoid decoding altogether.
    if (url.toString().toLowerCase().endsWith(".m4p")) {
      throw new UnsupportedAudioFileException("DRM encrypted file is unsupported: " + url);
    }
    this.url = url;
    this.fileBufferSize = fileBufferSize;
    // workaround covariant return type introduced in Java 9
    // ensure limit(int) is called on Buffer, not ByteBuffer
    ((Buffer) this.nativeBuffer).limit(0);
    this.pointer = lockedOpen(FFAudioFileReader.urlToString(url), streamIndex, fileBufferSize);
    this.seekable = isSeekable(pointer);
  }

  /**
   * Returns the file I/O buffer size configured for this stream.
   *
   * @return the FFmpeg file I/O buffer size in bytes
   */
  public int getFileBufferSize() {
    return fileBufferSize;
  }

  /**
   * Returns the I/O buffer size to use for {@code file:} URLs when none is specified explicitly.
   * Reads the {@value #FILE_BUFFER_SIZE_PROPERTY} system property; falls back to {@link
   * #DEFAULT_FILE_BUFFER_SIZE} if the property is absent or not a positive integer.
   *
   * @return effective default file I/O buffer size in bytes
   */
  public static int getDefaultFileBufferSize() {
    final String prop = System.getProperty(FILE_BUFFER_SIZE_PROPERTY);
    if (prop != null) {
      try {
        final int size = Integer.parseInt(prop.trim());
        if (size > 0) return size;
      } catch (NumberFormatException ignored) {
      }
    }
    return DEFAULT_FILE_BUFFER_SIZE;
  }

  /**
   * Returns the I/O buffer size to use for non-{@code file:} URLs when none is specified
   * explicitly. Reads the {@value #URL_BUFFER_SIZE_PROPERTY} system property; falls back to {@link
   * #DEFAULT_URL_BUFFER_SIZE} if the property is absent or not a positive integer.
   *
   * @return effective default URL I/O buffer size in bytes
   */
  public static int getDefaultUrlBufferSize() {
    final String prop = System.getProperty(URL_BUFFER_SIZE_PROPERTY);
    if (prop != null) {
      try {
        final int size = Integer.parseInt(prop.trim());
        if (size > 0) return size;
      } catch (NumberFormatException ignored) {
      }
    }
    return DEFAULT_URL_BUFFER_SIZE;
  }

  /**
   * Returns the default I/O buffer size for the given URL. Delegates to {@link
   * #getDefaultFileBufferSize()} for {@code file:} URLs and to {@link #getDefaultUrlBufferSize()}
   * for all others.
   *
   * @param url URL whose scheme determines which default applies
   * @return effective default I/O buffer size in bytes
   */
  public static int getDefaultBufferSize(final URL url) {
    return "file".equalsIgnoreCase(url.getProtocol())
        ? getDefaultFileBufferSize()
        : getDefaultUrlBufferSize();
  }

  @Override
  public boolean isSeekable() {
    return seekable;
  }

  @Override
  public synchronized void seek(final long time, final TimeUnit timeUnit)
      throws UnsupportedOperationException, IOException {
    if (!isOpen()) throw new IOException("Stream is already closed: " + url);
    if (!isSeekable())
      throw new UnsupportedOperationException("Seeking is not supported for " + url);
    final long microseconds = timeUnit.toMicros(time);
    seek(pointer, microseconds);
    // workaround covariant return type introduced in Java 9
    // ensure limit(int) is called on Buffer, not ByteBuffer
    ((Buffer) this.nativeBuffer).limit(0);
  }

  @Override
  protected void fillNativeBuffer() throws IOException {
    if (isOpen()) {
      fillNativeBuffer(pointer);
    }
  }

  @Override
  public String toString() {
    return "FFURLInputStream{" + "url=" + url + ", seekable=" + seekable + '}';
  }

  /**
   * Synchronizes calls to {@link #open(String, int, int)}.
   *
   * @param url url
   * @param streamIndex index of the stream in the file, typically 0, but may differ for STEMS
   * @param fileBufferSize FFmpeg file I/O buffer size in bytes
   * @return pointer to native peer
   * @throws IOException if something IO-related goes wrong
   * @throws UnsupportedAudioFileException if the file is not supported
   * @throws IndexOutOfBoundsException if the stream index is not valid
   */
  private long lockedOpen(final String url, final int streamIndex, final int fileBufferSize)
      throws IOException, UnsupportedAudioFileException {
    LOCK.lock();
    try {
      return open(url, streamIndex, fileBufferSize);
    } finally {
      LOCK.unlock();
    }
  }

  private native boolean isSeekable(final long pointer);

  private native void seek(final long pointer, final long microseconds) throws IOException;

  private native void fillNativeBuffer(final long pointer) throws IOException;

  private native long open(final String url, final int streamIndex, final int fileBufferSize)
      throws IOException, UnsupportedAudioFileException;

  protected native void close(final long pointer) throws IOException;
}
