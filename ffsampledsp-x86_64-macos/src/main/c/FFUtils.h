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
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */


/*! \mainpage FFSamplesSP
 *
 * \section intro_sec Introduction
 *
 * <a href="../index.html">FFSampledSP</a> is a free implementation
 * of the <a href="http://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/spi/package-summary.html">javax.sound.sampled.spi</a>
 * interfaces.
 * <br/>
 * Its main purpose is to decode audio from various formats at high speed.
 * FFSampledSP can support pretty much all formats supported by <a href="https://www.ffmpeg.org">FFmpeg</a>
 * as support for them has been compiled into the used FFmpeg libs.
 * <br/>
 * This is the source documentation for the native part of the library. You can find the
 * documentation for the Java part <a href="../apidocs/index.html">here</a>.
 */


#include <math.h>
#include <jni.h>
// see http://stackoverflow.com/questions/4585847/g-linking-error-on-mac-while-compiling-ffmpeg
// or http://ffmpeg.org/trac/ffmpeg/wiki/Including%20FFmpeg%20headers%20in%20a%20C%2B%2B%20application

#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <libavutil/timestamp.h>
#include <libavutil/opt.h>

/**
 * Central context representing the native peer to the Java FFNativePeerInputStream object.
 */
typedef struct {
    // general stuff
    JNIEnv          *env;                   ///< JNI environment
    jobject         java_instance;          ///< Calling Java instance
    jint            java_buffer_capacity;   ///< Current capacity of the Java nativeBuffer

    // decoding
    AVFormatContext *format_context;        ///< Current AVFormatContext
    AVStream        *stream;                ///< Audio stream we are interested in
    int             stream_index;           ///< Index of the audio stream we are using
    AVCodecContext  *decode_context;        ///< Codec context for decoding
    AVPacket        decode_packet;          ///< AVPacket for decoding
    AVFrame         *decode_frame;          ///< AVFrame for decoding
    uint8_t         **audio_data;           ///< Audio data (accommodates multiple planes)
	int             got_frame;              ///< Indicates whether we got a frame in the last call to avcodec_decode_audio4
    uint64_t        decoded_samples;        ///< Running count of decoded samples
    uint64_t        timestamp;              ///< Current timestamp (in samples, not seconds)

    // resampling
    SwrContext      *swr_context;           ///< Resampling context
    uint64_t        resampled_samples;      ///< Count of resampled samples

    // encoding
    AVCodecContext  *encode_context;        ///< Codec context for encoding
    AVPacket        encode_packet;          ///< AVPacket for encoding
    AVFrame         *encode_frame;          ///< AVFrame for encoding

} FFAudioIO;

extern const uint32_t CODEC_TAG_DRMS;

void logWarning(FFAudioIO*, int, const char*);

void logFine(FFAudioIO*, int, const char*);

void throwUnsupportedAudioFileExceptionIfError(JNIEnv*, int, const char*);

void throwIOExceptionIfError(JNIEnv*, int, const char*);

void throwIndexOutOfBoundsExceptionIfError(JNIEnv*, int, int);

void throwFileNotFoundExceptionIfError(JNIEnv*, int, const char*);

void dumpCodecIds();

int ff_open_stream(JNIEnv*, AVStream*, AVCodecContext**);

int ff_open_format_context(JNIEnv*, AVFormatContext**, const char*);

int ff_open_file(JNIEnv*, AVFormatContext**, AVStream**, AVCodecContext**, int*, const char*);

int ff_init_audioio(JNIEnv*, FFAudioIO*);

void ff_audioio_free(FFAudioIO*);

int ff_fill_buffer(FFAudioIO*);

AVCodec* ff_find_encoder(enum AVSampleFormat, int, int, int);

int ff_init_encoder(JNIEnv*, FFAudioIO*, AVCodec*);

int ff_big_endian(enum AVCodecID);


