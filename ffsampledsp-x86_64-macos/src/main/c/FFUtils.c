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

const uint32_t CODEC_TAG_DRMS = 'smrd'; // 'drms'

static jfieldID nativeBuffer_FID = NULL;
static jmethodID rewind_MID = NULL;
static jmethodID limit_MID = NULL;
static jmethodID capacity_MID = NULL;
static jmethodID setNativeBufferCapacity_MID = NULL;
static jmethodID logFine_MID = NULL;
static jmethodID logWarning_MID = NULL;
static const int MIN_PROBE_SCORE =
    5; // this is fairly arbitrary, but we need to give other
       // javax.sound.sampled impls a chance
// Minimum bytes to fill per ff_fill_buffer() call; reduces JNI roundtrips for
// bulk use.
static const int FF_FILL_TARGET = 64 * 1024;

/**
 * Init static method and field ids for Java methods/fields, if we don't have
 * them already.
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

    nativeBuffer_FID = (*env)->GetFieldID(env, streamClass, "nativeBuffer",
                                          "Ljava/nio/ByteBuffer;");
    rewind_MID =
        (*env)->GetMethodID(env, bufferClass, "rewind", "()Ljava/nio/Buffer;");
    limit_MID =
        (*env)->GetMethodID(env, bufferClass, "limit", "(I)Ljava/nio/Buffer;");
    capacity_MID = (*env)->GetMethodID(env, bufferClass, "capacity", "()I");
    setNativeBufferCapacity_MID = (*env)->GetMethodID(
        env, streamClass, "setNativeBufferCapacity", "(I)I");
    logFine_MID = (*env)->GetMethodID(env, streamClass, "logFine",
                                      "(Ljava/lang/String;)V");
    logWarning_MID = (*env)->GetMethodID(env, streamClass, "logWarning",
                                         "(Ljava/lang/String;)V");
  }
}

/**
 * Opens the given stream, i.e. sets up a decoder.
 *
 * @param env JNIEnv
 * @param stream AVStream
 */
int ff_open_stream(JNIEnv *env, AVStream *stream, AVCodecContext **context) {
#ifdef DEBUG
  fprintf(stderr, "Opening stream...\n");
#endif

  int res = 0;
  AVCodec *decoder = NULL;

  decoder = avcodec_find_decoder(stream->codecpar->codec_id);
  if (!decoder) {
    fprintf(stderr, "Failed to find %s codec\n",
            av_get_media_type_string(AVMEDIA_TYPE_AUDIO));
    res = AVERROR(EINVAL);
    throwUnsupportedAudioFileExceptionIfError(env, res,
                                              "Failed to find codec.");
    goto bail;
  }
  *context = avcodec_alloc_context3(decoder);
  if (!*context) {
    fprintf(stderr, "Failed to allocate context\n");
    res = AVERROR(ENOMEM);
    throwUnsupportedAudioFileExceptionIfError(
        env, res, "Failed to allocate codec context.");
    goto bail;
  }

  /* Copy codec parameters from input stream to output codec context */
  if ((res = avcodec_parameters_to_context(*context, stream->codecpar)) < 0) {
    fprintf(stderr, "Failed to copy %s codec parameters to decoder context\n",
            av_get_media_type_string(AVMEDIA_TYPE_AUDIO));
    throwUnsupportedAudioFileExceptionIfError(
        env, res, "Failed to copy codec parameters.");
    goto bail;
  }

  if ((res = avcodec_open2(*context, decoder, NULL)) < 0) {
    fprintf(stderr, "Failed to open %s codec\n",
            av_get_media_type_string(AVMEDIA_TYPE_AUDIO));
    throwUnsupportedAudioFileExceptionIfError(env, res,
                                              "Failed to open codec.");
    goto bail;
  }

#ifdef DEBUG
  fprintf(stderr, "Stream was opened.\n");
#endif
  return res;

bail:
  return res;
}

/**
 * Find a decoder and open it for the given AVFormatContext and AVMediaType.
 *
 * @param[out]  stream_index  index of the stream the decoder was opened for
 * @param[in]   format_context     format context
 * @param       type        media type - for this library typically audio
 */
static int open_codec_context(int *stream_index,
                              AVFormatContext *format_context,
                              enum AVMediaType type) {
  int res = av_find_best_stream(format_context, type, -1, -1, NULL, 0);
  if (res >= 0)
    *stream_index = res;
  return res;
}

/**
 * Throws an UnsupportedAudioFileException.
 */
void throwUnsupportedAudioFileExceptionIfError(JNIEnv *env, int err,
                                               const char *message) {
  if (err) {
    char formattedMessage[strlen(message) + 4 + AV_ERROR_MAX_STRING_SIZE];
    snprintf(formattedMessage, strlen(message) + 4 + AV_ERROR_MAX_STRING_SIZE,
             "%s (%.64s)", message, av_err2str(err));
#ifdef DEBUG
    fprintf(stderr, "UnsupportedAudioFileException: %s\n", formattedMessage);
#endif
    jclass excCls = (*env)->FindClass(
        env, "javax/sound/sampled/UnsupportedAudioFileException");
    (*env)->ThrowNew(env, excCls, formattedMessage);
  }
}

/**
 * Throws an IndexOutOfBoundsException.
 */
void throwIndexOutOfBoundsExceptionIfError(JNIEnv *env, int err, int index) {
  if (err) {
    char formattedMessage[15];
    snprintf(formattedMessage, 15, "%d", index);
#ifdef DEBUG
    fprintf(stderr, "IndexOutOfBoundsException: %d\n", index);
#endif
    jclass excCls =
        (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException");
    (*env)->ThrowNew(env, excCls, formattedMessage);
  }
}

/**
 * Throws an IOException.
 */
void throwIOExceptionIfError(JNIEnv *env, int err, const char *message) {
  if (err) {
    char formattedMessage[strlen(message) + 4 + AV_ERROR_MAX_STRING_SIZE];
    snprintf(formattedMessage, strlen(message) + 4 + AV_ERROR_MAX_STRING_SIZE,
             "%s (%.64s)", message, av_err2str(err));
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
void throwFileNotFoundExceptionIfError(JNIEnv *env, int err,
                                       const char *message) {
  if (err) {
#ifdef DEBUG
    fprintf(stderr, "FileNotFoundException: '%s' %d (%4.4s)\n", message,
            (int)err, (char *)&err);
#endif
    jclass excCls = (*env)->FindClass(env, "java/io/FileNotFoundException");
    (*env)->ThrowNew(env, excCls, message);
  }
}

/**
 * Log a warning.
 */
void logWarning(FFAudioIO *aio, int err, const char *message) {
  if (err) {
    char formattedMessage[strlen(message) + 20 + AV_ERROR_MAX_STRING_SIZE];
    snprintf(formattedMessage, strlen(message) + 20 + AV_ERROR_MAX_STRING_SIZE,
             "%s %i (%.64s)", message, err, av_err2str(err));
    jstring s = (*aio->env)->NewStringUTF(aio->env, formattedMessage);
    (*aio->env)->CallVoidMethod(aio->env, aio->java_instance, logWarning_MID,
                                s);
  } else {
    jstring s = (*aio->env)->NewStringUTF(aio->env, message);
    (*aio->env)->CallVoidMethod(aio->env, aio->java_instance, logWarning_MID,
                                s);
  }
}

/**
 * Log a debug message.
 */
void logFine(FFAudioIO *aio, int err, const char *message) {
  if (err) {
    char formattedMessage[strlen(message) + 20 + AV_ERROR_MAX_STRING_SIZE];
    snprintf(formattedMessage, strlen(message) + 20 + AV_ERROR_MAX_STRING_SIZE,
             "%s %i (%.64s)", message, err, av_err2str(err));
    jstring s = (*aio->env)->NewStringUTF(aio->env, formattedMessage);
    (*aio->env)->CallVoidMethod(aio->env, aio->java_instance, logFine_MID, s);
  } else {
    jstring s = (*aio->env)->NewStringUTF(aio->env, message);
    (*aio->env)->CallVoidMethod(aio->env, aio->java_instance, logFine_MID, s);
  }
}

/**
 * Opens the input file/url and allocates a AVFormatContext for it, but does not
 * open the audio stream with an appropriate decoder.
 *
 * @param env JNIEnv
 * @param format_context AVFormatContext
 * @param url URL to open
 * @return negative value, if something went wrong
 */
int ff_open_format_context(JNIEnv *env, AVFormatContext **format_context,
                           const char *url, int io_buffer_size) {
  int res = 0;
  int probe_score = 0;

  res = avformat_open_input(format_context, url, NULL, NULL);
  if (res) {
    if (res == AVERROR(ENOENT) || res == AVERROR_HTTP_NOT_FOUND) {
      throwFileNotFoundExceptionIfError(env, res, url);
    } else if (res == AVERROR_PROTOCOL_NOT_FOUND ||
               res == AVERROR_HTTP_BAD_REQUEST ||
               res == AVERROR_HTTP_UNAUTHORIZED ||
               res == AVERROR_HTTP_FORBIDDEN || res == AVERROR_HTTP_OTHER_4XX ||
               res == AVERROR_HTTP_SERVER_ERROR || res == AVERROR(EIO)) {
      throwIOExceptionIfError(env, res, url);
    } else {
      throwUnsupportedAudioFileExceptionIfError(env, res,
                                                "Failed to open audio file");
    }
    goto bail;
  }

  probe_score = (*format_context)->probe_score;

#ifdef DEBUG
  fprintf(stderr, "ff_open_format_context(): probe score=%i\n", probe_score);
#endif

  if (probe_score < MIN_PROBE_SCORE) {
    res = probe_score;
    throwUnsupportedAudioFileExceptionIfError(env, probe_score,
                                              "Probe score too low");
    goto bail;
  }

  res = avformat_find_stream_info(*format_context, NULL);
  if (res < 0) {
    throwUnsupportedAudioFileExceptionIfError(env, res,
                                              "Failed to find stream info");
    goto bail;
  }

  if (io_buffer_size > 0 && (*format_context)->pb) {
    AVIOContext *pb = (*format_context)->pb;
    if (pb->buffer_size < io_buffer_size) {
      unsigned char *new_buf = (unsigned char *)av_malloc(io_buffer_size);
      if (new_buf) {
        int valid = (int)(pb->buf_end - pb->buf_ptr);
        if (valid > 0)
          memcpy(new_buf, pb->buf_ptr, valid);
        av_free(pb->buffer);
        pb->buffer = new_buf;
        pb->buf_ptr = new_buf;
        pb->buf_end = new_buf + valid;
        pb->buffer_size = io_buffer_size;
        pb->buf_ptr_max = new_buf + valid;
        pb->checksum_ptr = NULL;
        pb->checksum = 0;
        pb->update_checksum = NULL;
      }
    }
  }

bail:

  return res;
}

/**
 * Opens the input file/url, allocates a AVFormatContext for it and opens the
 * audio stream with an appropriate decoder.
 *
 * @param env JNIEnv
 * @param format_context AVFormatContext
 * @param openedStream opened audio AVStream
 * @param stream_index[in] index of the desired <em>audio</em> stream
 * @param stream_index[out] index of the selected stream (index of <em>all</em>
 * streams)
 * @param url URL to open
 * @return negative value, if something went wrong
 */
int ff_open_file(JNIEnv *env, AVFormatContext **format_context,
                 AVStream **openedStream, AVCodecContext **context,
                 int *stream_index, const char *url, int io_buffer_size) {
  int res = 0;
  res = ff_open_format_context(env, format_context, url, io_buffer_size);
  if (res) {
    // exception has already been thrown
    goto bail;
  }

#ifdef DEBUG
  fprintf(stderr, "Desired audio stream index: %i.\n", *stream_index);
#endif

  if (*stream_index < 0) {
    // use best audio stream
    res = open_codec_context(stream_index, *format_context, AVMEDIA_TYPE_AUDIO);
    if (res) {
      throwUnsupportedAudioFileExceptionIfError(
          env, res, "Failed to find best audio stream.");
      goto bail;
    }
    *openedStream = (*format_context)->streams[*stream_index];
    res = ff_open_stream(env, *openedStream, context);
    if (res) {
      goto bail;
    }
  } else {
    // find xth audio stream
    // count possible audio streams
    int i;
    int audio_stream_number = 0;
    AVStream *stream = NULL;

    AVFormatContext *deref_format_context = *format_context;
    for (i = 0; i < deref_format_context->nb_streams; i++) {
      stream = deref_format_context->streams[i];
      if (stream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
        if (audio_stream_number == *stream_index) {
          *stream_index = i;
#ifdef DEBUG
          fprintf(stderr, "Found desired audio stream at index: %i.\n", i);
#endif
          break;
        }
        audio_stream_number++;
      }
      stream = NULL;
    }
    if (stream == NULL) {
      // we didn't find a stream with the given index
      res = -1;
      if (*stream_index == 0) {
        throwUnsupportedAudioFileExceptionIfError(env, res,
                                                  "No stream found (index=0).");
      } else {
        throwIndexOutOfBoundsExceptionIfError(env, res, *stream_index);
      }
      goto bail;
    }
    res = ff_open_stream(env, stream, context);
    if (res) {
      goto bail;
    }
    *openedStream = stream;
  }

  if ((*openedStream)->codecpar->codec_tag == CODEC_TAG_DRMS) {
    fprintf(stderr, "File is DRM-crippled.\n");
    res = -1;
    throwUnsupportedAudioFileExceptionIfError(env, res,
                                              "File is DRM-crippled.");
    goto bail;
  }

#ifdef DEBUG
  fprintf(stderr, "Opened stream index: %i.\n", *stream_index);
  fprintf(stderr, "Opened stream: %ld.\n", (long)*openedStream);
#endif

bail:

  return res;
}

/**
 * Allocates and initializes the SwrContext so that we don't have to deal with
 * planar sample formats.
 *
 * @param env JNIEnv
 * @param aio FFAudioIO
 * @return a negative value should an error occur
 */
static int init_swr(JNIEnv *env, FFAudioIO *aio) {
  int res = 0;
  AVChannelLayout ch_layout = {0};

  // If the stream has no explicit channel layout, derive a default from the
  // channel count.
  if (aio->stream->codecpar->ch_layout.order == AV_CHANNEL_ORDER_UNSPEC) {
    av_channel_layout_default(&ch_layout,
                              aio->stream->codecpar->ch_layout.nb_channels);
  } else {
    av_channel_layout_copy(&ch_layout, &aio->stream->codecpar->ch_layout);
  }

  aio->swr_context = swr_alloc();
  if (!aio->swr_context) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(env, res, "Could not allocate swr context.");
    goto bail;
  }

  av_opt_set_sample_fmt(aio->swr_context, "in_sample_fmt",
                        aio->stream->codecpar->format, 0);
  // make sure we get interleaved/packed output
  av_opt_set_sample_fmt(aio->swr_context, "out_sample_fmt",
                        av_get_packed_sample_fmt(aio->stream->codecpar->format),
                        0);

  av_opt_set_chlayout(aio->swr_context, "in_chlayout", &ch_layout, 0);
  av_opt_set_chlayout(aio->swr_context, "out_chlayout", &ch_layout, 0);
  av_opt_set_int(aio->swr_context, "in_sample_rate",
                 aio->stream->codecpar->sample_rate, 0);
  av_opt_set_int(aio->swr_context, "out_sample_rate",
                 aio->stream->codecpar->sample_rate, 0);

  res = swr_init(aio->swr_context);
  if (res < 0) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(env, res, "Could not initialize swr context");
    goto bail;
  }

bail:
  av_channel_layout_uninit(&ch_layout);
  return res;
}

/**
 * Finds an AVCodec encoder for the given sample format, bits per sample, byte
 * order and signed/unsigned encoding. This method never returns a planar, but
 * always a packed codec.
 *
 * @param sampleFormat  AVSampleFormat
 * @param bits          bits per sample
 * @param big_endian     true or false
 * @param signedSamples true, if the samples are signed
 * @return an appropriate encoder or NULL, if none can be found
 */
AVCodec *ff_find_encoder(enum AVSampleFormat sampleFormat, int bits,
                         int big_endian, int signedSamples) {
  enum AVCodecID codec_id;

#ifdef DEBUG
  fprintf(stderr, "ff_find_encoder(fmt=%d,bits=%d,big_endian=%d,signed=%d)\n",
          sampleFormat, bits, big_endian, signedSamples);
#endif

  switch (sampleFormat) {
  case AV_SAMPLE_FMT_U8:
  case AV_SAMPLE_FMT_U8P:
    if (signedSamples)
      codec_id = AV_CODEC_ID_PCM_S8;
    else
      codec_id = AV_CODEC_ID_PCM_U8;
    break;
  case AV_SAMPLE_FMT_S16:
  case AV_SAMPLE_FMT_S16P:
    if (signedSamples)
      codec_id = big_endian ? AV_CODEC_ID_PCM_S16BE : AV_CODEC_ID_PCM_S16LE;
    else
      codec_id = big_endian ? AV_CODEC_ID_PCM_U16BE : AV_CODEC_ID_PCM_U16LE;
    break;
  case AV_SAMPLE_FMT_S32:
  case AV_SAMPLE_FMT_S32P:
    if (bits == 24) {
      if (signedSamples)
        codec_id = big_endian ? AV_CODEC_ID_PCM_S24BE : AV_CODEC_ID_PCM_S24LE;
      else
        codec_id = big_endian ? AV_CODEC_ID_PCM_U24BE : AV_CODEC_ID_PCM_U24LE;
    } else {
      if (signedSamples)
        codec_id = big_endian ? AV_CODEC_ID_PCM_S32BE : AV_CODEC_ID_PCM_S32LE;
      else
        codec_id = big_endian ? AV_CODEC_ID_PCM_U32BE : AV_CODEC_ID_PCM_U32LE;
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
  AVChannelLayout out_ch_layout = {0};
  enum AVSampleFormat out_sample_fmt;

  // make sure we clean up before resetting this
  // in case this is called twice
  if (aio->encode_frame) {
    av_frame_free(&aio->encode_frame);
  }
  if (aio->encode_context) {
    avcodec_free_context(&aio->encode_context);
  }

  aio->encode_context = avcodec_alloc_context3(encoder);
  if (!aio->encode_context) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(env, res, "Could not allocate codec context.");
    goto bail;
  }

  // init to whatever we have in SwrContext
  av_opt_get_chlayout(aio->swr_context, "out_chlayout", 0, &out_ch_layout);
  av_opt_get_int(aio->swr_context, "out_sample_rate", 0, &out_sample_rate);
  av_opt_get_sample_fmt(aio->swr_context, "out_sample_fmt", 0, &out_sample_fmt);

  aio->encode_context->sample_fmt = out_sample_fmt;
  aio->encode_context->sample_rate = (int)out_sample_rate;
  av_channel_layout_copy(&aio->encode_context->ch_layout, &out_ch_layout);

  res = avcodec_open2(aio->encode_context, encoder, NULL);
  if (res < 0) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(env, res, "Could not open encoder.");
    goto bail;
  }

  if (!aio->encode_packet) {
    aio->encode_packet = av_packet_alloc();
    if (!aio->encode_packet) {
      res = AVERROR(ENOMEM);
      throwIOExceptionIfError(env, res, "Could not allocate encoder packet.");
      goto bail;
    }
  }

  aio->encode_frame = av_frame_alloc();
  if (!aio->encode_frame) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(env, res, "Could not allocate encoder frame.");
    goto bail;
  }
  aio->encode_frame->nb_samples =
      aio->encode_context->frame_size; // will be updated per-call
  aio->encode_frame->format = aio->encode_context->sample_fmt;
  av_channel_layout_copy(&aio->encode_frame->ch_layout,
                         &aio->encode_context->ch_layout);

bail:
  av_channel_layout_uninit(&out_ch_layout);
  return res;
}

/**
 * Initialize our main context FFAudioIO, so that SwrContext, decode buffers and
 * the encoder are set to reasonable values.
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
  nb_planes = av_sample_fmt_is_planar(aio->stream->codecpar->format)
                  ? aio->stream->codecpar->ch_layout.nb_channels
                  : 1;

  // always init SWR to keep code simpler
  res = init_swr(env, aio);
  if (res < 0) {
    // exception is already thrown
    goto bail;
  }
  // if for some reason the codec delivers 24bit, we need to encode its output
  // to little endian
  if (aio->stream->codecpar->bits_per_coded_sample == 24) {
    codec = ff_find_encoder(aio->stream->codecpar->format,
                            aio->stream->codecpar->bits_per_coded_sample,
                            ff_big_endian(aio->stream->codecpar->codec_id), 1);
    if (!codec) {
      res = AVERROR(EINVAL);
      throwIOExceptionIfError(env, res,
                              "Could not find suitable encoder codec.");
      goto bail;
    }
    res = ff_init_encoder(env, aio, codec);
    if (res < 0) {
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

  aio->decode_frame = av_frame_alloc();
  if (!aio->decode_frame) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(env, res, "Could not allocate frame.");
    goto bail;
  }

  // initialize packet
  aio->decode_packet = av_packet_alloc();
  if (!aio->decode_packet) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(env, res, "Could not allocate decode packet.");
    goto bail;
  }

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
static int encode_buffer(FFAudioIO *aio, const uint8_t *in_buf, int in_size,
                         const uint8_t *out_buf) {
  int res = 0;
  int nb_channels = aio->encode_context->ch_layout.nb_channels;

  res = av_samples_get_buffer_size(NULL, nb_channels,
                                   aio->encode_frame->nb_samples,
                                   aio->encode_context->sample_fmt, 1);

#ifdef DEBUG
  fprintf(stderr,
          "encode_buffer: channels=%d frame->nb_samples=%d in_size=%d\n",
          nb_channels, aio->encode_frame->nb_samples, in_size);
  fprintf(stderr, "encode_buffer: needed buffer=%d available=%d\n", res,
          in_size);
#endif

  // point the AVFrame's data arrays at in_buf (no copy)
  res = av_samples_fill_arrays(aio->encode_frame->data, NULL, in_buf,
                               nb_channels, aio->encode_frame->nb_samples,
                               aio->encode_context->sample_fmt, 1);
  if (res < 0) {
    throwIOExceptionIfError(aio->env, res, "Failed to fill audio frame.");
    goto bail;
  }

  av_packet_unref(aio->encode_packet);

  res = avcodec_send_frame(aio->encode_context, aio->encode_frame);
  if (res < 0) {
    throwIOExceptionIfError(aio->env, res,
                            "Failed to send audio frame for encoding.");
    goto bail;
  }
  res = avcodec_receive_packet(aio->encode_context, aio->encode_packet);
  if (res < 0 && res != AVERROR(EAGAIN) && res != AVERROR_EOF) {
    throwIOExceptionIfError(aio->env, res, "Failed to receive encoded packet.");
    goto bail;
  }
  if (res == 0) {
    res = aio->encode_packet->size;
    memcpy((char *)out_buf, aio->encode_packet->data, aio->encode_packet->size);
    av_packet_unref(aio->encode_packet);
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
 * @return number of samples copied/converted or a negative value, should things
 * go wrong
 */
static int resample(FFAudioIO *aio, uint8_t **out_buf, int out_samples,
                    const uint8_t **in_buf, const int in_samples) {
  int res = 0;
  AVChannelLayout out_ch_layout = {0};
  int out_channel_count;
  enum AVSampleFormat out_sample_format;

  if (out_samples == 0)
    goto bail; // nothing to do.

  av_opt_get_chlayout(aio->swr_context, "out_chlayout", 0, &out_ch_layout);
  out_channel_count = out_ch_layout.nb_channels;
  av_channel_layout_uninit(&out_ch_layout);
  av_opt_get_sample_fmt(aio->swr_context, "out_sample_fmt", 0,
                        &out_sample_format);

#ifdef DEBUG
  fprintf(
      stderr,
      "resample: out_samples=%d in_samples=%d, channels=%d sample_format=%d\n",
      out_samples, in_samples, out_channel_count, out_sample_format);
#endif

  // allocate temp buffer for resampled data
  res = av_samples_alloc(out_buf, NULL, out_channel_count, out_samples,
                         out_sample_format, 1);
  if (res < 0) {
    res = AVERROR(ENOMEM);
    throwIOExceptionIfError(aio->env, res,
                            "Could not allocate resample buffer.");
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

static int copy_to_java_buffer(FFAudioIO *aio, uint32_t offset, int samples,
                               uint8_t **resample_buf) {
  int res = 0;
  uint32_t buffer_size = 0;
  jobject byte_buffer = NULL;
  uint8_t *java_buffer = NULL;
  AVChannelLayout out_ch_layout = {0};
  int channel_count;
  enum AVSampleFormat format;

  av_opt_get_sample_fmt(aio->swr_context, "out_sample_fmt", 0, &format);
  av_opt_get_chlayout(aio->swr_context, "out_chlayout", 0, &out_ch_layout);
  channel_count = out_ch_layout.nb_channels;
  av_channel_layout_uninit(&out_ch_layout);

  res = av_samples_get_buffer_size(NULL, channel_count, samples, format, 1);
  if (res < 0)
    goto bail;
  else
    buffer_size = res;

  // ensure native buffer capacity
  if (aio->java_buffer_capacity < buffer_size + offset) {
    jint new_capacity = (*aio->env)->CallIntMethod(
        aio->env, aio->java_instance, setNativeBufferCapacity_MID,
        (jint)(buffer_size + offset));
    if ((*aio->env)->ExceptionCheck(aio->env)) {
      logWarning(aio, -1, "Failed to resize native Java buffer.");
      (*aio->env)->CallObjectMethod(aio->env, byte_buffer, rewind_MID);
      res = -1;
      goto bail;
    }
    aio->java_buffer_capacity = new_capacity;
  }
  // get java-managed byte buffer reference
  byte_buffer = (*aio->env)->GetObjectField(aio->env, aio->java_instance,
                                            nativeBuffer_FID);
  if (!byte_buffer) {
    res = -1;
    throwIOExceptionIfError(aio->env, 1, "Failed to get native buffer.");
    goto bail;
  }

  // we have some samples, let's copy them to the java buffer, using the desired
  // encoding at the desired offset
  java_buffer =
      (uint8_t *)(*aio->env)->GetDirectBufferAddress(aio->env, byte_buffer) +
      offset;
  if (!java_buffer) {
    throwIOExceptionIfError(aio->env, 1,
                            "Failed to get address for native buffer.");
    goto bail;
  }
  if (aio->encode_context) {
    aio->encode_frame->nb_samples = samples;
    res = encode_buffer(aio, resample_buf[0], buffer_size, java_buffer);
    if (res < 0) {
      buffer_size = 0;
      goto bail;
    }
    buffer_size = res;
  } else {
    memcpy(java_buffer, resample_buf[0], buffer_size);
  }
  // we already wrote to the buffer, now we still need to
  // set new bytebuffer limit and position to 0.
  if (offset == 0) {
    // only rewind, if this is the first call for this packet
    (*aio->env)->CallObjectMethod(aio->env, byte_buffer, rewind_MID);
  }
  (*aio->env)->CallObjectMethod(aio->env, byte_buffer, limit_MID,
                                offset + buffer_size);

  aio->resampled_samples += buffer_size;

bail:

  return res;
}

/**
 * Receive all available decoded frames from the decoder, resample/encode them,
 * and copy results to the Java-side native buffer.
 *
 * @param aio           FFAudioIO context
 * @param is_flush      non-zero when called after sending a flush (NULL) packet
 * @return bytes written to the Java buffer, or a negative error code
 */
static int receive_and_process_frames(FFAudioIO *aio, int is_flush,
                                      uint32_t initial_offset) {
  int res = 0;
  int total_bytes = 0;
  uint8_t **resample_buf = NULL;
  uint32_t java_buffer_offset = initial_offset;
  uint32_t out_buf_size = 0;
  int out_buf_samples = 0;
  int64_t out_sample_rate;

  init_ids(aio->env, aio->java_instance);
  av_opt_get_int(aio->swr_context, "out_sample_rate", 0, &out_sample_rate);

  resample_buf = av_mallocz(sizeof(uint8_t *));
  if (!resample_buf) {
    throwIOExceptionIfError(aio->env, AVERROR(ENOMEM),
                            "Could not allocate resample buffer pointer.");
    return AVERROR(ENOMEM);
  }

  while (1) {
    res = avcodec_receive_frame(aio->decode_context, aio->decode_frame);
    if (res == AVERROR(EAGAIN) || res == AVERROR_EOF) {
      res = 0;
      break;
    }
    if (res < 0) {
      logWarning(aio, res, "Error receiving frame from decoder:");
      res = 0;
      break;
    }

    if (aio->stream->codecpar->ch_layout.nb_channels !=
        aio->decode_frame->ch_layout.nb_channels) {
      logWarning(aio, 0, "Skipping frame: channel count mismatch");
      av_frame_unref(aio->decode_frame);
      continue;
    }

    aio->decoded_samples += aio->decode_frame->nb_samples;

#ifdef DEBUG
    fprintf(
        stderr, "samples%s n:%" PRIu64 " nb_samples:%d pts:%s\n",
        is_flush ? "(flush)" : "", aio->decoded_samples,
        aio->decode_frame->nb_samples,
        av_ts2timestr(aio->decode_frame->pts, &aio->decode_context->time_base));
#endif

    out_buf_samples = av_rescale_rnd(
        swr_get_delay(aio->swr_context, aio->stream->codecpar->sample_rate) +
            aio->decode_frame->nb_samples,
        out_sample_rate, aio->stream->codecpar->sample_rate, AV_ROUND_UP);

    res = av_samples_alloc(
        aio->audio_data, NULL, aio->decode_frame->ch_layout.nb_channels,
        aio->decode_frame->nb_samples, aio->decode_frame->format, 1);
    if (res < 0) {
      throwIOExceptionIfError(aio->env, res,
                              "Could not allocate audio buffer.");
      goto bail;
    }
    av_samples_copy(aio->audio_data, aio->decode_frame->data, 0, 0,
                    aio->decode_frame->nb_samples,
                    aio->decode_frame->ch_layout.nb_channels,
                    aio->decode_frame->format);

    res = resample(aio, resample_buf, out_buf_samples,
                   (const uint8_t **)aio->audio_data,
                   aio->decode_frame->nb_samples);
    if (res < 0)
      goto bail;
    out_buf_samples = res;

    if (out_buf_samples > 0 && resample_buf[0]) {
      java_buffer_offset += out_buf_size;
      res = copy_to_java_buffer(aio, java_buffer_offset, out_buf_samples,
                                resample_buf);
      if (res < 0)
        goto bail;
      total_bytes += res;
      out_buf_size = res;
    }
    if (resample_buf[0])
      av_freep(&resample_buf[0]);
    if (aio->audio_data[0])
      av_freep(&aio->audio_data[0]);
    av_frame_unref(aio->decode_frame);
  }

  // After the decoder is fully flushed, drain any remaining SwrContext delay.
  if (is_flush) {
    int64_t delay =
        swr_get_delay(aio->swr_context, aio->stream->codecpar->sample_rate);
    if (delay > 0) {
#ifdef DEBUG
      fprintf(stderr, "Flushing SWR delay: %" PRId64 " samples\n", delay);
#endif
      res = resample(aio, resample_buf, (int)delay, NULL, 0);
      if (res < 0)
        goto bail;
      out_buf_samples = res;
      if (out_buf_samples > 0 && resample_buf[0]) {
        java_buffer_offset += out_buf_size;
        res = copy_to_java_buffer(aio, java_buffer_offset, out_buf_samples,
                                  resample_buf);
        if (res < 0)
          goto bail;
        total_bytes += res;
        out_buf_size = res;
      }
      if (resample_buf[0])
        av_freep(&resample_buf[0]);
    }
  }

bail:
  if (resample_buf) {
    if (resample_buf[0])
      av_freep(&resample_buf[0]);
    av_free(resample_buf);
  }
  if (aio->audio_data[0])
    av_freep(&aio->audio_data[0]);
  return res < 0 ? res : total_bytes;
}

/**
 * Send one packet to the decoder and process all resulting frames.
 *
 * @param aio   FFAudioIO context (aio->decode_packet contains the packet to
 * decode)
 * @return bytes written to the Java buffer, or a negative error code
 */
static int decode_packet(FFAudioIO *aio, uint32_t offset) {
  int res = avcodec_send_packet(aio->decode_context, aio->decode_packet);
  if (res == AVERROR(EINVAL)) {
    throwUnsupportedAudioFileExceptionIfError(aio->env, res,
                                              "Invalid argument for decoder.");
    return 0;
  }
  if (res < 0 && res != AVERROR_EOF) {
    logWarning(aio, res, "Skipping packet. avcodec_send_packet failed:");
    return 0;
  }
  return receive_and_process_frames(aio, 0, offset);
}

/**
 * Reads frames via <code>av_read_frame</code>, decodes them, and writes
 * the result to the Java-side <code>nativeBuffer</code>.
 *
 * @param aio   current FFAudioIO
 * @return  a negative number, if something went wrong
 */
int ff_fill_buffer(FFAudioIO *aio) {
  int res = 0;
  int total_bytes = 0;

  // Loop over packets until the buffer reaches FF_FILL_TARGET or EOF/error.
  // Non-audio packets (other streams) are skipped without counting toward the
  // target.
  while (total_bytes < FF_FILL_TARGET) {
    int read_res = av_read_frame(aio->format_context, aio->decode_packet);
    if (read_res >= 0) {
      if (aio->decode_packet->stream_index == aio->stream_index) {
        aio->timestamp += aio->decode_packet->duration;
#ifdef DEBUG
        fprintf(stderr, "duration  : %lli\n", aio->decode_packet->duration);
        fprintf(stderr, "timestamp : %" PRId64 "\n", aio->timestamp);
        fprintf(stderr, "pts       : %" PRId64 "\n", aio->decode_packet->pts);
        fprintf(stderr, "dts       : %" PRId64 "\n", aio->decode_packet->dts);
#endif
        int bytes = decode_packet(aio, (uint32_t)total_bytes);
        if (bytes < 0) {
          res = bytes;
          av_packet_unref(aio->decode_packet);
          goto bail;
        }
        total_bytes += bytes;
      }
      av_packet_unref(aio->decode_packet);
    } else {
#ifdef DEBUG
      fprintf(stderr, "Flushing decoder at EOF\n");
#endif
      // EOF or error — flush the decoder and drain remaining frames
      av_packet_unref(aio->decode_packet);
      if (read_res == AVERROR_EOF) {
        avcodec_send_packet(aio->decode_context, NULL);
        int bytes = receive_and_process_frames(aio, 1, (uint32_t)total_bytes);
        if (bytes > 0)
          total_bytes += bytes;
        else if (bytes < 0)
          res = bytes;
      } else {
        throwIOExceptionIfError(aio->env, read_res, "Error reading frame.");
        res = read_res;
      }
      break;
    }
  }

bail:
  return res;
}

/**
 * Free all resources held by aio and then itself.
 */
void ff_audioio_free(FFAudioIO *aio) {
#ifdef DEBUG
  fprintf(stderr, "ff_audioio_free\n");
#endif

  if (aio) {

    if (aio->encode_frame) {
      av_frame_free(&aio->encode_frame);
    }
    if (aio->decode_context) {
      avcodec_free_context(&aio->decode_context);
    }
    if (aio->encode_context) {
      avcodec_free_context(&aio->encode_context);
    }
    if (aio->swr_context) {
      swr_free(&aio->swr_context);
    }
    if (aio->format_context) {
      AVFormatContext *s = aio->format_context;
      if ((s->iformat && s->iformat->flags & AVFMT_NOFILE) ||
          (s->flags & AVFMT_FLAG_CUSTOM_IO)) {
        if (s->pb) {
          avio_flush(s->pb);
          avio_context_free(&s->pb);
        }
      }
      avformat_close_input(&(aio->format_context));
    }
    if (aio->decode_frame) {
      av_frame_free(&aio->decode_frame);
    }
    if (aio->decode_packet) {
      av_packet_free(&aio->decode_packet);
    }
    if (aio->encode_packet) {
      av_packet_free(&aio->encode_packet);
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

/**
 * Make sure FFmpeg is initialized all the way.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  avformat_network_init();
  return JNI_VERSION_1_6;
}

char *ff_jstring_to_utf8(JNIEnv *env, jstring java_string) {
  jclass string_class = NULL;
  jmethodID get_bytes_mid = NULL;
  jstring utf8_charset = NULL;
  jbyteArray bytes = NULL;
  jsize len = 0;
  char *result = NULL;

  if (!java_string)
    return NULL;

  string_class = (*env)->GetObjectClass(env, java_string);
  if (!string_class)
    goto bail;

  get_bytes_mid = (*env)->GetMethodID(env, string_class, "getBytes",
                                      "(Ljava/lang/String;)[B");
  if (!get_bytes_mid)
    goto bail;

  utf8_charset = (*env)->NewStringUTF(env, "UTF-8");
  if (!utf8_charset)
    goto bail;

  bytes = (jbyteArray)(*env)->CallObjectMethod(env, java_string, get_bytes_mid,
                                               utf8_charset);
  if (!bytes)
    goto bail;

  len = (*env)->GetArrayLength(env, bytes);
  result = (char *)malloc(len + 1);
  if (!result)
    goto bail;

  (*env)->GetByteArrayRegion(env, bytes, 0, len, (jbyte *)result);
  result[len] = '\0';

bail:
  if (utf8_charset)
    (*env)->DeleteLocalRef(env, utf8_charset);
  if (bytes)
    (*env)->DeleteLocalRef(env, bytes);

  return result;
}