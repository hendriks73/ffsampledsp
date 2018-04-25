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

#include "com_tagtraum_ffsampledsp_FFCodecInputStream.h"
#include "FFUtils.h"
#include <math.h>


static jmethodID getEncoding_MID = NULL;
static jmethodID getSampleRate_MID = NULL;
static jmethodID getFrameSize_MID = NULL;
static jmethodID getSampleSizeInBits_MID = NULL;
static jmethodID getChannels_MID = NULL;
static jmethodID isBigEndian_MID = NULL;
static jmethodID toString_MID = NULL;

/**
 * Init static method and field ids for Java methods/fields, if we don't have them already.
 *
 * @param env JNIEnv
 */
static void init_ids(JNIEnv *env) {
    if (getSampleRate_MID == NULL) {
        jclass audioFormat_class = NULL;
        jclass encoding_class = NULL;

        audioFormat_class = (*env)->FindClass(env, "javax/sound/sampled/AudioFormat");
        encoding_class = (*env)->FindClass(env, "javax/sound/sampled/AudioFormat$Encoding");

        getEncoding_MID = (*env)->GetMethodID(env, audioFormat_class, "getEncoding", "()Ljavax/sound/sampled/AudioFormat$Encoding;");
        getSampleRate_MID = (*env)->GetMethodID(env, audioFormat_class, "getSampleRate", "()F");
        getFrameSize_MID = (*env)->GetMethodID(env, audioFormat_class, "getFrameSize", "()I");
        getSampleSizeInBits_MID = (*env)->GetMethodID(env, audioFormat_class, "getSampleSizeInBits", "()I");
        getChannels_MID = (*env)->GetMethodID(env, audioFormat_class, "getChannels", "()I");
        isBigEndian_MID = (*env)->GetMethodID(env, audioFormat_class, "isBigEndian", "()Z");
        toString_MID = (*env)->GetMethodID(env, encoding_class, "toString", "()Ljava/lang/String;");
    }
}


/**
 * Re-configures SwrContext and Encoder to match the provided target_format.
 *
 * @param env           JNIEnv
 * @param object        stream instance this call stems from, i.e. a FFCodecInputStream
 * @param target_format target AudioFormat
 * @param aio_pointer   Pointer to the FFAudioIO struct of the FFNativePeerInputStream that opened the file/stream
 * @return pointer to the FFAudioIO struct that was given as parameter
 */
JNIEXPORT jlong JNICALL Java_com_tagtraum_ffsampledsp_FFCodecInputStream_open(JNIEnv *env, jobject object, jobject target_format, jlong aio_pointer) {
    int res = 0;
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_NONE;
    int out_channel_layout = AV_CH_LAYOUT_STEREO;
    int is_float = 0;
    int is_signed = 0;
    AVCodec *encoder = NULL;
    int dither_method = SWR_DITHER_NONE;
    int output_sample_bits = 0;

    init_ids(env);

    FFAudioIO *aio = (FFAudioIO*)(intptr_t)aio_pointer;

    jfloat sample_rate = (*env)->CallFloatMethod(env, target_format, getSampleRate_MID);
    jint sample_size_in_bits = (*env)->CallIntMethod(env, target_format, getSampleSizeInBits_MID);
    jint channels = (*env)->CallIntMethod(env, target_format, getChannels_MID);
    jboolean big_endian = (*env)->CallBooleanMethod(env, target_format, isBigEndian_MID);
    jobject encoding = (*env)->CallObjectMethod(env, target_format, getEncoding_MID);
    jstring jencoding_name = (jstring)(*env)->CallObjectMethod(env, encoding, toString_MID);

    const char *encoding_name = (*env)->GetStringUTFChars(env, jencoding_name, NULL);
    is_float = strcmp("PCM_FLOAT", encoding_name) == 0;
    is_signed = strcmp("PCM_SIGNED", encoding_name) == 0;
    (*env)->ReleaseStringUTFChars(env, jencoding_name, encoding_name);

#ifdef DEBUG
    fprintf(stderr, "encoding = %s\n", encoding_name);
    fprintf(stderr, "signed   = %d\n", is_signed);
    fprintf(stderr, "float    = %d\n", is_float);
    fprintf(stderr, "bits     = %d\n", (int)sample_size_in_bits);
#endif

    if (sample_size_in_bits <= 8) {
        out_sample_fmt = AV_SAMPLE_FMT_U8;
    } else if (sample_size_in_bits <=16) {
        out_sample_fmt = AV_SAMPLE_FMT_S16;
    } else if (sample_size_in_bits <= 32 && is_float) {
        out_sample_fmt = AV_SAMPLE_FMT_FLT;
    } else if (sample_size_in_bits <=32) {
        out_sample_fmt = AV_SAMPLE_FMT_S32;
    } else if (sample_size_in_bits <= 64 && is_float) {
        out_sample_fmt = AV_SAMPLE_FMT_DBL;
    } else {
        fprintf(stderr, "Will use 64 bit PCM_FLOAT even though it might not have been desired.\n");
        out_sample_fmt = AV_SAMPLE_FMT_DBL;
    }

    if (aio->stream->codecpar->channels == channels) {
        out_channel_layout = aio->stream->codecpar->channel_layout;
    } else if (channels == 1) {
        out_channel_layout = AV_CH_LAYOUT_MONO;
    } else if (channels == 2) {
        out_channel_layout = AV_CH_LAYOUT_STEREO;
    } else {
        fprintf(stderr, "Undetermined channel layout, will use stereo.\n");
        channels = 2;
    }

    if (aio->stream->codecpar->bits_per_coded_sample > sample_size_in_bits) {
        dither_method = SWR_DITHER_TRIANGULAR;
        output_sample_bits = sample_size_in_bits;
    }

#ifdef DEBUG
    fprintf(stderr, "setting out format to: %d\n", out_sample_fmt);
#endif

    // remove default setup
    if (aio->swr_context) {
        swr_free(&aio->swr_context);
    }
    // allocate new
    aio->swr_context = swr_alloc();
    if (!aio->swr_context) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate swr context.");
        goto bail;
    }

    // standard stuff from input
    av_opt_set_sample_fmt(aio->swr_context, "in_sample_fmt",  aio->stream->codecpar->format, 0);
    av_opt_set_int(aio->swr_context, "in_channel_count",  aio->stream->codecpar->channels, 0);
    av_opt_set_int(aio->swr_context, "in_channel_layout",  aio->stream->codecpar->channel_layout, 0);
    av_opt_set_int(aio->swr_context, "in_sample_rate",     aio->stream->codecpar->sample_rate, 0);
    // custom stuff
    av_opt_set_int(aio->swr_context, "out_channel_layout", out_channel_layout, 0);
    av_opt_set_int(aio->swr_context, "out_channel_count", channels, 0);
    av_opt_set_int(aio->swr_context, "out_sample_rate", (int)round(sample_rate), 0);
    av_opt_set_sample_fmt(aio->swr_context, "out_sample_fmt", out_sample_fmt, 0);
    av_opt_set_int(aio->swr_context, "dither_method", dither_method, 0);
    av_opt_set_int(aio->swr_context, "output_sample_bits", output_sample_bits, 0);

    res = swr_init(aio->swr_context);
    if (res < 0) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not re-initialize swr context.");
        goto bail;
    }

#ifdef DEBUG
    fprintf(stderr, "open codec: dither method     : %d\n", dither_method);
    fprintf(stderr, "open codec: output sample bits: %d\n", aio->swr_context->dither.output_sample_bits);
#endif

    // re-adjust encoder
    encoder = ff_find_encoder(out_sample_fmt, sample_size_in_bits, big_endian, is_signed);
    if (!encoder) {
        res = AVERROR(EINVAL);
        throwIOExceptionIfError(env, res, "Could not find suitable encoder.");
        goto bail;
    }
    res = ff_init_encoder(env, aio, encoder);
    if (res < 0) {
        goto bail;
    }

    bail:

    return (jlong)(intptr_t)aio;
}
