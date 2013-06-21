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

#include "FFUtils.h"

static jfieldID nativeBuffer_FID = NULL;
static jmethodID rewind_MID = NULL;
static jmethodID limit_MID = NULL;
static jmethodID capacity_MID = NULL;
static jmethodID setNativeBufferCapacity_MID = NULL;

/**
 * Init static method and field ids for Java methods/fields, if we don't have them already.
 *
 * @param env JNIEnv
 * @param stream FFNativePeerInputStream instance
 */
static void init_ids(JNIEnv *env, jobject stream) {
    if (!nativeBuffer_FID || !rewind_MID || !limit_MID) {
        jclass bufferClass = NULL;
        jclass streamClass = NULL;

        bufferClass = (*env)->FindClass(env, "java/nio/Buffer");
        streamClass = (*env)->GetObjectClass(env, stream);

        nativeBuffer_FID = (*env)->GetFieldID(env, streamClass, "nativeBuffer", "Ljava/nio/ByteBuffer;");
        rewind_MID = (*env)->GetMethodID(env, bufferClass, "rewind", "()Ljava/nio/Buffer;");
        limit_MID = (*env)->GetMethodID(env, bufferClass, "limit", "(I)Ljava/nio/Buffer;");
        capacity_MID = (*env)->GetMethodID(env, bufferClass, "capacity", "()I");
        setNativeBufferCapacity_MID = (*env)->GetMethodID(env, streamClass, "setNativeBufferCapacity", "(I)I");
    }
}

/**
 * Find a decoder and open it for the given AVFormatContext and AVMediaType.
 *
 * @param[out]  stream_index  index of the stream the decoder was opened for
 * @param[in]   format_context     format context
 * @param       type        media type - for this library typically audio
 */
static int open_codec_context(int *stream_index,
        AVFormatContext *format_context, enum AVMediaType type) {

    int res = 0;
    AVStream *stream;
    AVCodec *decoder = NULL;

    res = av_find_best_stream(format_context, type, -1, -1, NULL, 0);
    if (res < 0) {
        goto bail;
    }
    *stream_index = res;
    stream = format_context->streams[*stream_index];

    // find decoder for the stream
    decoder = avcodec_find_decoder(stream->codec->codec_id);
    if (!decoder) {
        goto bail;
    }
    res = avcodec_open2(stream->codec, decoder, NULL);
    if (res < 0) {
        goto bail;
    }

    bail:

    return res;
}


/**
 * Throws an UnsupportedAudioFileException.
 */
void throwUnsupportedAudioFileExceptionIfError(JNIEnv *env, int err, const char * message) {
    if (err) {
        char formattedMessage [strlen(message)+4+AV_ERROR_MAX_STRING_SIZE];
        snprintf(formattedMessage, strlen(message)+4+AV_ERROR_MAX_STRING_SIZE, "%s (%.64s)", message, av_err2str(err));
#ifdef DEBUG
        fprintf(stderr, "UnsupportedAudioFileException: %s\n", formattedMessage);
#endif
        jclass excCls = (*env)->FindClass(env, "javax/sound/sampled/UnsupportedAudioFileException");
        (*env)->ThrowNew(env, excCls, formattedMessage);
    }
}

/**
 * Throws an IOException.
 */
void throwIOExceptionIfError(JNIEnv *env, int err, const char *message) {
    if (err) {
        char formattedMessage [strlen(message)+4+AV_ERROR_MAX_STRING_SIZE];
        snprintf(formattedMessage, strlen(message)+4+AV_ERROR_MAX_STRING_SIZE, "%s (%.64s)", message, av_err2str(err));
#ifdef DEBUG
        fprintf(stderr, "IOException: %s\n", formattedMessage);
#endif
        jclass excCls = (*env)->FindClass(env, "java/io/IOException");
        (*env)->ThrowNew(env, excCls, formattedMessage);
    }
}

/**
 * Throws an IllegalArgumentException.
 */
void throwFileNotFoundExceptionIfError(JNIEnv *env, int err, const char *message) {
    if (err) {
#ifdef DEBUG
		fprintf (stderr, "FileNotFoundException: '%s' %d (%4.4s)\n", message, (int)err, (char*)&err);
#endif
        jclass excCls = (*env)->FindClass(env, "java/io/FileNotFoundException");
        (*env)->ThrowNew(env, excCls, message);
    }
}


/**
 * Opens the input file/url, allocate a AVFormatContext for it and opens the audio stream with an
 * appropriate decoder.
 *
 * @param env JNIEnv
 * @param format_context AVFormatContext
 * @param stream audio AVStream
 * @param stream_index index of the selected stream
 * @param url URL to open
 * @return negative value, if something went wrong
 */
int ff_open_file(JNIEnv *env, AVFormatContext **format_context, AVStream **stream, int *stream_index, const char *url) {
    int res = 0;

    res = avformat_open_input(format_context, url, NULL, NULL);
    if (res) {
        if (res == AVERROR(ENOENT)) {
            throwFileNotFoundExceptionIfError(env, res, url);
        } else if (res == AVERROR_PROTOCOL_NOT_FOUND || res == AVERROR(EIO)) {
            throwIOExceptionIfError(env, res, url);
        } else {
            throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to open audio file");
        }
        goto bail;
    }

    res = avformat_find_stream_info(*format_context, NULL);
    if (res < 0) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to find stream info");
        goto bail;
    }

    res = open_codec_context(stream_index, *format_context, AVMEDIA_TYPE_AUDIO);
    if (res) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to open codec context.");
        goto bail;
    }
    *stream = (*format_context)->streams[*stream_index];

bail:

    return res;
}

/**
 * Allocates and initializes the SwrContext so that we don't have to deal with planar sample formats.
 *
 * @param env JNIEnv
 * @param aio FFAudioIO
 * @return a negative value should an error occur
 */
static int init_swr(JNIEnv *env, FFAudioIO *aio) {
    int res = 0;

    aio->swr_context = swr_alloc();
    if (!aio->swr_context) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate swr context.");
        goto bail;
    }

    av_opt_set_sample_fmt(aio->swr_context, "in_sample_fmt",  aio->stream->codec->sample_fmt, 0);
    // make sure we get interleaved/packed output
    av_opt_set_sample_fmt(aio->swr_context, "out_sample_fmt", av_get_packed_sample_fmt(aio->stream->codec->sample_fmt), 0);

    // keep everything else the way it was...
    av_opt_set_int(aio->swr_context, "in_channel_count",  aio->stream->codec->channels, 0);
    av_opt_set_int(aio->swr_context, "out_channel_count",  aio->stream->codec->channels, 0);
    av_opt_set_int(aio->swr_context, "in_channel_layout",  aio->stream->codec->channel_layout, 0);
    av_opt_set_int(aio->swr_context, "out_channel_layout", aio->stream->codec->channel_layout, 0);
    av_opt_set_int(aio->swr_context, "in_sample_rate",     aio->stream->codec->sample_rate, 0);
    av_opt_set_int(aio->swr_context, "out_sample_rate",    aio->stream->codec->sample_rate, 0);

    res = swr_init(aio->swr_context);
    if (res < 0) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not initialize swr context");
        goto bail;
    }

    //fprintf(stderr, "init_swr: dither context: %d\n", aio->swr_context->dither);
    //fprintf(stderr, "init_swr: output sample bits: %d\n", aio->swr_context->dither.output_sample_bits);

    bail:

    return res;
}

/**
 * Finds an AVCodec encoder for the given sample format, bits per sample, byte order and signed/unsigned encoding.
 * This method never returns a planar, but always a packed codec.
 *
 * @param sampleFormat  AVSampleFormat
 * @param bits          bits per sample
 * @param big_endian     true or false
 * @param signedSamples true, if the samples are signed
 * @return an appropriate encoder or NULL, if none can be found
 */
AVCodec* ff_find_encoder(enum AVSampleFormat sampleFormat, int bits, int big_endian, int signedSamples) {
    enum AVCodecID codec_id;

#ifdef DEBUG
    fprintf(stderr, "ff_find_encoder(fmt=%d,bits=%d,big_endian=%d,signed=%d)\n", sampleFormat, bits, big_endian, signedSamples);
#endif

    switch (sampleFormat) {
        case AV_SAMPLE_FMT_U8:
        case AV_SAMPLE_FMT_U8P:
            if (signedSamples) codec_id = AV_CODEC_ID_PCM_S8;
            else codec_id = AV_CODEC_ID_PCM_U8;
            break;
        case AV_SAMPLE_FMT_S16:
        case AV_SAMPLE_FMT_S16P:
            if (signedSamples) codec_id = big_endian ? AV_CODEC_ID_PCM_S16BE : AV_CODEC_ID_PCM_S16LE;
            else codec_id = big_endian ? AV_CODEC_ID_PCM_U16BE : AV_CODEC_ID_PCM_U16LE;
            break;
        case AV_SAMPLE_FMT_S32:
        case AV_SAMPLE_FMT_S32P:
            if (bits == 24) {
                if (signedSamples) codec_id = big_endian ? AV_CODEC_ID_PCM_S24BE : AV_CODEC_ID_PCM_S24LE;
                else codec_id = big_endian ? AV_CODEC_ID_PCM_U24BE : AV_CODEC_ID_PCM_U24LE;
            } else {
                if (signedSamples) codec_id = big_endian ? AV_CODEC_ID_PCM_S32BE : AV_CODEC_ID_PCM_S32LE;
                else codec_id = big_endian ? AV_CODEC_ID_PCM_U32BE : AV_CODEC_ID_PCM_U32LE;
            }
            break;
        case AV_SAMPLE_FMT_FLT:
        case AV_SAMPLE_FMT_FLTP:
            codec_id = big_endian ? AV_CODEC_ID_PCM_F32BE : AV_CODEC_ID_PCM_F32LE;
            break;
        case AV_SAMPLE_FMT_DBL:
        case AV_SAMPLE_FMT_DBLP:
            codec_id = big_endian ? AV_CODEC_ID_PCM_F64BE : AV_CODEC_ID_PCM_F64LE;
            break;
        default:
            codec_id = -1;
    }

    return avcodec_find_encoder(codec_id);
}

/**
 * Allocates and initializes the encoder context and frame in FFAudioIO.
 * As parameters serve the output parameters of the SwrContext from FFAudioIO.
 * Therefore the SwrContext must be setup first for this to be successful.
 *
 * @param env JNIEnv
 * @param aio FFAudioIO (our context)
 * @param encoder AVCodec to use to setup the encoder AVCodecContext
 * @return a negative value, if something goes wrong
 */
int ff_init_encoder(JNIEnv *env, FFAudioIO *aio, AVCodec *encoder) {
    int res = 0;
    int64_t out_sample_rate;
    int64_t out_channel_count;
    int64_t out_channel_layout;
    enum AVSampleFormat out_sample_fmt;

    // make sure we clean up before resetting this
    // in case this is called twice
    if (aio->encode_frame) {
        avcodec_free_frame(&aio->encode_frame);
    }
    if (aio->encode_context) {
        avcodec_close(aio->encode_context);
        av_free(aio->encode_context);
    }

    aio->encode_context = avcodec_alloc_context3(encoder);
    if (!aio->encode_context) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate codec context.");
        goto bail;
    }

    // init to whatever we have in SwrContext
    av_opt_get_int(aio->swr_context, "out_channel_count", 0, &out_channel_count);
    av_opt_get_int(aio->swr_context, "out_channel_layout", 0, &out_channel_layout);
    av_opt_get_int(aio->swr_context, "out_sample_rate", 0, &out_sample_rate);
    av_opt_get_sample_fmt(aio->swr_context, "out_sample_fmt", 0, &out_sample_fmt);

    aio->encode_context->sample_fmt = out_sample_fmt;
    aio->encode_context->sample_rate = out_sample_rate;
    aio->encode_context->channel_layout = out_channel_layout;
    aio->encode_context->channels = out_channel_count;

    res = avcodec_open2(aio->encode_context, encoder, NULL);
    if (res < 0) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not open encoder.");
        goto bail;
    }

    aio->encode_frame = avcodec_alloc_frame();
    if (!aio->encode_frame) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate encoder frame.");
        goto bail;
    }
    aio->encode_frame->nb_samples = aio->encode_context->frame_size; // this will be changed later!!
    aio->encode_frame->format = aio->encode_context->sample_fmt;
    aio->encode_frame->channel_layout = aio->encode_context->channel_layout;

    bail:

    return res;
}

/**
 * Initialize our main context FFAudioIO, so that SwrContext, decode buffers and the encoder are set
 * to reasonable values.
 *
 * @param JNIEnv    env
 * @param aio       our context, FFAudioIO
 * @return a negative value, if something went wrong
 */
int ff_init_audioio(JNIEnv *env, FFAudioIO *aio) {
    int res = 0;
    int nb_planes;
    AVCodec *codec = NULL;

    aio->timestamp = 0;

    // allocate pointer to the audio buffers, i.e. the multiple planes/channels.
    nb_planes = av_sample_fmt_is_planar(aio->stream->codec->sample_fmt)
        ? aio->stream->codec->channels
        : 1;

    // always init SWR to keep code simpler
    res = init_swr(env, aio);
    if (res < 0) {
        // exception is already thrown
        goto bail;
    }
    // if for some reason the codec delivers 24bit, we need to encode its output to little endian
    if (aio->stream->codec->bits_per_coded_sample == 24) {
        codec = ff_find_encoder(aio->stream->codec->sample_fmt, aio->stream->codec->bits_per_coded_sample, ff_big_endian(aio->stream->codec->codec_id), 1);
        if (!codec) {
            res = AVERROR(EINVAL);
            throwIOExceptionIfError(env, res, "Could not find suitable encoder codec.");
            goto bail;
        }
        res = ff_init_encoder(env, aio, codec);
        if (res<0) {
            throwIOExceptionIfError(env, res, "Could not initialize encoder codec.");
            goto bail;
        }
    }

    // allocate the buffer the codec decodes to
    aio->audio_data = av_mallocz(sizeof(uint8_t *) * nb_planes);
    if (!aio->audio_data) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate audio data buffers.");
        goto bail;
    }

    aio->decode_frame = avcodec_alloc_frame();
    if (!aio->decode_frame) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(env, res, "Could not allocate frame.");
        goto bail;
    }

    // initialize packet
    av_init_packet(&(aio->decode_packet));
    aio->decode_packet.data = NULL;
    aio->decode_packet.size = 0;

bail:

    return res;
}


/**
 * Encodes a buffer to the final format using its FFAudioIO encode_context.
 *
 * @param aio       FFAudioIO context
 * @param in_buf    input buffer, data to encode
 * @param in_size   size of the input buffer
 * @param out_buf   output buffer
 * @return a negative value should some go wrong
 */
static int encode_buffer(FFAudioIO *aio, const uint8_t *in_buf, int in_size, const uint8_t *out_buf) {
    int res = 0;
    int got_output;

    res = av_samples_get_buffer_size(NULL, aio->encode_context->channels, aio->encode_frame->nb_samples, aio->encode_context->sample_fmt, 1);

#ifdef DEBUG
    fprintf(stderr, "encode_buffer: channels=%d frame->nb_samples=%d in_size=%d\n", aio->encode_context->channels, aio->encode_frame->nb_samples, in_size);
    fprintf(stderr, "encode_buffer: needed buffer=%d available=%d\n", res, in_size);
#endif

    // setup the data pointers in the AVFrame
    res = avcodec_fill_audio_frame(aio->encode_frame,
            aio->encode_context->channels,
            aio->encode_context->sample_fmt,
            in_buf,
            in_size,
            1);
    if (res < 0) {
        throwIOExceptionIfError(aio->env, res, "Failed to fill audio frame.");
        goto bail;
    }
    av_init_packet(&aio->encode_packet);
    aio->encode_packet.data = NULL; // packet data will be allocated by the encoder
    aio->encode_packet.size = 0;
    // encode the samples
    res = avcodec_encode_audio2(aio->encode_context, &aio->encode_packet, aio->encode_frame, &got_output);
    if (res < 0) {
        throwIOExceptionIfError(aio->env, res, "Failed to encode audio frame.");
        goto bail;
    }
    if (got_output) {
        res = aio->encode_packet.size;
        memcpy((char*)out_buf, aio->encode_packet.data, aio->encode_packet.size);
        av_free_packet(&aio->encode_packet);
    }

    bail:

    return res;
}

/**
 * Resample a buffer using FFAudioUI->swr_context.
 * The returned out buffer needs to be freed by the caller.
 *
 * @param aio           FFAudioIO context
 * @param out_buf       out buffer
 * @param out_samples   out samples
 * @param in_buf        in buffer
 * @param in_samples    in samples
 * @return number of samples copied/converted or a negative value, should things go wrong
 */
static int resample(FFAudioIO *aio,  uint8_t **out_buf, int out_samples, const uint8_t **in_buf, const int in_samples) {
    int res = 0;
    int64_t out_channel_count;
    enum AVSampleFormat out_sample_format;

    if (out_samples == 0) goto bail; // nothing to do.

    av_opt_get_int(aio->swr_context, "out_channel_count", 0, &out_channel_count);
    av_opt_get_sample_fmt(aio->swr_context, "out_sample_fmt", 0, &out_sample_format);

    #ifdef DEBUG
        fprintf(stderr, "resample: out_samples=%d in_samples=%d, channels=%d sample_format=%d\n",
            out_samples, in_samples, (int)out_channel_count, out_sample_format);
    #endif

    // allocate temp buffer for resampled data
    res = av_samples_alloc(out_buf, NULL, out_channel_count, out_samples, out_sample_format, 1);
    if (res < 0) {
        res = AVERROR(ENOMEM);
        throwIOExceptionIfError(aio->env, res, "Could not allocate resample buffer.");
        goto bail;
    }

    // run the SWR conversion (even if it is not strictly necessary)
    res = swr_convert(aio->swr_context, out_buf, out_samples, in_buf, in_samples);
    if (res < 0) {
        throwIOExceptionIfError(aio->env, res, "Failed to convert audio data.");
        goto bail;
    }

    bail:

    return res;
}


/**
 * Decode a frame to a packet, run the result through SwrContext, if desired, encode it via an appropriate
 * encoder, and write the results to the Java-side native buffer.
 *
 * @param aio       FFAudio context
 * @param cached    true or false
 * @return number of bytes placed into java buffer or a negative value, if something went wrong
 */
static int decode_packet(FFAudioIO *aio, int cached) {
    int res = 0;
    uint8_t **resample_buf = NULL;
    jobject byte_buffer = NULL;
    uint8_t *javaBuffer = NULL;
    uint32_t out_buf_size = 0;
    int out_buf_samples = 0;
    int64_t out_channel_count;
    int64_t out_sample_rate;
    int flush = aio->got_frame;
    enum AVSampleFormat out;
    int bytesConsumed = 0;

    init_ids(aio->env, aio->java_instance);

    av_opt_get_int(aio->swr_context, "out_channel_count", 0, &out_channel_count);
    av_opt_get_int(aio->swr_context, "out_sample_rate", 0, &out_sample_rate);
    av_opt_get_sample_fmt(aio->swr_context, "out_sample_fmt", 0, &out);

    resample_buf = av_mallocz(sizeof(uint8_t *) * 1); // one plane!

    // make sure we really have an audio packet
    if (aio->decode_packet.stream_index == aio->stream_index) {
        // decode frame
        // got_frame indicates whether we got a frame
        bytesConsumed = avcodec_decode_audio4(aio->stream->codec, aio->decode_frame, &aio->got_frame, &aio->decode_packet);
        if (bytesConsumed < 0) {
            throwUnsupportedAudioFileExceptionIfError(aio->env, bytesConsumed, "Failed to decode audio frame.");
            return bytesConsumed;
        }

        if (aio->got_frame) {

            aio->decoded_samples += aio->decode_frame->nb_samples;
            out_buf_samples = aio->decode_frame->nb_samples;
#ifdef DEBUG
            fprintf(stderr, "samples%s n:%" PRIu64 " nb_samples:%d pts:%s\n",
                   cached ? "(cached)" : "",
                   aio->decoded_samples, aio->decode_frame->nb_samples,
                   av_ts2timestr(aio->decode_frame->pts, &aio->stream->codec->time_base));
#endif

            // adjust out sample number for a different sample rate
            // this is an estimate!!
            out_buf_samples = av_rescale_rnd(
                    swr_get_delay(aio->swr_context, aio->stream->codec->sample_rate) + aio->decode_frame->nb_samples,
                    out_sample_rate,
                    aio->stream->codec->sample_rate,
                    AV_ROUND_UP
            );

            // allocate new aio->audio_data buffers
            res = av_samples_alloc(aio->audio_data, NULL, av_frame_get_channels(aio->decode_frame),
                                   aio->decode_frame->nb_samples, aio->decode_frame->format, 1);
            if (res < 0) {
                throwIOExceptionIfError(aio->env, res, "Could not allocate audio buffer.");
                return AVERROR(ENOMEM);
            }
            // copy audio data to aio->audio_data
            av_samples_copy(aio->audio_data, aio->decode_frame->data, 0, 0,
                            aio->decode_frame->nb_samples, av_frame_get_channels(aio->decode_frame), aio->decode_frame->format);

            res = resample(aio, resample_buf, out_buf_samples, (const uint8_t **)aio->audio_data, aio->decode_frame->nb_samples);
            if (res < 0) goto bail;
            else out_buf_samples = res;

        } else if (flush && swr_get_delay(aio->swr_context, aio->stream->codec->sample_rate)) {

            res = resample(aio, resample_buf, swr_get_delay(aio->swr_context, aio->stream->codec->sample_rate), NULL, 0);
            if (res < 0) goto bail;
            else out_buf_samples = res;
        } else {
#ifdef DEBUG
            fprintf(stderr, "Got no frame.\n");
#endif
        }

        if (out_buf_samples > 0) {

            res =  av_samples_get_buffer_size(NULL, (int)out_channel_count, out_buf_samples, out, 1);
            if (res < 0) goto bail;
            else out_buf_size = res;

            // ensure native buffer capacity
            if (aio->java_buffer_capacity < out_buf_size) {
                aio->java_buffer_capacity = (*aio->env)->CallIntMethod(aio->env, aio->java_instance, setNativeBufferCapacity_MID, (jint)out_buf_size);
            }
            // get java-managed byte buffer reference
            byte_buffer = (*aio->env)->GetObjectField(aio->env, aio->java_instance, nativeBuffer_FID);
            if (!byte_buffer) {
                res = -1;
                throwIOExceptionIfError(aio->env, 1, "Failed to get native buffer.");
                goto bail;
            }

            // we have some samples, let's copy them to the java buffer, using the desired encoding
            javaBuffer = (uint8_t *)(*aio->env)->GetDirectBufferAddress(aio->env, byte_buffer);
            if (!javaBuffer) {
                throwIOExceptionIfError(aio->env, 1, "Failed to get address for native buffer.");
                goto bail;
            }
            if (aio->encode_context) {
                aio->encode_frame->nb_samples = out_buf_samples;
                res = encode_buffer(aio, resample_buf[0], out_buf_size, javaBuffer);
                if (res < 0) {
                    out_buf_size = 0;
                    goto bail;
                }
                out_buf_size = res;
            } else {
                memcpy(javaBuffer, resample_buf[0], out_buf_size);
            }
            // we already wrote to the buffer, now we still need to
            // set new bytebuffer limit and position to 0.
            (*aio->env)->CallObjectMethod(aio->env, byte_buffer, rewind_MID);
            (*aio->env)->CallObjectMethod(aio->env, byte_buffer, limit_MID, out_buf_size);
        }
    }

    aio->resampled_samples += out_buf_size;

bail:

    if (resample_buf) {
        if (resample_buf[0]) av_freep(&resample_buf[0]);
        av_free(resample_buf);
    }
    if (aio->audio_data[0]) av_freep(&aio->audio_data[0]);

    return res;
}


/**
 * Reads a frame via <code>av_read_frame(AVFormatContext, AVPacket)</code>,
 * decodes it to a AVPacket, and writes the result to the
 * Java-side <code>nativeBuffer</code>.
 *
 * @param aio   current FFAudioIO
 * @return  a negative number, if something went wrong
 */
int ff_fill_buffer(FFAudioIO *aio) {
    int res = 0;
    int read_frame = 0;

    aio->timestamp += aio->decode_packet.duration;

    while (res == 0 && read_frame >= 0) {
        read_frame = av_read_frame(aio->format_context, &aio->decode_packet);
        if (read_frame >= 0) {
            res = decode_packet(aio, 0);
    #ifdef DEBUG
            fprintf(stderr, "res       : %i\n", res);
            fprintf(stderr, "read_frame: %i\n", read_frame);
            fprintf(stderr, "duration  : %i\n", aio->decode_packet.duration);
            fprintf(stderr, "timestamp : %" PRId64 "\n", aio->timestamp);
            fprintf(stderr, "pts       : %" PRId64 "\n", aio->decode_packet.pts);
            fprintf(stderr, "dts       : %" PRId64 "\n", aio->decode_packet.dts);
    #endif
            av_free_packet(&(aio->decode_packet));
        } else {
    #ifdef DEBUG
            fprintf(stderr, "Reading cached frames: %i\n", aio->got_frame);
    #endif
            // flush cached frames
            av_free_packet(&(aio->decode_packet));
            res = decode_packet(aio, 1);
        }
    }

    return res;
}


/**
 * Free all resources held by aio and then itself.
 */
void ff_audioio_free(FFAudioIO *aio) {

    if (aio) {

        if (aio->encode_frame) {
            avcodec_free_frame(&aio->encode_frame);
        }
        if (aio->encode_context) {
            avcodec_close(aio->encode_context);
            av_free(aio->encode_context);
        }
        if (aio->swr_context) {
            swr_free(&aio->swr_context);
        }
        if (aio->stream && aio->stream->codec) {
            avcodec_close(aio->stream->codec);
        }
        if (aio->format_context) {
            AVFormatContext *s = aio->format_context;
            if ((s->iformat && s->iformat->flags & AVFMT_NOFILE) || (s->flags & AVFMT_FLAG_CUSTOM_IO)) {
                if (aio->format_context->pb) {
                    avio_flush(aio->format_context->pb);
                    av_free(aio->format_context->pb->buffer);
                    av_free(aio->format_context->pb);
                }
            }
            avformat_close_input(&(aio->format_context));
        }
        if (aio->decode_frame) {
            av_free(aio->decode_frame);
        }
        if (aio->audio_data) {
            av_free(aio->audio_data);
        }
        free(aio);
    }
}

/**
 * Indicates whether the given id belongs to a big endian codec.
 *
 * @param codec_id codec id
 * @return true if the codec id belongs to a big endian codec.
 */
int ff_big_endian(enum AVCodecID codec_id) {
    int big_endian = 0;
    switch (codec_id) {
        case AV_CODEC_ID_PCM_S16BE:
        case AV_CODEC_ID_PCM_U16BE:
        case AV_CODEC_ID_PCM_S32BE:
        case AV_CODEC_ID_PCM_U32BE:
        case AV_CODEC_ID_PCM_S24BE:
        case AV_CODEC_ID_PCM_U24BE:
        case AV_CODEC_ID_PCM_F32BE:
        case AV_CODEC_ID_PCM_F64BE:
            big_endian = 1;
            break;
        default:
            big_endian = 0;
    }
    return big_endian;
}


void dumpCodecIds() {

        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S16LE = %#x;\n", AV_CODEC_ID_PCM_S16LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S16BE = %#x;\n", AV_CODEC_ID_PCM_S16BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_U16LE = %#x;\n", AV_CODEC_ID_PCM_U16LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_U16BE = %#x;\n", AV_CODEC_ID_PCM_U16BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S8 = %#x;\n", AV_CODEC_ID_PCM_S8);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_U8 = %#x;\n", AV_CODEC_ID_PCM_U8);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_MULAW = %#x;\n", AV_CODEC_ID_PCM_MULAW);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_ALAW = %#x;\n", AV_CODEC_ID_PCM_ALAW);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S32LE = %#x;\n", AV_CODEC_ID_PCM_S32LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S32BE = %#x;\n", AV_CODEC_ID_PCM_S32BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_U32LE = %#x;\n", AV_CODEC_ID_PCM_U32LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_U32BE = %#x;\n", AV_CODEC_ID_PCM_U32BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S24LE = %#x;\n", AV_CODEC_ID_PCM_S24LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S24BE = %#x;\n", AV_CODEC_ID_PCM_S24BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_U24LE = %#x;\n", AV_CODEC_ID_PCM_U24LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_U24BE = %#x;\n", AV_CODEC_ID_PCM_U24BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S24DAUD = %#x;\n", AV_CODEC_ID_PCM_S24DAUD);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_ZORK = %#x;\n", AV_CODEC_ID_PCM_ZORK);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S16LE_PLANAR = %#x;\n", AV_CODEC_ID_PCM_S16LE_PLANAR);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_DVD = %#x;\n", AV_CODEC_ID_PCM_DVD);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_F32BE = %#x;\n", AV_CODEC_ID_PCM_F32BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_F32LE = %#x;\n", AV_CODEC_ID_PCM_F32LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_F64BE = %#x;\n", AV_CODEC_ID_PCM_F64BE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_F64LE = %#x;\n", AV_CODEC_ID_PCM_F64LE);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_BLURAY = %#x;\n", AV_CODEC_ID_PCM_BLURAY);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_LXF = %#x;\n", AV_CODEC_ID_PCM_LXF);
        fprintf(stdout, "private final int AV_CODEC_ID_S302M = %#x;\n", AV_CODEC_ID_S302M);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S8_PLANAR = %#x;\n", AV_CODEC_ID_PCM_S8_PLANAR);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S24LE_PLANAR = %#x;\n", AV_CODEC_ID_PCM_S24LE_PLANAR);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S32LE_PLANAR = %#x;\n", AV_CODEC_ID_PCM_S32LE_PLANAR);
        fprintf(stdout, "private final int AV_CODEC_ID_PCM_S16BE_PLANAR = %#x;\n", AV_CODEC_ID_PCM_S16BE_PLANAR);

        /* various ADPCM codecs */
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_QT = %#x;\n", AV_CODEC_ID_ADPCM_IMA_QT);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_WAV = %#x;\n", AV_CODEC_ID_ADPCM_IMA_WAV);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_DK3 = %#x;\n", AV_CODEC_ID_ADPCM_IMA_DK3);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_DK4 = %#x;\n", AV_CODEC_ID_ADPCM_IMA_DK4);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_WS = %#x;\n", AV_CODEC_ID_ADPCM_IMA_WS);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_SMJPEG = %#x;\n", AV_CODEC_ID_ADPCM_IMA_SMJPEG);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_MS = %#x;\n", AV_CODEC_ID_ADPCM_MS);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_4XM = %#x;\n", AV_CODEC_ID_ADPCM_4XM);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_XA = %#x;\n", AV_CODEC_ID_ADPCM_XA);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_ADX = %#x;\n", AV_CODEC_ID_ADPCM_ADX);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_EA = %#x;\n", AV_CODEC_ID_ADPCM_EA);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_G726 = %#x;\n", AV_CODEC_ID_ADPCM_G726);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_CT = %#x;\n", AV_CODEC_ID_ADPCM_CT);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_SWF = %#x;\n", AV_CODEC_ID_ADPCM_SWF);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_YAMAHA = %#x;\n", AV_CODEC_ID_ADPCM_YAMAHA);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_SBPRO_4 = %#x;\n", AV_CODEC_ID_ADPCM_SBPRO_4);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_SBPRO_3 = %#x;\n", AV_CODEC_ID_ADPCM_SBPRO_3);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_SBPRO_2 = %#x;\n", AV_CODEC_ID_ADPCM_SBPRO_2);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_THP = %#x;\n", AV_CODEC_ID_ADPCM_THP);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_AMV = %#x;\n", AV_CODEC_ID_ADPCM_IMA_AMV);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_EA_R1 = %#x;\n", AV_CODEC_ID_ADPCM_EA_R1);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_EA_R3 = %#x;\n", AV_CODEC_ID_ADPCM_EA_R3);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_EA_R2 = %#x;\n", AV_CODEC_ID_ADPCM_EA_R2);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_EA_SEAD = %#x;\n", AV_CODEC_ID_ADPCM_IMA_EA_SEAD);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_EA_EACS = %#x;\n", AV_CODEC_ID_ADPCM_IMA_EA_EACS);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_EA_XAS = %#x;\n", AV_CODEC_ID_ADPCM_EA_XAS);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_EA_MAXIS_XA = %#x;\n", AV_CODEC_ID_ADPCM_EA_MAXIS_XA);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_ISS = %#x;\n", AV_CODEC_ID_ADPCM_IMA_ISS);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_G722 = %#x;\n", AV_CODEC_ID_ADPCM_G722);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_APC = %#x;\n", AV_CODEC_ID_ADPCM_IMA_APC);
        fprintf(stdout, "private final int AV_CODEC_ID_VIMA = %#x;\n", AV_CODEC_ID_VIMA);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_AFC = %#x;\n", AV_CODEC_ID_ADPCM_AFC);
        fprintf(stdout, "private final int AV_CODEC_ID_ADPCM_IMA_OKI = %#x;\n", AV_CODEC_ID_ADPCM_IMA_OKI);

        /* AMR */
        fprintf(stdout, "private final int AV_CODEC_ID_AMR_NB = %#x;\n", AV_CODEC_ID_AMR_NB);
        fprintf(stdout, "private final int AV_CODEC_ID_AMR_WB = %#x;\n", AV_CODEC_ID_AMR_WB);

        /* RealAudio codecs*/
        fprintf(stdout, "private final int AV_CODEC_ID_RA_144 = %#x;\n", AV_CODEC_ID_RA_144);
        fprintf(stdout, "private final int AV_CODEC_ID_RA_288 = %#x;\n", AV_CODEC_ID_RA_288);

        /* various DPCM codecs */
        fprintf(stdout, "private final int AV_CODEC_ID_ROQ_DPCM = %#x;\n", AV_CODEC_ID_ROQ_DPCM);
        fprintf(stdout, "private final int AV_CODEC_ID_INTERPLAY_DPCM = %#x;\n", AV_CODEC_ID_INTERPLAY_DPCM);
        fprintf(stdout, "private final int AV_CODEC_ID_XAN_DPCM = %#x;\n", AV_CODEC_ID_XAN_DPCM);
        fprintf(stdout, "private final int AV_CODEC_ID_SOL_DPCM = %#x;\n", AV_CODEC_ID_SOL_DPCM);

        /* audio codecs */
        fprintf(stdout, "private final int AV_CODEC_ID_MP2 = %#x;\n", AV_CODEC_ID_MP2);
        fprintf(stdout, "private final int AV_CODEC_ID_MP3 = %#x;\n", AV_CODEC_ID_MP3); ///< preferred ID for decoding MPEG audio layer 1, 2 or 3
        fprintf(stdout, "private final int AV_CODEC_ID_AAC = %#x;\n", AV_CODEC_ID_AAC);
        fprintf(stdout, "private final int AV_CODEC_ID_AC3 = %#x;\n", AV_CODEC_ID_AC3);
        fprintf(stdout, "private final int AV_CODEC_ID_DTS = %#x;\n", AV_CODEC_ID_DTS);
        fprintf(stdout, "private final int AV_CODEC_ID_VORBIS = %#x;\n", AV_CODEC_ID_VORBIS);
        fprintf(stdout, "private final int AV_CODEC_ID_DVAUDIO = %#x;\n", AV_CODEC_ID_DVAUDIO);
        fprintf(stdout, "private final int AV_CODEC_ID_WMAV1 = %#x;\n", AV_CODEC_ID_WMAV1);
        fprintf(stdout, "private final int AV_CODEC_ID_WMAV2 = %#x;\n", AV_CODEC_ID_WMAV2);
        fprintf(stdout, "private final int AV_CODEC_ID_MACE3 = %#x;\n", AV_CODEC_ID_MACE3);
        fprintf(stdout, "private final int AV_CODEC_ID_MACE6 = %#x;\n", AV_CODEC_ID_MACE6);
        fprintf(stdout, "private final int AV_CODEC_ID_VMDAUDIO = %#x;\n", AV_CODEC_ID_VMDAUDIO);
        fprintf(stdout, "private final int AV_CODEC_ID_FLAC = %#x;\n", AV_CODEC_ID_FLAC);
        fprintf(stdout, "private final int AV_CODEC_ID_MP3ADU = %#x;\n", AV_CODEC_ID_MP3ADU);
        fprintf(stdout, "private final int AV_CODEC_ID_MP3ON4 = %#x;\n", AV_CODEC_ID_MP3ON4);
        fprintf(stdout, "private final int AV_CODEC_ID_SHORTEN = %#x;\n", AV_CODEC_ID_SHORTEN);
        fprintf(stdout, "private final int AV_CODEC_ID_ALAC = %#x;\n", AV_CODEC_ID_ALAC);
        fprintf(stdout, "private final int AV_CODEC_ID_WESTWOOD_SND1 = %#x;\n", AV_CODEC_ID_WESTWOOD_SND1);
        fprintf(stdout, "private final int AV_CODEC_ID_GSM = %#x;\n", AV_CODEC_ID_GSM); ///< as in Berlin toast format
        fprintf(stdout, "private final int AV_CODEC_ID_QDM2 = %#x;\n", AV_CODEC_ID_QDM2);
        fprintf(stdout, "private final int AV_CODEC_ID_COOK = %#x;\n", AV_CODEC_ID_COOK);
        fprintf(stdout, "private final int AV_CODEC_ID_TRUESPEECH = %#x;\n", AV_CODEC_ID_TRUESPEECH);
        fprintf(stdout, "private final int AV_CODEC_ID_TTA = %#x;\n", AV_CODEC_ID_TTA);
        fprintf(stdout, "private final int AV_CODEC_ID_SMACKAUDIO = %#x;\n", AV_CODEC_ID_SMACKAUDIO);
        fprintf(stdout, "private final int AV_CODEC_ID_QCELP = %#x;\n", AV_CODEC_ID_QCELP);
        fprintf(stdout, "private final int AV_CODEC_ID_WAVPACK = %#x;\n", AV_CODEC_ID_WAVPACK);
        fprintf(stdout, "private final int AV_CODEC_ID_DSICINAUDIO = %#x;\n", AV_CODEC_ID_DSICINAUDIO);
        fprintf(stdout, "private final int AV_CODEC_ID_IMC = %#x;\n", AV_CODEC_ID_IMC);
        fprintf(stdout, "private final int AV_CODEC_ID_MUSEPACK7 = %#x;\n", AV_CODEC_ID_MUSEPACK7);
        fprintf(stdout, "private final int AV_CODEC_ID_MLP = %#x;\n", AV_CODEC_ID_MLP);
        fprintf(stdout, "private final int AV_CODEC_ID_GSM_MS = %#x;\n", AV_CODEC_ID_GSM_MS); /* as found in WAV */
        fprintf(stdout, "private final int AV_CODEC_ID_ATRAC3 = %#x;\n", AV_CODEC_ID_ATRAC3);
        fprintf(stdout, "private final int AV_CODEC_ID_VOXWARE = %#x;\n", AV_CODEC_ID_VOXWARE);
        fprintf(stdout, "private final int AV_CODEC_ID_APE = %#x;\n", AV_CODEC_ID_APE);
        fprintf(stdout, "private final int AV_CODEC_ID_NELLYMOSER = %#x;\n", AV_CODEC_ID_NELLYMOSER);
        fprintf(stdout, "private final int AV_CODEC_ID_MUSEPACK8 = %#x;\n", AV_CODEC_ID_MUSEPACK8);
        fprintf(stdout, "private final int AV_CODEC_ID_SPEEX = %#x;\n", AV_CODEC_ID_SPEEX);
        fprintf(stdout, "private final int AV_CODEC_ID_WMAVOICE = %#x;\n", AV_CODEC_ID_WMAVOICE);
        fprintf(stdout, "private final int AV_CODEC_ID_WMAPRO = %#x;\n", AV_CODEC_ID_WMAPRO);
        fprintf(stdout, "private final int AV_CODEC_ID_WMALOSSLESS = %#x;\n", AV_CODEC_ID_WMALOSSLESS);
        fprintf(stdout, "private final int AV_CODEC_ID_ATRAC3P = %#x;\n", AV_CODEC_ID_ATRAC3P);
        fprintf(stdout, "private final int AV_CODEC_ID_EAC3 = %#x;\n", AV_CODEC_ID_EAC3);
        fprintf(stdout, "private final int AV_CODEC_ID_SIPR = %#x;\n", AV_CODEC_ID_SIPR);
        fprintf(stdout, "private final int AV_CODEC_ID_MP1 = %#x;\n", AV_CODEC_ID_MP1);
        fprintf(stdout, "private final int AV_CODEC_ID_TWINVQ = %#x;\n", AV_CODEC_ID_TWINVQ);
        fprintf(stdout, "private final int AV_CODEC_ID_TRUEHD = %#x;\n", AV_CODEC_ID_TRUEHD);
        fprintf(stdout, "private final int AV_CODEC_ID_MP4ALS = %#x;\n", AV_CODEC_ID_MP4ALS);
        fprintf(stdout, "private final int AV_CODEC_ID_ATRAC1 = %#x;\n", AV_CODEC_ID_ATRAC1);
        fprintf(stdout, "private final int AV_CODEC_ID_BINKAUDIO_RDFT = %#x;\n", AV_CODEC_ID_BINKAUDIO_RDFT);
        fprintf(stdout, "private final int AV_CODEC_ID_BINKAUDIO_DCT = %#x;\n", AV_CODEC_ID_BINKAUDIO_DCT);
        fprintf(stdout, "private final int AV_CODEC_ID_AAC_LATM = %#x;\n", AV_CODEC_ID_AAC_LATM);
        fprintf(stdout, "private final int AV_CODEC_ID_QDMC = %#x;\n", AV_CODEC_ID_QDMC);
        fprintf(stdout, "private final int AV_CODEC_ID_CELT = %#x;\n", AV_CODEC_ID_CELT);
        fprintf(stdout, "private final int AV_CODEC_ID_G723_1 = %#x;\n", AV_CODEC_ID_G723_1);
        fprintf(stdout, "private final int AV_CODEC_ID_G729 = %#x;\n", AV_CODEC_ID_G729);
        fprintf(stdout, "private final int AV_CODEC_ID_8SVX_EXP = %#x;\n", AV_CODEC_ID_8SVX_EXP);
        fprintf(stdout, "private final int AV_CODEC_ID_8SVX_FIB = %#x;\n", AV_CODEC_ID_8SVX_FIB);
        fprintf(stdout, "private final int AV_CODEC_ID_BMV_AUDIO = %#x;\n", AV_CODEC_ID_BMV_AUDIO);
        fprintf(stdout, "private final int AV_CODEC_ID_RALF = %#x;\n", AV_CODEC_ID_RALF);
        fprintf(stdout, "private final int AV_CODEC_ID_IAC = %#x;\n", AV_CODEC_ID_IAC);
        fprintf(stdout, "private final int AV_CODEC_ID_ILBC = %#x;\n", AV_CODEC_ID_ILBC);
        fprintf(stdout, "private final int AV_CODEC_ID_OPUS_DEPRECATED = %#x;\n", AV_CODEC_ID_OPUS_DEPRECATED);
        fprintf(stdout, "private final int AV_CODEC_ID_COMFORT_NOISE = %#x;\n", AV_CODEC_ID_COMFORT_NOISE);
        fprintf(stdout, "private final int AV_CODEC_ID_TAK_DEPRECATED = %#x;\n", AV_CODEC_ID_TAK_DEPRECATED);
        fprintf(stdout, "private final int AV_CODEC_ID_FFWAVESYNTH = %#x;\n", AV_CODEC_ID_FFWAVESYNTH);
        fprintf(stdout, "private final int AV_CODEC_ID_8SVX_RAW = %#x;\n", AV_CODEC_ID_8SVX_RAW);
        fprintf(stdout, "private final int AV_CODEC_ID_SONIC = %#x;\n", AV_CODEC_ID_SONIC);
        fprintf(stdout, "private final int AV_CODEC_ID_SONIC_LS = %#x;\n", AV_CODEC_ID_SONIC_LS);
        fprintf(stdout, "private final int AV_CODEC_ID_PAF_AUDIO = %#x;\n", AV_CODEC_ID_PAF_AUDIO);
        fprintf(stdout, "private final int AV_CODEC_ID_OPUS = %#x;\n", AV_CODEC_ID_OPUS);
        fprintf(stdout, "private final int AV_CODEC_ID_TAK = %#x;\n", AV_CODEC_ID_TAK);
        fprintf(stdout, "private final int AV_CODEC_ID_EVRC = %#x;\n", AV_CODEC_ID_EVRC);
        fprintf(stdout, "private final int AV_CODEC_ID_SMV = %#x;\n", AV_CODEC_ID_SMV);

}

/**
 * Make sure FFmpeg is initialized all the way.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    av_register_all();
    avformat_network_init();
    avcodec_register_all();
    return JNI_VERSION_1_6;
}