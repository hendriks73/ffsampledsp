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

#include "com_tagtraum_ffsampledsp_FFStreamInputStream.h"
#include "FFUtils.h"

static jmethodID fillReadBuffer_MID = NULL;
static jfieldID readBuffer_FID = NULL;

static int CALLBACK_BUFFERSIZE = 32*1024;

/**
 * Init static method and field ids for Java methods/fields, if we don't have them already.
 *
 * @param env JNIEnv
 * @param stream FFFileInputStream instance
 */
static void init_ids(JNIEnv *env, jobject stream) {
    if (!fillReadBuffer_MID || !readBuffer_FID) {
        jclass streamClass = NULL;
        streamClass = (*env)->GetObjectClass(env, stream);
        readBuffer_FID = (*env)->GetFieldID(env, streamClass, "readBuffer", "Ljava/nio/ByteBuffer;");
        fillReadBuffer_MID = (*env)->GetMethodID(env, streamClass, "fillReadBuffer", "()I");
    }
}

/**
 * Callback read function used by our custom AVIOContext.
 *
 * @param opaque    pointer to the current FFAudioIO
 * @param buf       buffer to write freshly read data to
 * @param size      size of buf
 * @return          number of bytes read or a negative number in case of an error
 */
static int read_callback(void *opaque, uint8_t *buf, int size) {
    int res = 0;
    int available_data;
    jobject read_buffer = NULL;
    uint8_t *java_buffer = NULL;
    FFAudioIO *aio = (FFAudioIO*)opaque;

    // tell java to fill buffer
    available_data = (int) (*aio->env)->CallIntMethod(aio->env, aio->java_instance, fillReadBuffer_MID);
    if (available_data > size) {
        res = -1;
        throwIOExceptionIfError(aio->env, 1, "Available data must not be larger than callback buffer.");
        goto bail;
    }
    if ((*aio->env)->ExceptionCheck(aio->env)) {
        // needed?
        //(*aio->env)->ExceptionDescribe(aio->env);
        res = -1;
        goto bail;
    }

    if (available_data <= 0) {
        res = 0;
        goto bail;
    }

    read_buffer = (*aio->env)->GetObjectField(aio->env, aio->java_instance, readBuffer_FID);
    if (!read_buffer) {
        res = -1;
        throwIOExceptionIfError(aio->env, 1, "Failed to get read buffer.");
        goto bail;
    }

    java_buffer = (uint8_t *)(*aio->env)->GetDirectBufferAddress(aio->env, read_buffer);
    if (!java_buffer) {
        res = -1;
        throwIOExceptionIfError(aio->env, 1, "Failed to get address for read buffer.");
        goto bail;
    }

    // copy to c buffer
    memcpy(buf, (const uint8_t *)java_buffer, available_data);
    // return size of buffer
    res = available_data;

bail:

    return res;
}


/**
 * Fills the java-side buffer (allocated via Java code) with fresh audio data.
 *
 * @param env JNIEnv
 * @param stream FFStreamInputStream instance
 * @param aio_pointer pointer to the FFAudioIO created when opening the file
 */
JNIEXPORT void JNICALL Java_com_tagtraum_ffsampledsp_FFStreamInputStream_fillNativeBuffer(JNIEnv *env, jobject stream, jlong aio_pointer) {

    FFAudioIO *aio = (FFAudioIO*)aio_pointer;
    aio->env = env;
    aio->java_instance = stream;
    ff_fill_buffer(aio);
    // we can ignore the return value,
    // because all ff_fill_buffer already
    // throws a suitable Java exception
    // in the case of an error
}

/**
 * Creates the FFAudioIO, custom AVIOContext etc for reading data from the stream.
 *
 * @param env       JNIEnv
 * @param stream    calling FFStreamInputStream instance
 * @return          pointer to the created FFAudioIO
 */
JNIEXPORT jlong JNICALL Java_com_tagtraum_ffsampledsp_FFStreamInputStream_open(JNIEnv *env, jobject stream) {

    int res = 0;
    FFAudioIO *aio;
    AVIOContext *io_context;
    unsigned char* callback_buffer = NULL;

    init_ids(env, stream);

    aio = calloc(1, sizeof(FFAudioIO));
    if (!aio) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate audio IO.");
        goto bail;
    }
    aio->env = env;
    aio->java_instance = stream;

    aio->format_context = avformat_alloc_context();
    if (!aio->format_context) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate format context.");
        goto bail;
    }

    // limit probe to less than what we read in one chunk...
    aio->format_context->probesize = 8*1024;
    aio->format_context->max_analyze_duration = 5*AV_TIME_BASE;

    callback_buffer = (unsigned char*)av_malloc(CALLBACK_BUFFERSIZE * sizeof(uint8_t));
    if (!callback_buffer) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate callback buffer.");
        goto bail;
    }

    io_context = avio_alloc_context(
        callback_buffer,      // IOBuffer
        CALLBACK_BUFFERSIZE, // Buffer Size (32kb corresponds to Java code)
        0,                   // Write flag, only reading, so 0
        aio,                 // FFAudioIO pointer (opaque)
        read_callback,       // Read callback
        NULL,                // Write callback
        NULL                 // Seek callback
    );
    if (!io_context) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate custom IO context.");
        goto bail;
    }
    // we didn't supply a seek function in avio_alloc_context,
    // so we need to make sure we don't seek...
    io_context->seekable = 0;

    aio->format_context->pb = io_context;

    res = ff_open_file(env, &aio->format_context, &aio->stream, &aio->stream_index, "MemoryAVIOContext");
    if (res) {
        // exception is already thrown
        goto bail;
    }

    res = ff_init_audioio(env, aio);
    if (res) {
        // exception is already thrown
        goto bail;
    }

#ifdef DEBUG
    fprintf(stderr, "stream->codec->bits_per_coded_sample: %i\n", aio->stream->codec->bits_per_coded_sample);
    fprintf(stderr, "stream->codec->bits_per_raw_sample  : %i\n", aio->stream->codec->bits_per_raw_sample);
    fprintf(stderr, "stream->codec->bit_rate             : %i\n", aio->stream->codec->bit_rate);
    fprintf(stderr, "frames     : %" PRId64 "\n", aio->stream->nb_frames);
    fprintf(stderr, "sample_rate: %i\n", aio->stream->codec->sample_rate);
    fprintf(stderr, "channels   : %i\n", aio->stream->codec->channels);
    fprintf(stderr, "frame_size : %i\n", aio->stream->codec->frame_size);
    fprintf(stderr, "codec_id   : %i\n", aio->stream->codec->codec_id);
#endif

bail:

    if (res) ff_audioio_free(aio);
    return (jlong)aio;
}

/**
 * Frees all resources associated with the given FFAudioIO.
 *
 * @param env           JNIEnv
 * @param stream        FFStreamInputStream instance
 * @param aio_pointer    pointer to FFAudioIO
 */
JNIEXPORT void JNICALL Java_com_tagtraum_ffsampledsp_FFStreamInputStream_close(JNIEnv *env, jobject stream, jlong aio_pointer) {
    ff_audioio_free((FFAudioIO*)aio_pointer);
}
