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
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Our version of {@link AudioFileFormat}, instantiated from native code in <code>FFAudioFileReader.c</code>.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFAudioFileFormat extends AudioFileFormat {


    private static final Type AAC = new Type("AAC", "m4a");
    private static final Type PAAC = new Type("AAC", "m4p");
    private static final Type MP1 = new Type("MP1", "mp1");
    private static final Type MP2 = new Type("MP2", "mp2");
    private static final Type MP3 = new Type("MP3", "mp3");
    private static final Type MP4 = new Type("MP4", "mp4");
    private static final Type MPEG4VIDEO = new Type("MPEG-4 Video", "m4v");
    private static final Type FLAC = new Type("Flac", "flac");
    private static final Type VORBIS = new Type("Vorbis", "ogg");

    private static Map<Integer, Type> TYPE_MAP = new HashMap<Integer, Type>();

    static {
        TYPE_MAP.put(FFAudioFormat.FFEncoding.Codec.MP1.getEncoding().getCodecId(), FFAudioFileFormat.MP1);
        TYPE_MAP.put(FFAudioFormat.FFEncoding.Codec.MP2.getEncoding().getCodecId(), FFAudioFileFormat.MP2);
        TYPE_MAP.put(FFAudioFormat.FFEncoding.Codec.MP3.getEncoding().getCodecId(), FFAudioFileFormat.MP3);
        TYPE_MAP.put(FFAudioFormat.FFEncoding.Codec.FLAC.getEncoding().getCodecId(), FFAudioFileFormat.FLAC);
        TYPE_MAP.put(FFAudioFormat.FFEncoding.Codec.VORBIS.getEncoding().getCodecId(), FFAudioFileFormat.VORBIS);
        // TODO: add more?
    }


    private HashMap<String, Object> properties;

    public FFAudioFileFormat(final String url, final int codecId,
                             final float sampleRate, final int sampleSize, final int channels, final int packetSize,
                             final float frameRate, final boolean bigEndian, final long durationInMicroSeconds,
                             final int bitRate, final Boolean vbr)
            throws UnsupportedAudioFileException {

        super(getAudioFileFormatType(url, codecId), getLength(url),
                new FFAudioFormat(codecId, sampleRate, sampleSize, channels, packetSize,
                        determineFrameRate(codecId, sampleRate, frameRate), bigEndian, bitRate, vbr),
                        determineFrameLength(sampleRate, durationInMicroSeconds)
        );
        this.properties = new HashMap<java.lang.String,java.lang.Object>();
        if (durationInMicroSeconds > 0) this.properties.put("duration", durationInMicroSeconds);
    }

    private static int getLength(final String urlString) {
        try {
            final URL url = new URL(urlString);
            if ("file".equals(url.getProtocol())) {
                return (int)new File(url.getFile()).length();
            }
        } catch (MalformedURLException e) {
            // nothing
        }
        return -1;
    }

    private static int determineFrameLength(final float sampleRate, final long durationInMicroSeconds) {
        if (sampleRate * durationInMicroSeconds < 0) {
            return AudioSystem.NOT_SPECIFIED;
        }
        else {
            return (int) Math.round((sampleRate * (double)durationInMicroSeconds) / 1000000.0);
        }
    }

    private static float determineFrameRate(final int codecId, final float sampleRate, final float frameRate) {
        if (frameRate != AudioSystem.NOT_SPECIFIED) return frameRate;
        // frame rate and sample rate are equal when we have  PCM, A-law or mu-law data.
        final FFAudioFormat.FFEncoding.Codec codec = FFAudioFormat.FFEncoding.getCodec(codecId);

        if (codecId == FFAudioFormat.FFEncoding.Codec.ALAW.getEncoding().getCodecId()
                || codecId == FFAudioFormat.FFEncoding.Codec.ULAW.getEncoding().getCodecId()
                || (codec != null && codec.isPCM())) {
            return sampleRate;
        } else {
            return AudioSystem.NOT_SPECIFIED;
        }
    }

    private static Type getAudioFileFormatType(final String url, final int codecId) throws UnsupportedAudioFileException {
        if (url == null) {
            final AudioFormat.Encoding encoding = FFAudioFormat.FFEncoding.getInstance(codecId);
            final Type type = TYPE_MAP.get(codecId);
            if (type != null) return type;
            return new Type(encoding.toString().toUpperCase(), encoding.toString());
        }
        final Type fileType;
        final int lastDot = url.lastIndexOf('.');
        if (lastDot != -1) {
            final String extension = url.substring(lastDot + 1).toLowerCase();
            if (AAC.getExtension().equals(extension)) {
                fileType = AAC;
            } else if (PAAC.getExtension().equals(extension)) {
                fileType = PAAC;
            } else if (MPEG4VIDEO.getExtension().equals(extension)) {
                fileType = MPEG4VIDEO;
            } else if (MP3.getExtension().equals(extension)) {
                fileType = MP3;
            } else if (Type.WAVE.getExtension().equals(extension)) {
                fileType = Type.WAVE;
            } else if (Type.AIFF.getExtension().equals(extension)) {
                fileType = Type.AIFF;
            } else if (Type.AIFC.getExtension().equals(extension)) {
                fileType = Type.AIFC;
            } else if (MP4.getExtension().equals(extension)) {
                fileType = MP4;
            } else {
                fileType = new Type(extension.toUpperCase(), extension);
            }
        } else {
            throw new UnsupportedAudioFileException("Unknown target audio url type: " + url);
        }
        return fileType;
    }

    @Override
    public Map<String, Object> properties() {
        Map<java.lang.String,java.lang.Object> obj;
        if (properties == null) {
            obj = new HashMap<java.lang.String,java.lang.Object>(0);
        } else {
            obj = (Map<java.lang.String,java.lang.Object>)properties.clone();
        }
        return Collections.unmodifiableMap(obj);
    }

    @Override
    public Object getProperty(final String s) {
        if (properties == null) {
            return null;
        } else {
            return properties.get(s);
        }
    }
}
