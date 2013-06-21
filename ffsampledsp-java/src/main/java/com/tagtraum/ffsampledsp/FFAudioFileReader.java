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

    private static Map<URL, AudioFileFormat> cache = Collections.synchronizedMap(new LinkedHashMap<URL, AudioFileFormat>() {
        private static final int MAX_ENTRIES = 20;

        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    });

    private static void addAudioAudioFileFormatToCache(final URL url, final AudioFileFormat audioFileFormat) {
        cache.put(url, audioFileFormat);
    }

    private static AudioFileFormat getAudioFileFormatFromCache(final URL url) {
        return cache.get(url);
    }

    @Override
    public AudioFileFormat getAudioFileFormat(final InputStream stream) throws UnsupportedAudioFileException, IOException {
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
    public AudioFileFormat getAudioFileFormat(final File file) throws UnsupportedAudioFileException, IOException {
        if (!file.exists()) throw new FileNotFoundException(file.toString());
        if (!file.canRead()) throw new IOException("Can't read " + file.toString());
        return getAudioFileFormat(fileToURL(file));
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
     * Make sure that file URLs on Windows follow the super special libav style, e.g. "file:C:/path/file.ext".
     */
    static String urlToString(final URL url) {
        if (url == null) return null;
        final String s = url.toString();
        if (WINDOWS && s.matches("file\\:/[^\\/].*")) {
            return s.replace("file:/", "file:");
        }
        return s;
    }

    @Override
    public AudioFileFormat getAudioFileFormat(final URL url) throws UnsupportedAudioFileException, IOException {
        if (!nativeLibraryLoaded) throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
        final AudioFileFormat fileFormat = getAudioFileFormatFromCache(url);
        if (fileFormat != null) {
            return fileFormat;
        }
        final AudioFileFormat audioFileFormat = lockedGetAudioFileFormatFromURL(urlToString(url));
        if (audioFileFormat != null) {
            addAudioAudioFileFormatToCache(url, audioFileFormat);
        }
        return audioFileFormat;
    }

    @Override
    public AudioInputStream getAudioInputStream(final InputStream stream) throws UnsupportedAudioFileException, IOException {
        if (!nativeLibraryLoaded) throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
        final AudioFileFormat fileFormat = getAudioFileFormat(stream);
        return new FFAudioInputStream(new FFStreamInputStream(stream), fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    @Override
    public AudioInputStream getAudioInputStream(final URL url) throws UnsupportedAudioFileException, IOException {
        if (!nativeLibraryLoaded) throw new UnsupportedAudioFileException("Native library ffsampledsp not loaded.");
        final AudioFileFormat fileFormat = getAudioFileFormat(url);
        return new FFAudioInputStream(new FFURLInputStream(url), fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    @Override
    public AudioInputStream getAudioInputStream(final File file) throws UnsupportedAudioFileException, IOException {
        if (!file.exists()) throw new FileNotFoundException(file.toString());
        if (!file.canRead()) throw new IOException("Can't read " + file.toString());
        return getAudioInputStream(fileToURL(file));
    }

    /**
     * Makes sure that functions like <code>avcodec_open2</code> are not called from multiple
     * threads at the same time.
     *
     * @param url url
     * @return file format
     * @throws IOException
     */
    private AudioFileFormat lockedGetAudioFileFormatFromURL(final String url) throws IOException {
        LOCK.lock();
        try {
            return getAudioFileFormatFromURL(url);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Makes sure that functions like <code>avcodec_open2</code> are not called from multiple
     * threads at the same time.
     *
     * @param byteBuffer byteBuffer
     * @return file format
     * @throws IOException
     */
    private AudioFileFormat lockedGetAudioFileFormatFromBuffer(final ByteBuffer byteBuffer) throws IOException {
        LOCK.lock();
        try {
            return getAudioFileFormatFromBuffer(byteBuffer);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Determine {@link AudioFileFormat} from url.
     *
     * @param url url
     * @return {@link AudioFileFormat}
     * @throws IOException
     */
    private native AudioFileFormat getAudioFileFormatFromURL(final String url) throws IOException;

    /**
     * Determine {@link AudioFileFormat} from a file containing just the first kbs from a stream.
     *
     * @param byteBuffer buffer with the beginning from an audio stream
     * @return {@link AudioFileFormat}
     * @throws IOException
     */
    private native AudioFileFormat getAudioFileFormatFromBuffer(final ByteBuffer byteBuffer) throws IOException;


}

