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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

/**
 * Open URLs/files or streams and returns a {@link AudioFileFormat} instance.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFAudioFileReader extends AudioFileReader {

  /** Creates a new {@code FFAudioFileReader}. */
  public FFAudioFileReader() {}

  private static final boolean nativeLibraryLoaded;

  static {
    // Ensure JNI library is loaded
    nativeLibraryLoaded = FFNativeLibraryLoader.loadLibrary();
  }

  private static final boolean WINDOWS =
      System.getProperty("os.name").toLowerCase().contains("win");

  private static final Map<URL, AudioFileFormat[]> cache =
      Collections.synchronizedMap(
          new LinkedHashMap<URL, AudioFileFormat[]>() {
            private static final int MAX_ENTRIES = 20;

            @Override
            protected boolean removeEldestEntry(final Map.Entry eldest) {
              return size() > MAX_ENTRIES;
            }
          });

  private static void addAudioFileFormatToCache(
      final URL url, final AudioFileFormat[] audioFileFormat) {
    cache.put(url, audioFileFormat);
  }

  private static AudioFileFormat[] getAudioFileFormatsFromCache(final URL url) {
    return cache.get(url);
  }

  /**
   * Returns all {@link AudioFileFormat}s detected in the given stream. The stream must support
   * {@link InputStream#mark(int)}.
   *
   * @param stream mark-supporting input stream to probe
   * @return array of detected audio file formats
   * @throws UnsupportedAudioFileException if the stream cannot be recognised as a supported format
   * @throws IOException if an I/O error occurs or the stream does not support {@code mark}
   */
  public AudioFileFormat[] getAudioFileFormats(final InputStream stream)
      throws UnsupportedAudioFileException, IOException {
    if (!nativeLibraryLoaded)
      throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
    if (!stream.markSupported()) throw new IOException("InputStream must support mark()");
    final int readlimit = 1024 * 32;
    stream.mark(readlimit);

    final ReadableByteChannel channel = Channels.newChannel(stream);
    final ByteBuffer buf = ByteBuffer.allocateDirect(readlimit);
    try {
      channel.read(buf);
      buf.flip();
      return lockedGetAudioFileFormatFromBuffer(buf);
    } finally {
      stream.reset();
    }
  }

  @Override
  public AudioFileFormat getAudioFileFormat(final InputStream stream)
      throws UnsupportedAudioFileException, IOException {
    return getAudioFileFormats(stream)[0];
  }

  @Override
  public AudioFileFormat getAudioFileFormat(final File file)
      throws UnsupportedAudioFileException, IOException {
    if (!file.exists()) throw new FileNotFoundException(file.toString());
    if (!file.canRead()) throw new IOException("Can't read " + file);
    return getAudioFileFormat(fileToURL(file));
  }

  /**
   * Returns one or more {@link AudioFileFormat}s for the given file. Multiple objects are returned,
   * if the file contains multiple streams, e.g. for STEM files.
   *
   * @param file file
   * @return one or more {@link AudioFileFormat}s for the given URL
   * @throws UnsupportedAudioFileException if the audio is not supported
   * @throws IOException if an IO error occurs
   * @see <a href="https://www.stems-music.com">www.stems-music.com</a>
   * @see #getAudioFileFormat(File)
   */
  public AudioFileFormat[] getAudioFileFormats(final File file)
      throws UnsupportedAudioFileException, IOException {
    if (!file.exists()) throw new FileNotFoundException(file.toString());
    if (!file.canRead()) throw new IOException("Can't read " + file);
    return getAudioFileFormats(fileToURL(file));
  }

  /**
   * Convert file to URL. The returned URL keeps all path characters percent-encoded as produced by
   * {@link File#toURI()}, except that {@code +} is encoded as {@code %2B} so that {@link
   * #urlToString(URL)} (which calls {@link java.net.URLDecoder}) does not misinterpret it as a
   * space. All decoding for FFmpeg is done exclusively in {@code urlToString}.
   *
   * @param file file
   * @return percent-encoded file URL suitable for passing to {@link #urlToString(URL)}
   * @throws MalformedURLException if the URL is malformed
   */
  static URL fileToURL(final File file) throws MalformedURLException {
    return new URL(file.toURI().toString().replace("+", "%2B"));
  }

  /**
   * Make sure that file URLs on Windows follow the super special libav style, e.g.
   * "file:C:/path/file.ext" or "file://UNCServerName/path/file.ext". For file: URLs on all
   * platforms, percent-encoded sequences (e.g. %20 for space) are decoded because FFmpeg's file:
   * protocol handler passes the path directly to the OS without decoding.
   */
  static String urlToString(final URL url) {
    if (url == null) return null;
    String s = url.toString();
    if (s.startsWith("file:")) {
      // FFmpeg's file: protocol handler does not percent-decode paths, so decode here.
      // Protect '+' first so URLDecoder does not convert it to a space.
      try {
        s = URLDecoder.decode(s.replace("+", "%2B"), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // UTF-8 is always available; cannot happen
      }
    }
    if (WINDOWS && s.matches("file\\:/[^\\/].*")) {
      return s.replace("file:/", "file:");
    }
    // deal with UNC paths
    if (WINDOWS && s.matches("file\\:////[^\\/].*")) {
      return s.replace("file://", "file:");
    }
    return s;
  }

  /**
   * Returns one or more {@link AudioFileFormat}s for the given file. Multiple objects are returned,
   * if the file contains multiple streams, e.g. for Stem files.
   *
   * @param url url
   * @return one or more {@link AudioFileFormat}s for the given URL
   * @throws UnsupportedAudioFileException if the audio is not supported
   * @throws IOException if an IO error occurs
   * @see <a href="https://www.stems-music.com">www.stems-music.com</a>
   * @see #getAudioFileFormat(URL)
   */
  public AudioFileFormat[] getAudioFileFormats(final URL url)
      throws UnsupportedAudioFileException, IOException {
    if (!nativeLibraryLoaded)
      throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
    final AudioFileFormat[] fileFormats = getAudioFileFormatsFromCache(url);
    if (fileFormats != null) {
      return fileFormats;
    }
    final AudioFileFormat[] audioFileFormat = lockedGetAudioFileFormatsFromURL(urlToString(url));
    if (audioFileFormat != null) {
      addAudioFileFormatToCache(url, audioFileFormat);
    }
    return audioFileFormat;
  }

  private static void checkPlausibility(final AudioFileFormat[] audioFileFormat)
      throws UnsupportedAudioFileException {
    if (audioFileFormat != null
        && audioFileFormat.length >= 1
        && audioFileFormat[0].getFormat() != null) {
      // verify plausibility of audioFileFormat
      final AudioFileFormat firstFileFormat = audioFileFormat[0];
      final AudioFormat firstFormat = audioFileFormat[0].getFormat();
      if (firstFileFormat.getFrameLength() == 0
          && firstFormat.getSampleRate() == 0
          && firstFormat.getSampleSizeInBits() == 0
          && firstFormat.getChannels() == 0)
        throw new UnsupportedAudioFileException("Nonplausable audio format: " + firstFileFormat);
    }
  }

  @Override
  public AudioFileFormat getAudioFileFormat(final URL url)
      throws UnsupportedAudioFileException, IOException {
    return getAudioFileFormats(url)[0];
  }

  @Override
  public AudioInputStream getAudioInputStream(final InputStream stream)
      throws UnsupportedAudioFileException, IOException {
    return getAudioInputStream(stream, 0);
  }

  @Override
  public AudioInputStream getAudioInputStream(final URL url)
      throws UnsupportedAudioFileException, IOException {
    return getAudioInputStream(url, 0);
  }

  @Override
  public AudioInputStream getAudioInputStream(final File file)
      throws UnsupportedAudioFileException, IOException {
    return getAudioInputStream(file, 0);
  }

  /**
   * Allows you to open a specific audio stream from the given stream. Useful for <a
   * href="https://www.stems-music.com">Stems</a>.
   *
   * @param stream stream
   * @param streamIndex audio stream index
   * @return audio stream
   * @throws UnsupportedAudioFileException if the audio is not supported
   * @throws IOException if an IO error occurs
   * @throws IndexOutOfBoundsException if the index is not valid.
   * @see #getAudioInputStream(URL)
   * @see #getAudioInputStream(File, int)
   */
  public AudioInputStream getAudioInputStream(final InputStream stream, final int streamIndex)
      throws UnsupportedAudioFileException, IOException {
    if (!nativeLibraryLoaded)
      throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
    final AudioFileFormat fileFormat = getAudioFileFormats(stream)[streamIndex];
    return new FFAudioInputStream(
        new FFStreamInputStream(stream, streamIndex),
        fileFormat.getFormat(),
        fileFormat.getFrameLength());
  }

  /**
   * Allows you to open a specific audio stream from the given URL. Useful for <a
   * href="https://www.stems-music.com">Stems</a>.
   *
   * <p>The FFmpeg file I/O buffer size is determined by the {@value
   * FFURLInputStream#FILE_BUFFER_SIZE_PROPERTY} system property, falling back to {@link
   * FFURLInputStream#DEFAULT_FILE_BUFFER_SIZE} if the property is not set.
   *
   * @param url url
   * @param streamIndex audio stream index
   * @return audio stream
   * @throws UnsupportedAudioFileException if the audio is not supported
   * @throws IOException if an IO error occurs
   * @throws IndexOutOfBoundsException if the index is not valid.
   * @see #getAudioInputStream(URL)
   * @see #getAudioInputStream(File, int)
   * @see #getAudioInputStream(URL, int, int)
   */
  public AudioInputStream getAudioInputStream(final URL url, final int streamIndex)
      throws UnsupportedAudioFileException, IOException {
    return getAudioInputStream(url, streamIndex, FFURLInputStream.getDefaultBufferSize(url));
  }

  /**
   * Allows you to open a specific audio stream from the given URL with an explicit file I/O buffer
   * size. Useful for <a href="https://www.stems-music.com">Stems</a>.
   *
   * <p>The {@code fileBufferSize} controls the size of the FFmpeg internal read buffer, passed as
   * {@code AVFormatContext.io_buffer_size}. Larger values reduce system-call overhead and improve
   * throughput for bulk transcoding or analysis at the cost of higher per-stream memory use. Use
   * {@link FFURLInputStream#DEFAULT_FILE_BUFFER_SIZE} (1 MB) as a baseline for bulk work.
   *
   * @param url url
   * @param streamIndex audio stream index
   * @param fileBufferSize FFmpeg file I/O buffer size in bytes; must be positive
   * @return audio stream
   * @throws UnsupportedAudioFileException if the audio is not supported
   * @throws IOException if an IO error occurs
   * @throws IndexOutOfBoundsException if the index is not valid.
   * @throws IllegalArgumentException if {@code fileBufferSize} is not positive
   * @see #getAudioInputStream(URL, int)
   * @see FFURLInputStream#DEFAULT_FILE_BUFFER_SIZE
   * @see FFURLInputStream#FILE_BUFFER_SIZE_PROPERTY
   */
  public AudioInputStream getAudioInputStream(
      final URL url, final int streamIndex, final int fileBufferSize)
      throws UnsupportedAudioFileException, IOException {
    if (!nativeLibraryLoaded)
      throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
    final AudioFileFormat fileFormat = getAudioFileFormats(url)[streamIndex];
    return new FFAudioInputStream(
        new FFURLInputStream(url, streamIndex, fileBufferSize),
        fileFormat.getFormat(),
        fileFormat.getFrameLength());
  }

  /**
   * Allows you to open a specific audio stream from the given file. Useful for <a
   * href="https://www.stems-music.com">Stems</a>.
   *
   * <p>The FFmpeg file I/O buffer size is determined by the {@value
   * FFURLInputStream#FILE_BUFFER_SIZE_PROPERTY} system property, falling back to {@link
   * FFURLInputStream#DEFAULT_FILE_BUFFER_SIZE} if the property is not set.
   *
   * @param file file
   * @param streamIndex audio stream index
   * @return audio stream
   * @throws UnsupportedAudioFileException if the audio is not supported
   * @throws IOException if an IO error occurs
   * @throws IndexOutOfBoundsException if the index is not valid.
   * @see #getAudioInputStream(URL, int)
   * @see #getAudioInputStream(File)
   * @see #getAudioInputStream(File, int, int)
   */
  public AudioInputStream getAudioInputStream(final File file, final int streamIndex)
      throws UnsupportedAudioFileException, IOException {
    if (!file.exists()) throw new FileNotFoundException(file.toString());
    if (!file.canRead()) throw new IOException("Can't read " + file);
    return getAudioInputStream(fileToURL(file), streamIndex);
  }

  /**
   * Allows you to open a specific audio stream from the given file with an explicit file I/O buffer
   * size. Useful for <a href="https://www.stems-music.com">Stems</a>.
   *
   * <p>The {@code fileBufferSize} controls the size of the FFmpeg internal read buffer, passed as
   * {@code AVFormatContext.io_buffer_size}. Larger values reduce system-call overhead and improve
   * throughput for bulk transcoding or analysis at the cost of higher per-stream memory use. Use
   * {@link FFURLInputStream#DEFAULT_FILE_BUFFER_SIZE} (1 MB) as a baseline for bulk work.
   *
   * @param file file
   * @param streamIndex audio stream index
   * @param fileBufferSize FFmpeg file I/O buffer size in bytes; must be positive
   * @return audio stream
   * @throws UnsupportedAudioFileException if the audio is not supported
   * @throws IOException if an IO error occurs
   * @throws IndexOutOfBoundsException if the index is not valid.
   * @throws IllegalArgumentException if {@code fileBufferSize} is not positive
   * @see #getAudioInputStream(File, int)
   * @see #getAudioInputStream(URL, int, int)
   * @see FFURLInputStream#DEFAULT_FILE_BUFFER_SIZE
   * @see FFURLInputStream#FILE_BUFFER_SIZE_PROPERTY
   */
  public AudioInputStream getAudioInputStream(
      final File file, final int streamIndex, final int fileBufferSize)
      throws UnsupportedAudioFileException, IOException {
    if (!file.exists()) throw new FileNotFoundException(file.toString());
    if (!file.canRead()) throw new IOException("Can't read " + file);
    return getAudioInputStream(fileToURL(file), streamIndex, fileBufferSize);
  }

  /**
   * Makes sure that functions like <code>avcodec_open2</code> are not called from multiple threads
   * at the same time.
   *
   * @param url url
   * @return file formats
   * @throws IOException if an IO error occurs
   * @throws UnsupportedAudioFileException if the audio is not supported
   */
  private AudioFileFormat[] lockedGetAudioFileFormatsFromURL(final String url)
      throws IOException, UnsupportedAudioFileException {
    LOCK.lock();
    try {
      final AudioFileFormat[] audioFileFormat =
          getAudioFileFormatsFromURL(url, url.getBytes(StandardCharsets.UTF_8));
      checkPlausibility(audioFileFormat);
      return audioFileFormat;
    } finally {
      LOCK.unlock();
    }
  }

  /**
   * Makes sure that functions like <code>avcodec_open2</code> are not called from multiple threads
   * at the same time.
   *
   * @param byteBuffer byteBuffer
   * @return file formats
   * @throws IOException if an IO error occurs
   * @throws UnsupportedAudioFileException if the audio is not supported
   */
  private AudioFileFormat[] lockedGetAudioFileFormatFromBuffer(final ByteBuffer byteBuffer)
      throws IOException, UnsupportedAudioFileException {
    LOCK.lock();
    try {
      final AudioFileFormat[] audioFileFormat = getAudioFileFormatsFromBuffer(byteBuffer);
      checkPlausibility(audioFileFormat);
      return audioFileFormat;
    } finally {
      LOCK.unlock();
    }
  }

  /**
   * Determine {@link AudioFileFormat}s from url.
   *
   * @param url url
   * @return {@link AudioFileFormat}s
   * @throws IOException if an IO error occurs
   */
  private native AudioFileFormat[] getAudioFileFormatsFromURL(
      final String url, final byte[] urlBytes) throws IOException, UnsupportedAudioFileException;

  /**
   * Determine {@link AudioFileFormat} from a file containing just the first kbs from a stream.
   *
   * @param byteBuffer buffer with the beginning from an audio stream
   * @return {@link AudioFileFormat}
   * @throws IOException if an IO error occurs
   */
  private native AudioFileFormat[] getAudioFileFormatsFromBuffer(final ByteBuffer byteBuffer)
      throws IOException, UnsupportedAudioFileException;
}
