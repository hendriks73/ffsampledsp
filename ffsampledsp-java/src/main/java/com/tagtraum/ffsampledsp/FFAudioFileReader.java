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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tagtraum.ffsampledsp.FFGlobalLock.LOCK;

/**
 * Open URLs/files or streams and returns a {@link AudioFileFormat} instance.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFAudioFileReader extends AudioFileReader {

    private static final boolean nativeLibraryLoaded;

    static {
        // Ensure JNI library is loaded
        nativeLibraryLoaded = FFNativeLibraryLoader.loadLibrary();
    }

    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private static Map<URL, AudioFileFormat[]> cache = Collections.synchronizedMap(new LinkedHashMap<URL, AudioFileFormat[]>() {
        private static final int MAX_ENTRIES = 20;

        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    });

    private static void addAudioFileFormatToCache(final URL url, final AudioFileFormat[] audioFileFormat) {
        cache.put(url, audioFileFormat);
    }

    private static AudioFileFormat[] getAudioFileFormatsFromCache(final URL url) {
        return cache.get(url);
    }

    public AudioFileFormat[] getAudioFileFormats(final InputStream stream) throws UnsupportedAudioFileException, IOException {
        if (!nativeLibraryLoaded) throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
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
    public AudioFileFormat getAudioFileFormat(final InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormats(stream)[0];
    }

    @Override
    public AudioFileFormat getAudioFileFormat(final File file) throws UnsupportedAudioFileException, IOException {
        if (!file.exists()) throw new FileNotFoundException(file.toString());
        if (!file.canRead()) throw new IOException("Can't read " + file.toString());
        return getAudioFileFormat(fileToURL(file));
    }

    /**
     * Returns one or more {@link AudioFileFormat}s for the given file.
     * Multiple objects are returned, if the file contains multiple streams, e.g. for
     * STEM files.
     *
     * @param file file
     * @return one or more {@link AudioFileFormat}s for the given URL
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @see <a href="http://www.stems-music.com">www.stems-music.com</a>
     * @see #getAudioFileFormat(File)
     */
    public AudioFileFormat[] getAudioFileFormats(final File file) throws UnsupportedAudioFileException, IOException {
        if (!file.exists()) throw new FileNotFoundException(file.toString());
        if (!file.canRead()) throw new IOException("Can't read " + file.toString());
        return getAudioFileFormats(fileToURL(file));
    }

    /**
     * Convert file to URL. Assumes that any punctuation in the filename must not be url encoded.
     *
     * @param file file
     * @return correctly encoded URL
     * @throws MalformedURLException
     */
    static URL fileToURL(final File file) throws MalformedURLException {
        try {
            String encoded = file.toURI().toString().replace("+", "%2B");
            return new URL(URLDecoder.decode(encoded, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            final MalformedURLException malformedURLException = new MalformedURLException();
            malformedURLException.initCause(e);
            throw malformedURLException;
        }
    }

    /**
     * Make sure that file URLs on Windows follow the super special libav style, e.g. "file:C:/path/file.ext"
     * or "file://UNCServerName/path/file.ext".
     */
    static String urlToString(final URL url) {
        if (url == null) return null;
        final String s = url.toString();
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
     * Returns one or more {@link AudioFileFormat}s for the given file.
     * Multiple objects are returned, if the file contains multiple streams, e.g. for
     * Stem files.
     *
     * @param url url
     * @return one or more {@link AudioFileFormat}s for the given URL
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @see <a href="http://www.stems-music.com">www.stems-music.com</a>
     * @see #getAudioFileFormat(URL)
     */
    public AudioFileFormat[] getAudioFileFormats(final URL url) throws UnsupportedAudioFileException, IOException {
        if (!nativeLibraryLoaded) throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
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

    private static void checkPlausibility(final AudioFileFormat[] audioFileFormat) throws UnsupportedAudioFileException {
        if (audioFileFormat != null && audioFileFormat.length >= 1 && audioFileFormat[0].getFormat() != null) {
            // verify plausibility of audioFileFormat
            final AudioFileFormat firstFileFormat = audioFileFormat[0];
            final AudioFormat firstFormat = audioFileFormat[0].getFormat();
            if (firstFileFormat.getFrameLength() == 0
                && firstFormat.getSampleRate() == 0
                && firstFormat.getSampleSizeInBits() == 0
                && firstFormat.getChannels() == 0) throw new UnsupportedAudioFileException("Nonplausable audio format: " + firstFileFormat);
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(final URL url) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormats(url)[0];
    }

    @Override
    public AudioInputStream getAudioInputStream(final InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(stream, 0);
    }

    @Override
    public AudioInputStream getAudioInputStream(final URL url) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(url, 0);
    }

    @Override
    public AudioInputStream getAudioInputStream(final File file) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(file, 0);
    }

    /**
     * Allows you to open a specific audio stream from the given stream.
     * Useful for <a href="http://www.stems-music.com">Stems</a>.
     *
     * @param stream stream
     * @param streamIndex audio stream index
     * @return audio stream
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @throws IndexOutOfBoundsException if the index is not valid.
     * @see #getAudioInputStream(URL)
     * @see #getAudioInputStream(File, int)
     */
    public AudioInputStream getAudioInputStream(final InputStream stream, final int streamIndex) throws UnsupportedAudioFileException, IOException {
        if (!nativeLibraryLoaded) throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
        final AudioFileFormat fileFormat = getAudioFileFormats(stream)[streamIndex];
        return new FFAudioInputStream(new FFStreamInputStream(stream, streamIndex), fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    /**
     * Allows you to open a specific audio stream from the given URL.
     * Useful for <a href="http://www.stems-music.com">Stems</a>.
     *
     * @param url url
     * @param streamIndex audio stream index
     * @return audio stream
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @throws IndexOutOfBoundsException if the index is not valid.
     * @see #getAudioInputStream(URL)
     * @see #getAudioInputStream(File, int)
     */
    public AudioInputStream getAudioInputStream(final URL url, final int streamIndex) throws UnsupportedAudioFileException, IOException {
        if (!nativeLibraryLoaded) throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
        final AudioFileFormat fileFormat = getAudioFileFormats(url)[streamIndex];
        return new FFAudioInputStream(new FFURLInputStream(url, streamIndex), fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    /**
     * Allows you to open a specific audio stream from the given file.
     * Useful for <a href="http://www.stems-music.com">Stems</a>.
     *
     * @param file file
     * @param streamIndex audio stream index
     * @return audio stream
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @throws IndexOutOfBoundsException if the index is not valid.
     * @see #getAudioInputStream(URL, int)
     * @see #getAudioInputStream(File)
     */
    public AudioInputStream getAudioInputStream(final File file, final int streamIndex) throws UnsupportedAudioFileException, IOException {
        if (!file.exists()) throw new FileNotFoundException(file.toString());
        if (!file.canRead()) throw new IOException("Can't read " + file.toString());
        return getAudioInputStream(fileToURL(file), streamIndex);
    }

    /**
     * Makes sure that functions like <code>avcodec_open2</code> are not called from multiple
     * threads at the same time.
     *
     * @param url url
     * @return file formats
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    private AudioFileFormat[] lockedGetAudioFileFormatsFromURL(final String url) throws IOException, UnsupportedAudioFileException {
        LOCK.lock();
        try {
            final AudioFileFormat[] audioFileFormat = getAudioFileFormatsFromURL(url);
            checkPlausibility(audioFileFormat);
            return audioFileFormat;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Makes sure that functions like <code>avcodec_open2</code> are not called from multiple
     * threads at the same time.
     *
     * @param byteBuffer byteBuffer
     * @return file formats
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    private AudioFileFormat[] lockedGetAudioFileFormatFromBuffer(final ByteBuffer byteBuffer) throws IOException, UnsupportedAudioFileException {
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
     * @throws IOException
     */
    private native AudioFileFormat[] getAudioFileFormatsFromURL(final String url) throws IOException, UnsupportedAudioFileException;

    /**
     * Determine {@link AudioFileFormat} from a file containing just the first kbs from a stream.
     *
     * @param byteBuffer buffer with the beginning from an audio stream
     * @return {@link AudioFileFormat}
     * @throws IOException
     */
    private native AudioFileFormat[] getAudioFileFormatsFromBuffer(final ByteBuffer byteBuffer) throws IOException, UnsupportedAudioFileException;


}

