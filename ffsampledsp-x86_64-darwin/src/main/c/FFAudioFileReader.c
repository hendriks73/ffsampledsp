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

#include "com_tagtraum_ffsampledsp_FFAudioFileReader.h"
#include "FFUtils.h"

static int CALLBACK_BUFFERSIZE = 32*1024;

static jmethodID limit_MID = NULL;
static jmethodID ffAudioFileFormat_MID = NULL;

/**
 * Init static method and field ids for Java methods/fields, if we don't have them already.
 *
 * @param env JNIEnv
 */
static void init_ids(JNIEnv *env) {
#ifdef DEBUG
    fprintf(stderr, "FFAudioFileReader.c init_ids(env)\n");
#endif

    if (!limit_MID) {
        jclass bufferClass = NULL;
        jclass ffAudioFileFormat_class = NULL;

        bufferClass = (*env)->FindClass(env, "java/nio/Buffer");
        ffAudioFileFormat_class = (*env)->FindClass(env, "com/tagtraum/ffsampledsp/FFAudioFileFormat");

#ifdef DEBUG
        if (!bufferClass) fprintf(stderr, "Failed to find java/nio/Buffer\n");
        if (!ffAudioFileFormat_class) fprintf(stderr, "Failed to find com/tagtraum/ffsampledsp/FFAudioFileFormat\n");
#endif

        limit_MID = (*env)->GetMethodID(env, bufferClass, "limit", "()I");
        ffAudioFileFormat_MID = (*env)->GetMethodID(env, ffAudioFileFormat_class, "<init>", "(Ljava/lang/String;IFIIIFZJILjava/lang/Boolean;)V");
#ifdef DEBUG
        if (!limit_MID) fprintf(stderr, "Failed to find limit method\n");
        if (!ffAudioFileFormat_MID) fprintf(stderr, "Failed to find constructor of AudioFileFormat\n");
#endif
    }
}

/**
 * Simple struct used for the read_callback of stream data.
 */
typedef struct {
    JNIEnv          *env;           ///< JNI environment
    jobject         byte_buffer;    ///< byte buffer passed in (read only once)
    int             call_count;     ///< number of times the read_callback was called
} FFCallback;

/**
 * Callback read function for our custom AVIOContext.
 *
 * @param opaque    pointer passed in with Callback information
 * @param buf       the buffer to write to
 * @param size      size of buf
 * @return          number of bytes written to buf or a negative value in case of an error
 */
static int read_callback(void *opaque, uint8_t *buf, int size) {
    int res = 0;
    int availableData;
    uint8_t *java_buffer = NULL;
    FFCallback *callback = (FFCallback *)opaque;

    if (callback->call_count > 0) goto bail;
    callback->call_count = 1;

    java_buffer = (uint8_t *)(*callback->env)->GetDirectBufferAddress(callback->env, callback->byte_buffer);
    if (!java_buffer) {
        res = -1;
        throwIOExceptionIfError(callback->env, 1, "Failed to get address for byte buffer");
        goto bail;
    }
    // get available data, i.e. the limit of the java buffer
    availableData = (*callback->env)->CallIntMethod(callback->env, callback->byte_buffer, limit_MID);
    // copy to c buf
    memcpy(buf, (const uint8_t *)java_buffer, availableData);

    res = availableData;

bail:

    return res;
}

/**
 * Determines, whether the given codec is a standard, interleaved PCM codec or not.
 *
 * @param id    AVCodecID
 * @return      1, if id is a standard PCM codec
 */
static int is_pcm(enum AVCodecID id) {
    int res = 0;
    switch (id) {
        case AV_CODEC_ID_PCM_S8:
        case AV_CODEC_ID_PCM_U8:
        case AV_CODEC_ID_PCM_S16BE:
        case AV_CODEC_ID_PCM_S16LE:
        case AV_CODEC_ID_PCM_U16BE:
        case AV_CODEC_ID_PCM_U16LE:
        case AV_CODEC_ID_PCM_S24BE:
        case AV_CODEC_ID_PCM_S24LE:
        case AV_CODEC_ID_PCM_S32BE:
        case AV_CODEC_ID_PCM_S32LE:
        case AV_CODEC_ID_PCM_U32BE:
        case AV_CODEC_ID_PCM_U32LE:
        case AV_CODEC_ID_PCM_F32BE:
        case AV_CODEC_ID_PCM_F32LE:
        case AV_CODEC_ID_PCM_F64BE:
        case AV_CODEC_ID_PCM_F64LE:
            res = 1;
            break;
        default:
            res = 0;
    }
    return res;
}

/**
 * Computes the duration of a given stream in microseconds.
 *
 * @param   format_context AVFormatContext
 * @param   stream audio stream
 * @return  duration in microseconds or -1, if unknown
 */
static jlong duration(AVFormatContext *format_context, AVStream *stream) {
    jlong duration_in_microseconds = -1;

    if (format_context->duration != AV_NOPTS_VALUE) {
        int64_t micro_seconds_base = AV_TIME_BASE / 1000000; // this should be=1
        duration_in_microseconds = (jlong)(format_context->duration / micro_seconds_base);
    }

    if (stream->nb_frames != 0 && duration_in_microseconds <=0 && stream->codec->sample_rate > 0) {
        duration_in_microseconds = stream->nb_frames * 1000000L / stream->codec->sample_rate;
    }
    return duration_in_microseconds;
}

/**
 * Computes the frame rate of a given stream.
 *
 * @param   duration duration in microseconds
 * @param   stream audio stream
 * @return  frame rate or -1, if unknown
 */
static jfloat get_frame_rate(AVStream *stream, jlong duration) {
    jfloat frame_rate = -1;

    if (frame_rate <=0 && stream->nb_frames > 0 && duration > 0) {
        frame_rate = stream->nb_frames * 1000000LL / (jfloat)duration;
    }

    if (frame_rate <=0 && stream->codec->frame_size > 0 && stream->codec->sample_rate > 0) {
        frame_rate = (jfloat)stream->codec->sample_rate/(jfloat)stream->codec->frame_size;
    }

    if (frame_rate <=0 && stream->codec->frame_size == 0 && stream->codec->sample_rate > 0) {
        frame_rate = (jfloat)stream->codec->sample_rate;
    }


#ifdef DEBUG
    if (stream->nb_frames > 0 && duration > 0) {
        fprintf(stderr, "1 frame rate : %f\n", stream->nb_frames * 1000000LL / (jfloat)duration);
    }
    if (stream->codec->frame_size > 0 && stream->codec->sample_rate > 0) {
        fprintf(stderr, "2 frame rate : %f\n", (jfloat)stream->codec->sample_rate/(jfloat)stream->codec->frame_size);
    }
    if (stream->codec->frame_size == 0 && stream->codec->sample_rate > 0) {
        fprintf(stderr, "3 frame rate : %f\n", (jfloat)stream->codec->sample_rate);
    }
#endif

    return frame_rate;
}

/**
 * Creates an FFAudioFileFormat object.
 *
 * @return freshly instantiated FFAudioFileFormat or NULL in case of an error
 */
static jobject create_ffaudiofileformat(JNIEnv *env, jstring url, jint codecId, jfloat sampleRate, jint sampleSize,
                                         jint channels, jint frame_size, jfloat frame_rate, jboolean big_endian, jlong duration,
                                         jint bitRate, jobject vbr) {
    jclass ffAudioFileFormat_class = NULL;

    ffAudioFileFormat_class = (*env)->FindClass(env, "com/tagtraum/ffsampledsp/FFAudioFileFormat");

    /* Construct an FFAudioFileFormat object */
    return (*env)->NewObject(env, ffAudioFileFormat_class, ffAudioFileFormat_MID, url, codecId, sampleRate, sampleSize,
                channels, frame_size, frame_rate, big_endian, duration, bitRate, vbr);
}

/**
 * Opens the given URL to determine its AudioFileFormat.
 *
 * @param env JNIEnv
 * @param instance calling FFAudioFileReader instance
 * @param url URL (as jstring)
 */
 JNIEXPORT jobject JNICALL Java_com_tagtraum_ffsampledsp_FFAudioFileReader_getAudioFileFormatFromURL(JNIEnv *env, jobject instance, jstring url) {

#ifdef DEBUG
    fprintf(stderr, "openFromUrl_1\n");
#endif

    int res = 0;
    AVFormatContext *format_context = NULL;
    AVStream *stream = NULL;
    int stream_index = 0;

    jlong duration_in_microseconds = -1;
    jfloat frame_rate = -1;
    jobject vbr = NULL;
    jboolean big_endian = 1;
    jobject audio_format = NULL;
    jint frame_size = -1;
    jint sample_size = 0;

    init_ids(env);

    const char *input_url = (*env)->GetStringUTFChars(env, url, NULL);
    res = ff_open_file(env, &format_context, &stream, &stream_index, input_url);
    if (res) {
        goto bail;
    }
    duration_in_microseconds = duration(format_context, stream);
    frame_rate = get_frame_rate(stream, duration_in_microseconds);
    big_endian = ff_big_endian(stream->codec->codec_id);
    if (is_pcm(stream->codec->codec_id)) {
        frame_size = (stream->codec->bits_per_coded_sample / 8) * stream->codec->channels;
    }
    // TODO: Support VBR.

    sample_size = stream->codec->bits_per_coded_sample
        ? stream->codec->bits_per_coded_sample
        : stream->codec->bits_per_raw_sample;


#ifdef DEBUG
    fprintf(stderr, "stream->codec->bits_per_coded_sample: %i\n", stream->codec->bits_per_coded_sample);
    fprintf(stderr, "stream->codec->bits_per_raw_sample  : %i\n", stream->codec->bits_per_raw_sample);
    fprintf(stderr, "stream->codec->bit_rate             : %i\n", stream->codec->bit_rate);
    fprintf(stderr, "format_context->packet_size         : %i\n", format_context->packet_size);
    fprintf(stderr, "frames     : %" PRId64 "\n", stream->nb_frames);
    fprintf(stderr, "sample_rate: %i\n", stream->codec->sample_rate);
    fprintf(stderr, "sampleSize : %i\n", stream->codec->bits_per_coded_sample);
    fprintf(stderr, "channels   : %i\n", stream->codec->channels);
    fprintf(stderr, "frame_size : %i\n", frame_size);
    fprintf(stderr, "codec_id   : %i\n", stream->codec->codec_id);
    fprintf(stderr, "duration   : %" PRId64 "\n", (int64_t)duration_in_microseconds);
    fprintf(stderr, "frame_rate : %f\n", frame_rate);
    if (big_endian) {
        fprintf(stderr, "big_endian  : true\n");
    } else {
        fprintf(stderr, "big_endian  : false\n");
    }
#endif
    audio_format = create_ffaudiofileformat(env, url,
                                                   stream->codec->codec_id,
                                                   (jfloat)stream->codec->sample_rate,
                                                   sample_size,
                                                   stream->codec->channels,
                                                   frame_size,
                                                   frame_rate,
                                                   big_endian,
                                                   duration_in_microseconds,
                                                   stream->codec->bit_rate,
                                                   vbr);
bail:
    if (stream && stream->codec) {
        avcodec_close(stream->codec);
    }
    if (format_context) {
        avformat_close_input(&format_context);
    }
    (*env)->ReleaseStringUTFChars(env, url, input_url);
    return audio_format;
}

/**
 * Opens the byte buffer to determine its AudioFileFormat.
 *
 * @param env JNIEnv
 * @param instance calling FFAudioFileReader instance
 * @param byte_buffer audio data
 * @return AudioFileFormat object
 */
 JNIEXPORT jobject JNICALL Java_com_tagtraum_ffsampledsp_FFAudioFileReader_getAudioFileFormatFromBuffer(JNIEnv *env, jobject instance, jobject byte_buffer) {
    int res = 0;
    AVFormatContext *format_context = NULL;
    AVStream *stream = NULL;
    int stream_index = -1;

    jfloat frame_rate = -1;
    jobject vbr = NULL;
    jboolean big_endian = 1;
    jobject audio_format = NULL;
    jint frame_size = -1;
    jint sample_size = 0;

    unsigned char* callbackBuffer = NULL;
    FFCallback *callback = NULL;
    AVIOContext *io_context;

    init_ids(env);

    callback = calloc(1, sizeof(FFCallback));
    if (!callback) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate callback.");
        goto bail;
    }
    callback->env = env;
    callback->byte_buffer = byte_buffer;
    callback->call_count = 0;

    format_context = avformat_alloc_context();
    if (!format_context) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate format context.");
        goto bail;
    }

    // limit probe to less than what we read in one chunk...
    format_context->probesize2 = 8*1024; // this corresponds to the Java code!
    format_context->max_analyze_duration2 = 5*AV_TIME_BASE;

    callbackBuffer = (unsigned char*)av_malloc(CALLBACK_BUFFERSIZE * sizeof(uint8_t));
    if (!callbackBuffer) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate callback buffer.");
        goto bail;
    }

    io_context = avio_alloc_context(
        callbackBuffer,      // IOBuffer
        CALLBACK_BUFFERSIZE, // Buffer Size (32kb corresponds to Java code)
        0,                   // Write flag, only reading, so 0
        callback,            // FFCallback pointer (opaque)
        read_callback,       // Read callback
        NULL,                // Write callback
        NULL                 // Seek callback
    );
    if (!io_context) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate custom IO context.");
        goto bail;
    }
    // we didn't supply a seek function in avio_alloc_context, so we need to make sure we don't seek..
    io_context->seekable = 0;

    format_context->pb = io_context;

    res = ff_open_file(env, &format_context, &stream, &stream_index, "MemoryAVIOContext");
    if (res) {
        goto bail;
    }
    frame_rate = get_frame_rate(stream, -1);
    big_endian = ff_big_endian(stream->codec->codec_id);
    if (is_pcm(stream->codec->codec_id)) {
        frame_size = (stream->codec->bits_per_coded_sample / 8) * stream->codec->channels;
    }

    sample_size = stream->codec->bits_per_coded_sample
        ? stream->codec->bits_per_coded_sample
        : stream->codec->bits_per_raw_sample;

#ifdef DEBUG
    fprintf(stderr, "stream->codec->bits_per_coded_sample: %i\n", stream->codec->bits_per_coded_sample);
    fprintf(stderr, "stream->codec->bits_per_raw_sample  : %i\n", stream->codec->bits_per_raw_sample);
    fprintf(stderr, "stream->codec->bit_rate             : %i\n", stream->codec->bit_rate);

    fprintf(stderr, "format_context->packet_size         : %i\n", format_context->packet_size);
    fprintf(stderr, "frames     : %" PRId64 "\n", stream->nb_frames);
    fprintf(stderr, "sample_rate: %i\n", stream->codec->sample_rate);
    fprintf(stderr, "sampleSize : %i\n", stream->codec->bits_per_coded_sample);
    fprintf(stderr, "channels   : %i\n", stream->codec->channels);
    fprintf(stderr, "frame_size : %i\n", frame_size);
    fprintf(stderr, "codec_id   : %i\n", stream->codec->codec_id);
    fprintf(stderr, "frame_rate : %f\n", frame_rate);
    if (big_endian) {
        fprintf(stderr, "big_endian  : true\n");
    } else {
        fprintf(stderr, "big_endian  : false\n");
    }
#endif
    audio_format = create_ffaudiofileformat(env, NULL,
                                                   stream->codec->codec_id,
                                                   (jfloat)stream->codec->sample_rate,
                                                   sample_size,
                                                   stream->codec->channels,
                                                   frame_size,
                                                   frame_rate,
                                                   big_endian,
                                                   -1,
                                                   stream->codec->bit_rate,
                                                   vbr);

bail:

    if (stream && stream->codec) {
        avcodec_close(stream->codec);
    }
    if (format_context) {
        AVFormatContext *s = format_context;
        if ((s->iformat && s->iformat->flags & AVFMT_NOFILE) || (s->flags & AVFMT_FLAG_CUSTOM_IO)) {
            if (s->pb) {
                avio_flush(s->pb);
                av_free(s->pb->buffer);
                av_free(s->pb);
            }
        }

        avformat_close_input(&format_context);
    }
    if (callback) {
        free(callback);
    }

    return audio_format;
}


