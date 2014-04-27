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

#include "com_tagtraum_ffsampledsp_FFURLInputStream.h"
#include "FFUtils.h"


/**
 * Fills the java-side buffer (allocated via Java code) with fresh audio data.
 *
 * @param env JNIEnv
 * @param stream FFURLInputStream instance
 * @param aio_pointer pointer to the FFAudioIO created when opening the file
 */
JNIEXPORT void JNICALL Java_com_tagtraum_ffsampledsp_FFURLInputStream_fillNativeBuffer(JNIEnv *env, jobject stream, jlong aio_pointer) {

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
 * Open a file/URL and create a corresponding FFAudioIO.
 *
 * @param env JNIEnv
 * @param stream calling FFURLInputStream instance
 * @param url URL
 * @return pointer to new FFAudioIO
 */
JNIEXPORT jlong JNICALL Java_com_tagtraum_ffsampledsp_FFURLInputStream_open(JNIEnv *env, jobject stream, jstring url) {

    int res = 0;
    FFAudioIO *aio = NULL;

    // copy URL to local char*
    const char *input_url = (*env)->GetStringUTFChars(env, url, NULL);
    if (!input_url) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Failed to get url");
        goto bail;
    }

    aio = calloc(1, sizeof(FFAudioIO));
    if (!aio) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate audio io");
        goto bail;
    }

    res = ff_open_file(env, &(aio->format_context), &(aio->stream), &(aio->stream_index), input_url);
    if (res) {
        goto bail;
    }
    res = ff_init_audioio(env, aio);
    if (res) {
        goto bail;
    }

#ifdef DEBUG
    fprintf(stderr, "stream->codec->bits_per_coded_sample: %i\n", aio->stream->codec->bits_per_coded_sample);
    fprintf(stderr, "stream->codec->bits_per_raw_sample  : %i\n", aio->stream->codec->bits_per_raw_sample);
    fprintf(stderr, "stream->codec->bit_rate: %i\n", aio->stream->codec->bit_rate);
    fprintf(stderr, "frames     : %" PRId64 "\n", aio->stream->nb_frames);
    fprintf(stderr, "sample_rate: %i\n", aio->stream->codec->sample_rate);
    fprintf(stderr, "channels   : %i\n", aio->stream->codec->channels);
    fprintf(stderr, "frame_size : %i\n", aio->stream->codec->frame_size);
    fprintf(stderr, "codec_id   : %i\n", aio->stream->codec->codec_id);
#endif

bail:

    if (res) ff_audioio_free(aio);
    (*env)->ReleaseStringUTFChars(env, url, input_url);
    
    return (jlong)aio;
}

/**
 * Indicates whether an FFAudioIO context is seekable.
 *
 * @param env JNIEnv
 * @param stream calling FFURLInputStream instance
 * @param aio_pointer pointer to FFAudioIO context
 * @return true or false, depending on whether the URL is seekable
 */
JNIEXPORT jboolean JNICALL Java_com_tagtraum_ffsampledsp_FFURLInputStream_isSeekable(JNIEnv *env, jobject stream, jlong aio_pointer) {
    FFAudioIO *aio = (FFAudioIO*)aio_pointer;
    jboolean seekable = JNI_FALSE;
    seekable = aio->format_context->pb->seekable != 0;
    return seekable;
}

/**
 * Seeks to a point in time.
 *
 * @param env JNIEnv
 * @param stream calling FFURLInputStream instance
 * @param aio_pointer pointer to FFAudioIO context
 * @param microseconds timestamp to seek to
 */
JNIEXPORT void JNICALL Java_com_tagtraum_ffsampledsp_FFURLInputStream_seek(JNIEnv *env, jobject stream, jlong aio_pointer, jlong microseconds) {
    FFAudioIO *aio = (FFAudioIO*)aio_pointer;
    int res = 0;
    int64_t seek_target = microseconds;
    int64_t current_timestamp = 0;

    current_timestamp = aio->timestamp;
    seek_target = av_rescale_q(seek_target, AV_TIME_BASE_Q, aio->stream->time_base);
#ifdef DEBUG
    fprintf(stderr, "Current Timestamp = %" PRId64 ", seek_target = %" PRId64 "\n", current_timestamp, seek_target);
#endif
    res = av_seek_frame(aio->format_context, aio->stream_index, seek_target, current_timestamp > seek_target ? AVSEEK_FLAG_BACKWARD : 0);
    if (res < 0) {
        throwIOExceptionIfError(env, res, "Failed to seek.");
        goto bail;
    }

    // make sure everything is flushed.
    av_init_packet(&(aio->decode_packet));
    aio->decode_packet.data = NULL;
    aio->decode_packet.size = 0;
    // flush codec
    avcodec_flush_buffers(aio->stream->codec);
    // set timestamp to seek_target, since that's hopefully now our current timestamp..
    aio->timestamp = seek_target;

    bail:

    return;
}

/**
 * Free all resources associated with a given FFAudioIO.
 *
 * @param env JNIEnv
 * @param stream calling FFURLInputStream instance
 * @param aio_pointer pointer to FFAudioIO
 */
JNIEXPORT void JNICALL Java_com_tagtraum_ffsampledsp_FFURLInputStream_close(JNIEnv *env, jobject stream, jlong aio_pointer) {
    ff_audioio_free((FFAudioIO*)aio_pointer);
}
