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

import java.util.*;
import javax.sound.sampled.AudioFormat;

/**
 * FFSampledSP's {@link AudioFormat} adding a {@link #PROVIDER} property and a special constructor
 * to be called from {@link FFAudioFileFormat}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFAudioFormat extends AudioFormat {

  /**
   * Property key to mark where this {@link AudioFormat} came from.
   *
   * @see #FFSAMPLEDSP
   */
  public static final String PROVIDER = "provider";

  /**
   * Special property value for {@link #PROVIDER}, marking FFSampledSP audio formats. This allows us
   * to take some shortcuts.
   *
   * @see #PROVIDER
   * @see FFCodecInputStream
   */
  public static final String FFSAMPLEDSP = "ffsampledsp";

  /**
   * Creates a new {@code FFAudioFormat} from the given codec and format parameters.
   *
   * @param codecId FFmpeg {@code AVCodecID} value
   * @param sampleRate sample rate in Hz
   * @param sampleSize bits per sample, or {@link javax.sound.sampled.AudioSystem#NOT_SPECIFIED}
   * @param channels channel count, or {@link javax.sound.sampled.AudioSystem#NOT_SPECIFIED}
   * @param frameSize frame size in bytes, or {@link javax.sound.sampled.AudioSystem#NOT_SPECIFIED}
   * @param frameRate frame rate in frames per second, or {@link
   *     javax.sound.sampled.AudioSystem#NOT_SPECIFIED}
   * @param bigEndian {@code true} if samples are big-endian
   * @param bitRate bit rate in bits per second, or 0 if unknown
   * @param vbr {@code true} if variable bit rate, {@code false} if CBR, or {@code null} if unknown
   * @param encrypted {@code true} if the stream is DRM-protected
   */
  public FFAudioFormat(
      final int codecId,
      final float sampleRate,
      final int sampleSize,
      final int channels,
      final int frameSize,
      final float frameRate,
      final boolean bigEndian,
      final int bitRate,
      final Boolean vbr,
      final boolean encrypted) {
    super(
        FFEncoding.getInstance(codecId),
        sampleRate,
        sampleSize,
        channels,
        frameSize,
        frameRate,
        bigEndian,
        createProperties(bitRate, vbr, encrypted));
  }

  private static Map<String, Object> createProperties(
      final int bitRate, final Boolean vbr, final boolean encrypted) {
    final Map<String, Object> properties = new HashMap<String, Object>();
    if (bitRate > 0) properties.put("bitrate", bitRate);
    if (vbr != null) properties.put("vbr", vbr);
    if (encrypted) properties.put("encrypted", encrypted);
    properties.put(PROVIDER, FFSAMPLEDSP);
    return properties;
  }

  /** libavcodec encodings that are aware of their AVCodecID. */
  public static class FFEncoding extends Encoding {

    // PCM codecs (0x10000+)
    private static final int AV_CODEC_ID_PCM_S16LE = 0x10000;
    private static final int AV_CODEC_ID_PCM_S16BE = 0x10001;
    private static final int AV_CODEC_ID_PCM_U16LE = 0x10002;
    private static final int AV_CODEC_ID_PCM_U16BE = 0x10003;
    private static final int AV_CODEC_ID_PCM_S8 = 0x10004;
    private static final int AV_CODEC_ID_PCM_U8 = 0x10005;
    private static final int AV_CODEC_ID_PCM_MULAW = 0x10006;
    private static final int AV_CODEC_ID_PCM_ALAW = 0x10007;
    private static final int AV_CODEC_ID_PCM_S32LE = 0x10008;
    private static final int AV_CODEC_ID_PCM_S32BE = 0x10009;
    private static final int AV_CODEC_ID_PCM_U32LE = 0x1000a;
    private static final int AV_CODEC_ID_PCM_U32BE = 0x1000b;
    private static final int AV_CODEC_ID_PCM_S24LE = 0x1000c;
    private static final int AV_CODEC_ID_PCM_S24BE = 0x1000d;
    private static final int AV_CODEC_ID_PCM_U24LE = 0x1000e;
    private static final int AV_CODEC_ID_PCM_U24BE = 0x1000f;
    private static final int AV_CODEC_ID_PCM_S24DAUD = 0x10010;
    private static final int AV_CODEC_ID_PCM_ZORK = 0x10011;
    private static final int AV_CODEC_ID_PCM_S16LE_PLANAR = 0x10012;
    private static final int AV_CODEC_ID_PCM_DVD = 0x10013;
    private static final int AV_CODEC_ID_PCM_F32BE = 0x10014;
    private static final int AV_CODEC_ID_PCM_F32LE = 0x10015;
    private static final int AV_CODEC_ID_PCM_F64BE = 0x10016;
    private static final int AV_CODEC_ID_PCM_F64LE = 0x10017;
    private static final int AV_CODEC_ID_PCM_BLURAY = 0x10018;
    private static final int AV_CODEC_ID_PCM_LXF = 0x10019;
    private static final int AV_CODEC_ID_S302M = 0x1001a;
    private static final int AV_CODEC_ID_PCM_S8_PLANAR = 0x1001b;
    private static final int AV_CODEC_ID_PCM_S24LE_PLANAR = 0x1001c;
    private static final int AV_CODEC_ID_PCM_S32LE_PLANAR = 0x1001d;
    private static final int AV_CODEC_ID_PCM_S16BE_PLANAR = 0x1001e;
    private static final int AV_CODEC_ID_PCM_S64LE = 0x1001f;
    private static final int AV_CODEC_ID_PCM_S64BE = 0x10020;
    private static final int AV_CODEC_ID_PCM_F16LE = 0x10021;
    private static final int AV_CODEC_ID_PCM_F24LE = 0x10022;
    private static final int AV_CODEC_ID_PCM_VIDC = 0x10023;
    private static final int AV_CODEC_ID_PCM_SGA = 0x10024;

    // ADPCM codecs (0x11000+)
    private static final int AV_CODEC_ID_ADPCM_IMA_QT = 0x11000;
    private static final int AV_CODEC_ID_ADPCM_IMA_WAV = 0x11001;
    private static final int AV_CODEC_ID_ADPCM_IMA_DK3 = 0x11002;
    private static final int AV_CODEC_ID_ADPCM_IMA_DK4 = 0x11003;
    private static final int AV_CODEC_ID_ADPCM_IMA_WS = 0x11004;
    private static final int AV_CODEC_ID_ADPCM_IMA_SMJPEG = 0x11005;
    private static final int AV_CODEC_ID_ADPCM_MS = 0x11006;
    private static final int AV_CODEC_ID_ADPCM_4XM = 0x11007;
    private static final int AV_CODEC_ID_ADPCM_XA = 0x11008;
    private static final int AV_CODEC_ID_ADPCM_ADX = 0x11009;
    private static final int AV_CODEC_ID_ADPCM_EA = 0x1100a;
    private static final int AV_CODEC_ID_ADPCM_G726 = 0x1100b;
    private static final int AV_CODEC_ID_ADPCM_CT = 0x1100c;
    private static final int AV_CODEC_ID_ADPCM_SWF = 0x1100d;
    private static final int AV_CODEC_ID_ADPCM_YAMAHA = 0x1100e;
    private static final int AV_CODEC_ID_ADPCM_SBPRO_4 = 0x1100f;
    private static final int AV_CODEC_ID_ADPCM_SBPRO_3 = 0x11010;
    private static final int AV_CODEC_ID_ADPCM_SBPRO_2 = 0x11011;
    private static final int AV_CODEC_ID_ADPCM_THP = 0x11012;
    private static final int AV_CODEC_ID_ADPCM_IMA_AMV = 0x11013;
    private static final int AV_CODEC_ID_ADPCM_EA_R1 = 0x11014;
    private static final int AV_CODEC_ID_ADPCM_EA_R3 = 0x11015;
    private static final int AV_CODEC_ID_ADPCM_EA_R2 = 0x11016;
    private static final int AV_CODEC_ID_ADPCM_IMA_EA_SEAD = 0x11017;
    private static final int AV_CODEC_ID_ADPCM_IMA_EA_EACS = 0x11018;
    private static final int AV_CODEC_ID_ADPCM_EA_XAS = 0x11019;
    private static final int AV_CODEC_ID_ADPCM_EA_MAXIS_XA = 0x1101a;
    private static final int AV_CODEC_ID_ADPCM_IMA_ISS = 0x1101b;
    private static final int AV_CODEC_ID_ADPCM_G722 = 0x1101c;
    private static final int AV_CODEC_ID_ADPCM_IMA_APC = 0x1101d;
    private static final int AV_CODEC_ID_ADPCM_VIMA = 0x1101e;
    private static final int AV_CODEC_ID_ADPCM_AFC = 0x1101f;
    private static final int AV_CODEC_ID_ADPCM_IMA_OKI = 0x11020;
    private static final int AV_CODEC_ID_ADPCM_DTK = 0x11021;
    private static final int AV_CODEC_ID_ADPCM_IMA_RAD = 0x11022;
    private static final int AV_CODEC_ID_ADPCM_G726LE = 0x11023;
    private static final int AV_CODEC_ID_ADPCM_THP_LE = 0x11024;
    private static final int AV_CODEC_ID_ADPCM_PSX = 0x11025;
    private static final int AV_CODEC_ID_ADPCM_AICA = 0x11026;
    private static final int AV_CODEC_ID_ADPCM_IMA_DAT4 = 0x11027;
    private static final int AV_CODEC_ID_ADPCM_MTAF = 0x11028;
    private static final int AV_CODEC_ID_ADPCM_AGM = 0x11029;
    private static final int AV_CODEC_ID_ADPCM_ARGO = 0x1102a;
    private static final int AV_CODEC_ID_ADPCM_IMA_SSI = 0x1102b;
    private static final int AV_CODEC_ID_ADPCM_ZORK = 0x1102c;
    private static final int AV_CODEC_ID_ADPCM_IMA_APM = 0x1102d;
    private static final int AV_CODEC_ID_ADPCM_IMA_ALP = 0x1102e;
    private static final int AV_CODEC_ID_ADPCM_IMA_MTF = 0x1102f;
    private static final int AV_CODEC_ID_ADPCM_IMA_CUNNING = 0x11030;
    private static final int AV_CODEC_ID_ADPCM_IMA_MOFLEX = 0x11031;
    private static final int AV_CODEC_ID_ADPCM_IMA_ACORN = 0x11032;
    private static final int AV_CODEC_ID_ADPCM_XMD = 0x11033;
    private static final int AV_CODEC_ID_ADPCM_IMA_XBOX = 0x11034;
    private static final int AV_CODEC_ID_ADPCM_SANYO = 0x11035;
    private static final int AV_CODEC_ID_ADPCM_IMA_HVQM4 = 0x11036;
    private static final int AV_CODEC_ID_ADPCM_IMA_PDA = 0x11037;
    private static final int AV_CODEC_ID_ADPCM_N64 = 0x11038;
    private static final int AV_CODEC_ID_ADPCM_IMA_HVQM2 = 0x11039;
    private static final int AV_CODEC_ID_ADPCM_IMA_MAGIX = 0x1103a;
    private static final int AV_CODEC_ID_ADPCM_PSXC = 0x1103b;
    private static final int AV_CODEC_ID_ADPCM_CIRCUS = 0x1103c;
    private static final int AV_CODEC_ID_ADPCM_IMA_ESCAPE = 0x1103d;

    // AMR codecs (0x12000+)
    private static final int AV_CODEC_ID_AMR_NB = 0x12000;
    private static final int AV_CODEC_ID_AMR_WB = 0x12001;

    // RealAudio codecs (0x13000+)
    private static final int AV_CODEC_ID_RA_144 = 0x13000;
    private static final int AV_CODEC_ID_RA_288 = 0x13001;

    // DPCM codecs (0x14000+)
    private static final int AV_CODEC_ID_ROQ_DPCM = 0x14000;
    private static final int AV_CODEC_ID_INTERPLAY_DPCM = 0x14001;
    private static final int AV_CODEC_ID_XAN_DPCM = 0x14002;
    private static final int AV_CODEC_ID_SOL_DPCM = 0x14003;
    private static final int AV_CODEC_ID_SDX2_DPCM = 0x14004;
    private static final int AV_CODEC_ID_GREMLIN_DPCM = 0x14005;
    private static final int AV_CODEC_ID_DERF_DPCM = 0x14006;
    private static final int AV_CODEC_ID_WADY_DPCM = 0x14007;
    private static final int AV_CODEC_ID_CBD2_DPCM = 0x14008;

    // Audio codecs (0x15000+)
    private static final int AV_CODEC_ID_MP2 = 0x15000;
    private static final int AV_CODEC_ID_MP3 = 0x15001;
    private static final int AV_CODEC_ID_AAC = 0x15002;
    private static final int AV_CODEC_ID_AC3 = 0x15003;
    private static final int AV_CODEC_ID_DTS = 0x15004;
    private static final int AV_CODEC_ID_VORBIS = 0x15005;
    private static final int AV_CODEC_ID_DVAUDIO = 0x15006;
    private static final int AV_CODEC_ID_WMAV1 = 0x15007;
    private static final int AV_CODEC_ID_WMAV2 = 0x15008;
    private static final int AV_CODEC_ID_MACE3 = 0x15009;
    private static final int AV_CODEC_ID_MACE6 = 0x1500a;
    private static final int AV_CODEC_ID_VMDAUDIO = 0x1500b;
    private static final int AV_CODEC_ID_FLAC = 0x1500c;
    private static final int AV_CODEC_ID_MP3ADU = 0x1500d;
    private static final int AV_CODEC_ID_MP3ON4 = 0x1500e;
    private static final int AV_CODEC_ID_SHORTEN = 0x1500f;
    private static final int AV_CODEC_ID_ALAC = 0x15010;
    private static final int AV_CODEC_ID_WESTWOOD_SND1 = 0x15011;
    private static final int AV_CODEC_ID_GSM = 0x15012;
    private static final int AV_CODEC_ID_QDM2 = 0x15013;
    private static final int AV_CODEC_ID_COOK = 0x15014;
    private static final int AV_CODEC_ID_TRUESPEECH = 0x15015;
    private static final int AV_CODEC_ID_TTA = 0x15016;
    private static final int AV_CODEC_ID_SMACKAUDIO = 0x15017;
    private static final int AV_CODEC_ID_QCELP = 0x15018;
    private static final int AV_CODEC_ID_WAVPACK = 0x15019;
    private static final int AV_CODEC_ID_DSICINAUDIO = 0x1501a;
    private static final int AV_CODEC_ID_IMC = 0x1501b;
    private static final int AV_CODEC_ID_MUSEPACK7 = 0x1501c;
    private static final int AV_CODEC_ID_MLP = 0x1501d;
    private static final int AV_CODEC_ID_GSM_MS = 0x1501e;
    private static final int AV_CODEC_ID_ATRAC3 = 0x1501f;
    private static final int AV_CODEC_ID_APE = 0x15020;
    private static final int AV_CODEC_ID_NELLYMOSER = 0x15021;
    private static final int AV_CODEC_ID_MUSEPACK8 = 0x15022;
    private static final int AV_CODEC_ID_SPEEX = 0x15023;
    private static final int AV_CODEC_ID_WMAVOICE = 0x15024;
    private static final int AV_CODEC_ID_WMAPRO = 0x15025;
    private static final int AV_CODEC_ID_WMALOSSLESS = 0x15026;
    private static final int AV_CODEC_ID_ATRAC3P = 0x15027;
    private static final int AV_CODEC_ID_EAC3 = 0x15028;
    private static final int AV_CODEC_ID_SIPR = 0x15029;
    private static final int AV_CODEC_ID_MP1 = 0x1502a;
    private static final int AV_CODEC_ID_TWINVQ = 0x1502b;
    private static final int AV_CODEC_ID_TRUEHD = 0x1502c;
    private static final int AV_CODEC_ID_MP4ALS = 0x1502d;
    private static final int AV_CODEC_ID_ATRAC1 = 0x1502e;
    private static final int AV_CODEC_ID_BINKAUDIO_RDFT = 0x1502f;
    private static final int AV_CODEC_ID_BINKAUDIO_DCT = 0x15030;
    private static final int AV_CODEC_ID_AAC_LATM = 0x15031;
    private static final int AV_CODEC_ID_QDMC = 0x15032;
    private static final int AV_CODEC_ID_CELT = 0x15033;
    private static final int AV_CODEC_ID_G723_1 = 0x15034;
    private static final int AV_CODEC_ID_G729 = 0x15035;
    private static final int AV_CODEC_ID_8SVX_EXP = 0x15036;
    private static final int AV_CODEC_ID_8SVX_FIB = 0x15037;
    private static final int AV_CODEC_ID_BMV_AUDIO = 0x15038;
    private static final int AV_CODEC_ID_RALF = 0x15039;
    private static final int AV_CODEC_ID_IAC = 0x1503a;
    private static final int AV_CODEC_ID_ILBC = 0x1503b;
    private static final int AV_CODEC_ID_OPUS = 0x1503c;
    private static final int AV_CODEC_ID_COMFORT_NOISE = 0x1503d;
    private static final int AV_CODEC_ID_TAK = 0x1503e;
    private static final int AV_CODEC_ID_METASOUND = 0x1503f;
    private static final int AV_CODEC_ID_PAF_AUDIO = 0x15040;
    private static final int AV_CODEC_ID_ON2AVC = 0x15041;
    private static final int AV_CODEC_ID_DSS_SP = 0x15042;
    private static final int AV_CODEC_ID_CODEC2 = 0x15043;
    private static final int AV_CODEC_ID_FFWAVESYNTH = 0x15044;
    private static final int AV_CODEC_ID_SONIC = 0x15045;
    private static final int AV_CODEC_ID_SONIC_LS = 0x15046;
    private static final int AV_CODEC_ID_EVRC = 0x15047;
    private static final int AV_CODEC_ID_SMV = 0x15048;
    private static final int AV_CODEC_ID_DSD_LSBF = 0x15049;
    private static final int AV_CODEC_ID_DSD_MSBF = 0x1504a;
    private static final int AV_CODEC_ID_DSD_LSBF_PLANAR = 0x1504b;
    private static final int AV_CODEC_ID_DSD_MSBF_PLANAR = 0x1504c;
    private static final int AV_CODEC_ID_4GV = 0x1504d;
    private static final int AV_CODEC_ID_INTERPLAY_ACM = 0x1504e;
    private static final int AV_CODEC_ID_XMA1 = 0x1504f;
    private static final int AV_CODEC_ID_XMA2 = 0x15050;
    private static final int AV_CODEC_ID_DST = 0x15051;
    private static final int AV_CODEC_ID_ATRAC3AL = 0x15052;
    private static final int AV_CODEC_ID_ATRAC3PAL = 0x15053;
    private static final int AV_CODEC_ID_DOLBY_E = 0x15054;
    private static final int AV_CODEC_ID_APTX = 0x15055;
    private static final int AV_CODEC_ID_APTX_HD = 0x15056;
    private static final int AV_CODEC_ID_SBC = 0x15057;
    private static final int AV_CODEC_ID_ATRAC9 = 0x15058;
    private static final int AV_CODEC_ID_HCOM = 0x15059;
    private static final int AV_CODEC_ID_ACELP_KELVIN = 0x1505a;
    private static final int AV_CODEC_ID_MPEGH_3D_AUDIO = 0x1505b;
    private static final int AV_CODEC_ID_SIREN = 0x1505c;
    private static final int AV_CODEC_ID_HCA = 0x1505d;
    private static final int AV_CODEC_ID_FASTAUDIO = 0x1505e;
    private static final int AV_CODEC_ID_MSNSIREN = 0x1505f;
    private static final int AV_CODEC_ID_DFPWM = 0x15060;
    private static final int AV_CODEC_ID_BONK = 0x15061;
    private static final int AV_CODEC_ID_MISC4 = 0x15062;
    private static final int AV_CODEC_ID_APAC = 0x15063;
    private static final int AV_CODEC_ID_FTR = 0x15064;
    private static final int AV_CODEC_ID_WAVARC = 0x15065;
    private static final int AV_CODEC_ID_RKA = 0x15066;
    private static final int AV_CODEC_ID_AC4 = 0x15067;
    private static final int AV_CODEC_ID_OSQ = 0x15068;
    private static final int AV_CODEC_ID_QOA = 0x15069;
    private static final int AV_CODEC_ID_LC3 = 0x1506a;
    private static final int AV_CODEC_ID_G728 = 0x1506b;
    private static final int AV_CODEC_ID_AHX = 0x1506c;

    /** Codecs supported by libavcodec. */
    public enum Codec {
      /** MPEG-1 Layer 1 audio. */
      MP1("MPEG-1, Layer 1", AV_CODEC_ID_MP1),
      /** MPEG-1 Layer 2 audio. */
      MP2("MPEG-1, Layer 2", AV_CODEC_ID_MP2),
      /** MPEG-1 Layer 3 audio (MP3). */
      MP3("MPEG-1, Layer 3", AV_CODEC_ID_MP3),

      /** Apple Lossless Audio Codec (ALAC). */
      APPLE_LOSSLESS("Apple Lossless", AV_CODEC_ID_ALAC),

      /** MPEG-4 Advanced Audio Coding (AAC). */
      MPEG4_AAC("MPEG4 AAC", AV_CODEC_ID_AAC),
      /** MPEG-4 AAC with LATM/LOAS framing. */
      MPEG4_AAC_LATM("MPEG4 AAC-LATM", AV_CODEC_ID_AAC_LATM),

      /** MPEG-4 QCELP (Qualcomm Code Excited Linear Prediction). */
      MPEG4_QCELP("QCELP", AV_CODEC_ID_QCELP),
      /** MPEG-4 TwinVQ. */
      MPEG4_TWINVQ("MPEG4 TwinVQ", AV_CODEC_ID_TWINVQ),
      /** MPEG-4 Audio Lossless Coding (ALS). */
      MPEG4_ALS("MPEG4 Audio Lossless Coding", AV_CODEC_ID_MP4ALS),

      /** ITU-T G.711 mu-law (ULAW) PCM. */
      ULAW(Encoding.ULAW.toString(), AV_CODEC_ID_PCM_MULAW),
      /** ITU-T G.711 A-law (ALAW) PCM. */
      ALAW(Encoding.ALAW.toString(), AV_CODEC_ID_PCM_ALAW),

      /** Generic signed PCM (format determined by sample size and endianness). */
      PCM_SIGNED(Encoding.PCM_SIGNED.toString(), -1, true),
      /** Generic unsigned PCM (format determined by sample size and endianness). */
      PCM_UNSIGNED(Encoding.PCM_UNSIGNED.toString(), -1, true),
      /** Generic floating-point PCM (format determined by sample size and endianness). */
      PCM_FLOAT(Encoding.PCM_FLOAT.toString(), -1, true),

      /** Signed 8-bit PCM. */
      PCM_S8(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S8, true),
      /** Signed 16-bit big-endian PCM. */
      PCM_S16BE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S16BE, true),
      /** Signed 16-bit little-endian PCM. */
      PCM_S16LE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S16LE, true),
      /** Signed 24-bit big-endian PCM. */
      PCM_S24BE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S24BE, true),
      /** Signed 24-bit little-endian PCM. */
      PCM_S24LE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S24LE, true),
      /** Signed 32-bit big-endian PCM. */
      PCM_S32BE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S32BE, true),
      /** Signed 32-bit little-endian PCM. */
      PCM_S32LE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S32LE, true),
      /** Signed 64-bit big-endian PCM. */
      PCM_S64BE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S64BE, true),
      /** Signed 64-bit little-endian PCM. */
      PCM_S64LE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S64LE, true),

      /** Unsigned 8-bit PCM. */
      PCM_U8(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U8, true),
      /** Unsigned 16-bit big-endian PCM. */
      PCM_U16BE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U16BE, true),
      /** Unsigned 16-bit little-endian PCM. */
      PCM_U16LE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U16LE, true),
      /** Unsigned 24-bit big-endian PCM. */
      PCM_U24BE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U24BE, true),
      /** Unsigned 24-bit little-endian PCM. */
      PCM_U24LE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U24LE, true),
      /** Unsigned 32-bit big-endian PCM. */
      PCM_U32BE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U32BE, true),
      /** Unsigned 32-bit little-endian PCM. */
      PCM_U32LE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U32LE, true),

      /** 16-bit floating-point little-endian PCM. */
      PCM_F16LE(Encoding.PCM_FLOAT.toString(), AV_CODEC_ID_PCM_F16LE, true),
      /** 24-bit floating-point little-endian PCM. */
      PCM_F24LE(Encoding.PCM_FLOAT.toString(), AV_CODEC_ID_PCM_F24LE, true),
      /** 32-bit floating-point big-endian PCM. */
      PCM_F32BE(Encoding.PCM_FLOAT.toString(), AV_CODEC_ID_PCM_F32BE, true),
      /** 32-bit floating-point little-endian PCM. */
      PCM_F32LE(Encoding.PCM_FLOAT.toString(), AV_CODEC_ID_PCM_F32LE, true),
      /** 64-bit floating-point big-endian PCM. */
      PCM_F64BE(Encoding.PCM_FLOAT.toString(), AV_CODEC_ID_PCM_F64BE, true),
      /** 64-bit floating-point little-endian PCM. */
      PCM_F64LE(Encoding.PCM_FLOAT.toString(), AV_CODEC_ID_PCM_F64LE, true),

      /** Signed 8-bit planar PCM. */
      PCM_S8_PLANAR("PCM S8 Planar", AV_CODEC_ID_PCM_S8_PLANAR, true),
      /** Signed 16-bit little-endian planar PCM. */
      PCM_S16LE_PLANAR("PCM S16LE Planar", AV_CODEC_ID_PCM_S16LE_PLANAR, true),
      /** Signed 16-bit big-endian planar PCM. */
      PCM_S16BE_PLANAR("PCM S16BE Planar", AV_CODEC_ID_PCM_S16BE_PLANAR, true),
      /** Signed 24-bit little-endian planar PCM. */
      PCM_S24LE_PLANAR("PCM S24LE Planar", AV_CODEC_ID_PCM_S24LE_PLANAR, true),
      /** Signed 32-bit little-endian planar PCM. */
      PCM_S32LE_PLANAR("PCM S32LE Planar", AV_CODEC_ID_PCM_S32LE_PLANAR, true),

      /** Signed 24-bit PCM in DAUD format. */
      PCM_S24DAUD("PCM S24 DAUD", AV_CODEC_ID_PCM_S24DAUD, true),
      /** Zork PCM. */
      PCM_ZORK("Zork", AV_CODEC_ID_PCM_ZORK, true),
      /** PCM as used on DVD. */
      PCM_DVD("PCM DVD", AV_CODEC_ID_PCM_DVD, true),
      /** PCM as used on Blu-ray. */
      PCM_BLURAY("PCM BLURAY", AV_CODEC_ID_PCM_BLURAY, true),
      /** PCM as used in LXF files. */
      PCM_LXF("PCM LXF", AV_CODEC_ID_PCM_LXF, true),
      /** VIDC PCM. */
      PCM_VIDC("PCM VIDC", AV_CODEC_ID_PCM_VIDC, true),
      /** SGA PCM. */
      PCM_SGA("PCM SGA", AV_CODEC_ID_PCM_SGA, true),

      /** Internet Low Bitrate Codec (iLBC). */
      I_LBC("iLBC", AV_CODEC_ID_ILBC),
      /** Microsoft GSM 6.10. */
      MICROSOFT_GSM("Microsoft GSM", AV_CODEC_ID_GSM_MS),
      /** Adaptive Multi-Rate Wideband (AMR-WB). */
      AMR_WB("AMR WB", AV_CODEC_ID_AMR_WB),
      /** Adaptive Multi-Rate Narrowband (AMR-NB). */
      AMR_NB("AMR NB", AV_CODEC_ID_AMR_NB),
      /** Dolby AC-3 (Audio Codec 3). */
      AC3("AC3", AV_CODEC_ID_AC3),
      /** Ogg Vorbis. */
      VORBIS("VORBIS", AV_CODEC_ID_VORBIS),
      /** Free Lossless Audio Codec (FLAC). */
      FLAC("FLAC", AV_CODEC_ID_FLAC),
      /** Digital Theater Systems (DTS). */
      DTS("DTS", AV_CODEC_ID_DTS),

      /** Windows Media Audio v1. */
      WMA_V1("WMA 1", AV_CODEC_ID_WMAV1),
      /** Windows Media Audio v2. */
      WMA_V2("WMA 2", AV_CODEC_ID_WMAV2),
      /** Windows Media Audio Lossless. */
      WMA_LOSSLESS("WMA Lossless", AV_CODEC_ID_WMALOSSLESS),
      /** Windows Media Audio Professional. */
      WMA_PRO("WMA Pro", AV_CODEC_ID_WMAPRO),
      /** Windows Media Audio Voice. */
      WMA_VOICE("WMA Voice", AV_CODEC_ID_WMAVOICE),

      /** Sony ATRAC1. */
      ATRAC_1("ATRAC 1", AV_CODEC_ID_ATRAC1),
      /** Sony ATRAC3. */
      ATRAC_3("ATRAC 3", AV_CODEC_ID_ATRAC3),
      /** Sony ATRAC3plus. */
      ATRAC_3P("ATRAC 3plus", AV_CODEC_ID_ATRAC3P),
      /** Sony ATRAC3 Advanced Lossless. */
      ATRAC_3AL("ATRAC 3 Advanced Lossless", AV_CODEC_ID_ATRAC3AL),
      /** Sony ATRAC3plus Advanced Lossless. */
      ATRAC_3PAL("ATRAC 3plus Advanced Lossless", AV_CODEC_ID_ATRAC3PAL),
      /** Sony ATRAC9. */
      ATRAC_9("ATRAC9", AV_CODEC_ID_ATRAC9),

      /** Global System for Mobile (GSM) full-rate codec. */
      GSM("GSM", AV_CODEC_ID_GSM),
      /** Dolby TrueHD lossless audio. */
      TRUE_HD("TrueHD", AV_CODEC_ID_TRUEHD),
      /** Microsoft TrueSpeech. */
      TRUESPEECH("Truespeech", AV_CODEC_ID_TRUESPEECH),

      /** Nellymoser Asao codec. */
      NELLYMOSER("Nellymoser", AV_CODEC_ID_NELLYMOSER),
      /** Speex speech codec. */
      SPEEX("Speex", AV_CODEC_ID_SPEEX),
      /** Opus interactive audio codec. */
      OPUS("OPUS", AV_CODEC_ID_OPUS),

      /** SMPTE 302M audio. */
      S302M("S302M", AV_CODEC_ID_S302M),

      /** ADPCM for Shockwave Flash (SWF). */
      ADPCM_SWF("ADPCM SWF", AV_CODEC_ID_ADPCM_SWF),
      /** ADPCM IMA QuickTime. */
      ADPCM_IMA_QT("ADPCM IMA QT", AV_CODEC_ID_ADPCM_IMA_QT),
      /** ADPCM IMA WAV. */
      ADPCM_IMA_WAV("ADPCM IMA WAV", AV_CODEC_ID_ADPCM_IMA_WAV),
      /** ADPCM IMA Duck DK3. */
      ADPCM_IMA_DK3("ADPCM IMA DK3", AV_CODEC_ID_ADPCM_IMA_DK3),
      /** ADPCM IMA Duck DK4. */
      ADPCM_IMA_DK4("ADPCM IMA DK4", AV_CODEC_ID_ADPCM_IMA_DK4),
      /** ADPCM IMA Westwood Studios. */
      ADPCM_IMA_WS("ADPCM IMA WS", AV_CODEC_ID_ADPCM_IMA_WS),
      /** ADPCM IMA SMJPEG. */
      ADPCM_IMA_SMJPEG("ADPCM IMA SMJPEG", AV_CODEC_ID_ADPCM_IMA_SMJPEG),
      /** ADPCM Microsoft. */
      ADPCM_MS("ADPCM MS", AV_CODEC_ID_ADPCM_MS),
      /** ADPCM 4X Movie. */
      ADPCM_4XM("ADPCM 4XM", AV_CODEC_ID_ADPCM_4XM),
      /** ADPCM XA (PlayStation CD-ROM). */
      ADPCM_XA("ADPCM XA", AV_CODEC_ID_ADPCM_XA),
      /** ADPCM ADX (CRI Middleware). */
      ADPCM_ADX("ADPCM ADX", AV_CODEC_ID_ADPCM_ADX),
      /** ADPCM Electronic Arts. */
      ADPCM_EA("ADPCM EA", AV_CODEC_ID_ADPCM_EA),
      /** ADPCM ITU-T G.726 big-endian. */
      ADPCM_G726("ADPCM G726", AV_CODEC_ID_ADPCM_G726),
      /** ADPCM ITU-T G.726 little-endian. */
      ADPCM_G726LE("ADPCM G726LE", AV_CODEC_ID_ADPCM_G726LE),
      /** ADPCM Creative Technology. */
      ADPCM_CT("ADPCM CT", AV_CODEC_ID_ADPCM_CT),
      /** ADPCM Yamaha. */
      ADPCM_YAMAHA("ADPCM YAMAHA", AV_CODEC_ID_ADPCM_YAMAHA),
      /** ADPCM Sound Blaster Pro 4-bit. */
      ADPCM_SBPRO_4("ADPCM SBPRO_4", AV_CODEC_ID_ADPCM_SBPRO_4),
      /** ADPCM Sound Blaster Pro 3-bit. */
      ADPCM_SBPRO_3("ADPCM SBPRO_3", AV_CODEC_ID_ADPCM_SBPRO_3),
      /** ADPCM Sound Blaster Pro 2-bit. */
      ADPCM_SBPRO_2("ADPCM SBPRO_2", AV_CODEC_ID_ADPCM_SBPRO_2),
      /** ADPCM Nintendo THP. */
      ADPCM_THP("ADPCM THP", AV_CODEC_ID_ADPCM_THP),
      /** ADPCM Nintendo THP little-endian. */
      ADPCM_THP_LE("ADPCM THP LE", AV_CODEC_ID_ADPCM_THP_LE),
      /** ADPCM IMA AMV. */
      ADPCM_IMA_AMV("ADPCM IMA AMV", AV_CODEC_ID_ADPCM_IMA_AMV),
      /** ADPCM Electronic Arts R1. */
      ADPCM_EA_R1("ADPCM EA R1", AV_CODEC_ID_ADPCM_EA_R1),
      /** ADPCM Electronic Arts R3. */
      ADPCM_EA_R3("ADPCM EA R3", AV_CODEC_ID_ADPCM_EA_R3),
      /** ADPCM Electronic Arts R2. */
      ADPCM_EA_R2("ADPCM EA R2", AV_CODEC_ID_ADPCM_EA_R2),
      /** ADPCM IMA Electronic Arts SEAD. */
      ADPCM_IMA_EA_SEAD("ADPCM IMA EA SEAD", AV_CODEC_ID_ADPCM_IMA_EA_SEAD),
      /** ADPCM IMA Electronic Arts EACS. */
      ADPCM_IMA_EA_EACS("ADPCM IMA EA EACS", AV_CODEC_ID_ADPCM_IMA_EA_EACS),
      /** ADPCM Electronic Arts XAS. */
      ADPCM_EA_XAS("ADPCM EA XAS", AV_CODEC_ID_ADPCM_EA_XAS),
      /** ADPCM Electronic Arts Maxis XA. */
      ADPCM_EA_MAXIS_XA("ADPCM EA MAXIS XA", AV_CODEC_ID_ADPCM_EA_MAXIS_XA),
      /** ADPCM IMA ISS. */
      ADPCM_IMA_ISS("ADPCM IMA ISS", AV_CODEC_ID_ADPCM_IMA_ISS),
      /** ADPCM ITU-T G.722. */
      ADPCM_G722("ADPCM G722", AV_CODEC_ID_ADPCM_G722),
      /** ADPCM IMA APC. */
      ADPCM_IMA_APC("ADPCM IMA APC", AV_CODEC_ID_ADPCM_IMA_APC),
      /** ADPCM VIMA. */
      ADPCM_VIMA("ADPCM VIMA", AV_CODEC_ID_ADPCM_VIMA),
      /** ADPCM Nintendo AFC. */
      ADPCM_AFC("ADPCM AFC", AV_CODEC_ID_ADPCM_AFC),
      /** ADPCM IMA OKI. */
      ADPCM_IMA_OKI("ADPCM IMA OKI", AV_CODEC_ID_ADPCM_IMA_OKI),
      /** ADPCM DTK (Nintendo GameCube). */
      ADPCM_DTK("ADPCM DTK", AV_CODEC_ID_ADPCM_DTK),
      /** ADPCM IMA RAD. */
      ADPCM_IMA_RAD("ADPCM IMA RAD", AV_CODEC_ID_ADPCM_IMA_RAD),
      /** ADPCM PSX (PlayStation). */
      ADPCM_PSX("ADPCM PSX", AV_CODEC_ID_ADPCM_PSX),
      /** ADPCM AICA (Sega Dreamcast). */
      ADPCM_AICA("ADPCM AICA", AV_CODEC_ID_ADPCM_AICA),
      /** ADPCM IMA DAT4. */
      ADPCM_IMA_DAT4("ADPCM IMA DAT4", AV_CODEC_ID_ADPCM_IMA_DAT4),
      /** ADPCM MTAF. */
      ADPCM_MTAF("ADPCM MTAF", AV_CODEC_ID_ADPCM_MTAF),
      /** ADPCM AGM. */
      ADPCM_AGM("ADPCM AGM", AV_CODEC_ID_ADPCM_AGM),
      /** ADPCM Argonaut Games. */
      ADPCM_ARGO("ADPCM ARGO", AV_CODEC_ID_ADPCM_ARGO),
      /** ADPCM IMA SSI. */
      ADPCM_IMA_SSI("ADPCM IMA SSI", AV_CODEC_ID_ADPCM_IMA_SSI),
      /** ADPCM Zork. */
      ADPCM_ZORK("ADPCM ZORK", AV_CODEC_ID_ADPCM_ZORK),
      /** ADPCM IMA APM. */
      ADPCM_IMA_APM("ADPCM IMA APM", AV_CODEC_ID_ADPCM_IMA_APM),
      /** ADPCM IMA ALP. */
      ADPCM_IMA_ALP("ADPCM IMA ALP", AV_CODEC_ID_ADPCM_IMA_ALP),
      /** ADPCM IMA MTF. */
      ADPCM_IMA_MTF("ADPCM IMA MTF", AV_CODEC_ID_ADPCM_IMA_MTF),
      /** ADPCM IMA Cunning Developments. */
      ADPCM_IMA_CUNNING("ADPCM IMA CUNNING", AV_CODEC_ID_ADPCM_IMA_CUNNING),
      /** ADPCM IMA MoFlex. */
      ADPCM_IMA_MOFLEX("ADPCM IMA MOFLEX", AV_CODEC_ID_ADPCM_IMA_MOFLEX),
      /** ADPCM IMA Acorn Replay. */
      ADPCM_IMA_ACORN("ADPCM IMA ACORN", AV_CODEC_ID_ADPCM_IMA_ACORN),
      /** ADPCM XMD. */
      ADPCM_XMD("ADPCM XMD", AV_CODEC_ID_ADPCM_XMD),
      /** ADPCM IMA Xbox. */
      ADPCM_IMA_XBOX("ADPCM IMA XBOX", AV_CODEC_ID_ADPCM_IMA_XBOX),
      /** ADPCM Sanyo. */
      ADPCM_SANYO("ADPCM SANYO", AV_CODEC_ID_ADPCM_SANYO),
      /** ADPCM IMA HVQM4. */
      ADPCM_IMA_HVQM4("ADPCM IMA HVQM4", AV_CODEC_ID_ADPCM_IMA_HVQM4),
      /** ADPCM IMA PDA. */
      ADPCM_IMA_PDA("ADPCM IMA PDA", AV_CODEC_ID_ADPCM_IMA_PDA),
      /** ADPCM Nintendo 64. */
      ADPCM_N64("ADPCM N64", AV_CODEC_ID_ADPCM_N64),
      /** ADPCM IMA HVQM2. */
      ADPCM_IMA_HVQM2("ADPCM IMA HVQM2", AV_CODEC_ID_ADPCM_IMA_HVQM2),
      /** ADPCM IMA Magix. */
      ADPCM_IMA_MAGIX("ADPCM IMA MAGIX", AV_CODEC_ID_ADPCM_IMA_MAGIX),
      /** ADPCM PSXC. */
      ADPCM_PSXC("ADPCM PSXC", AV_CODEC_ID_ADPCM_PSXC),
      /** ADPCM Circus. */
      ADPCM_CIRCUS("ADPCM CIRCUS", AV_CODEC_ID_ADPCM_CIRCUS),
      /** ADPCM IMA Escape. */
      ADPCM_IMA_ESCAPE("ADPCM IMA ESCAPE", AV_CODEC_ID_ADPCM_IMA_ESCAPE),

      /** RealAudio 1.0 (144 bits/frame). */
      RA_144("RA 144", AV_CODEC_ID_RA_144),
      /** RealAudio 2.0 (288 bits/frame). */
      RA_288("RA_288", AV_CODEC_ID_RA_288),
      /** RoQ DPCM. */
      ROQ_DPCM("ROQ DPCM", AV_CODEC_ID_ROQ_DPCM),
      /** Interplay DPCM. */
      INTERPLAY_DPCM("INTERPLAY DPCM", AV_CODEC_ID_INTERPLAY_DPCM),
      /** Xan DPCM. */
      XAN_DPCM("XAN DPCM", AV_CODEC_ID_XAN_DPCM),
      /** Sol DPCM. */
      SOL_DPCM("SOL DPCM", AV_CODEC_ID_SOL_DPCM),
      /** SDX2 DPCM. */
      SDX2_DPCM("SDX2 DPCM", AV_CODEC_ID_SDX2_DPCM),
      /** Gremlin DPCM. */
      GREMLIN_DPCM("GREMLIN DPCM", AV_CODEC_ID_GREMLIN_DPCM),
      /** DERF DPCM. */
      DERF_DPCM("DERF DPCM", AV_CODEC_ID_DERF_DPCM),
      /** WADY DPCM. */
      WADY_DPCM("WADY DPCM", AV_CODEC_ID_WADY_DPCM),
      /** CBD2 DPCM. */
      CBD2_DPCM("CBD2 DPCM", AV_CODEC_ID_CBD2_DPCM),

      /** DV audio. */
      DVAUDIO("DVAUDIO", AV_CODEC_ID_DVAUDIO),
      /** Apple MACE 3:1 audio compression. */
      MACE3("MACE3", AV_CODEC_ID_MACE3),
      /** Apple MACE 6:1 audio compression. */
      MACE6("MACE6", AV_CODEC_ID_MACE6),
      /** Sierra VMD audio. */
      VMDAUDIO("VMDAUDIO", AV_CODEC_ID_VMDAUDIO),
      /** MP3 ADU (Application Data Unit) variant. */
      MP3ADU("MP3ADU", AV_CODEC_ID_MP3ADU),
      /** MP3 on MP4 (mp3on4). */
      MP3ON4("MP3ON4", AV_CODEC_ID_MP3ON4),
      /** Shorten lossless audio. */
      SHORTEN("SHORTEN", AV_CODEC_ID_SHORTEN),
      /** Westwood Studios SND1 audio. */
      WESTWOOD_SND1("WESTWOOD SND1", AV_CODEC_ID_WESTWOOD_SND1),
      /** QDesign Music Codec 2. */
      QDM2("QDM2", AV_CODEC_ID_QDM2),
      /** RealAudio COOK. */
      COOK("COOK", AV_CODEC_ID_COOK),
      /** True Audio (TTA) lossless codec. */
      TTA("TTA", AV_CODEC_ID_TTA),
      /** Smacker audio. */
      SMACKAUDIO("SMACKAUDIO", AV_CODEC_ID_SMACKAUDIO),
      /** WavPack lossless/hybrid audio. */
      WAVPACK("WAVPACK", AV_CODEC_ID_WAVPACK),
      /** Delphine Software International CIN audio. */
      DSICINAUDIO("DSICINAUDIO", AV_CODEC_ID_DSICINAUDIO),
      /** IMC (Intel Music Coder). */
      IMC("IMC", AV_CODEC_ID_IMC),
      /** Musepack SV7. */
      MUSEPACK7("MUSEPACK7", AV_CODEC_ID_MUSEPACK7),
      /** Meridian Lossless Packing (MLP). */
      MLP("MLP", AV_CODEC_ID_MLP),
      /** Monkey's Audio (APE) lossless codec. */
      APE("APE", AV_CODEC_ID_APE),
      /** Musepack SV8. */
      MUSEPACK8("MUSEPACK8", AV_CODEC_ID_MUSEPACK8),
      /** Dolby Digital Plus (E-AC-3). */
      EAC3("EAC3", AV_CODEC_ID_EAC3),
      /** RealAudio SIPR (Sipro Lab Telecom). */
      SIPR("SIPR", AV_CODEC_ID_SIPR),
      /** Bink Audio RDFT. */
      BINKAUDIO_RDFT("BINKAUDIO RDFT", AV_CODEC_ID_BINKAUDIO_RDFT),
      /** Bink Audio DCT. */
      BINKAUDIO_DCT("BINKAUDIO DCT", AV_CODEC_ID_BINKAUDIO_DCT),
      /** QDesign Music Codec 1. */
      QDMC("QDMC", AV_CODEC_ID_QDMC),
      /** Constrained Energy Lapped Transform (CELT). */
      CELT("CELT", AV_CODEC_ID_CELT),
      /** ITU-T G.723.1 audio codec. */
      G723_1("G723 1", AV_CODEC_ID_G723_1),
      /** ITU-T G.729 audio codec. */
      G729("G729", AV_CODEC_ID_G729),
      /** ITU-T G.728 audio codec. */
      G728("G728", AV_CODEC_ID_G728),
      /** 8SVX exponential codec. */
      _8SVX_EXP("8SVX EXP", AV_CODEC_ID_8SVX_EXP),
      /** 8SVX Fibonacci codec. */
      _8SVX_FIB("8SVX FIB", AV_CODEC_ID_8SVX_FIB),
      /** BMV audio. */
      BMV_AUDIO("BMV_AUDIO", AV_CODEC_ID_BMV_AUDIO),
      /** RealAudio Lossless (RALF). */
      RALF("RALF", AV_CODEC_ID_RALF),
      /** Indeo Audio Coder (IAC). */
      IAC("IAC", AV_CODEC_ID_IAC),
      /** Comfort noise (RFC 3389). */
      COMFORT_NOISE("COMFORT NOISE", AV_CODEC_ID_COMFORT_NOISE),
      /** FFmpeg WaveSynth. */
      FFWAVESYNTH("FFWAVESYNTH", AV_CODEC_ID_FFWAVESYNTH),
      /** Sonic lossless audio codec. */
      SONIC("SONIC", AV_CODEC_ID_SONIC),
      /** Sonic lossless audio codec (lossless mode). */
      SONIC_LS("SONIC_LS", AV_CODEC_ID_SONIC_LS),
      /** PAF audio. */
      PAF_AUDIO("PAF_AUDIO", AV_CODEC_ID_PAF_AUDIO),
      /** Tom's lossless Audio Kompressor (TAK). */
      TAK("TAK", AV_CODEC_ID_TAK),
      /** Voxware MetaSound. */
      METASOUND("METASOUND", AV_CODEC_ID_METASOUND),
      /** On2 AVC audio. */
      ON2AVC("ON2AVC", AV_CODEC_ID_ON2AVC),
      /** Digital Speech Standard SP (DSS SP). */
      DSS_SP("DSS SP", AV_CODEC_ID_DSS_SP),
      /** Codec 2 (open source speech codec). */
      CODEC2("CODEC2", AV_CODEC_ID_CODEC2),
      /** Enhanced Variable Rate Codec (EVRC). */
      EVRC("EVRC", AV_CODEC_ID_EVRC),
      /** Selectable Mode Vocoder (SMV). */
      SMV("SMV", AV_CODEC_ID_SMV),
      /** Direct Stream Digital (DSD) least-significant-bit-first. */
      DSD_LSBF("DSD LSBF", AV_CODEC_ID_DSD_LSBF),
      /** Direct Stream Digital (DSD) most-significant-bit-first. */
      DSD_MSBF("DSD MSBF", AV_CODEC_ID_DSD_MSBF),
      /** Direct Stream Digital (DSD) least-significant-bit-first, planar. */
      DSD_LSBF_PLANAR("DSD LSBF Planar", AV_CODEC_ID_DSD_LSBF_PLANAR),
      /** Direct Stream Digital (DSD) most-significant-bit-first, planar. */
      DSD_MSBF_PLANAR("DSD MSBF Planar", AV_CODEC_ID_DSD_MSBF_PLANAR),
      /** 4GV (QUALCOMM PureVoice) speech codec. */
      _4GV("4GV", AV_CODEC_ID_4GV),
      /** Interplay ACM audio. */
      INTERPLAY_ACM("INTERPLAY ACM", AV_CODEC_ID_INTERPLAY_ACM),
      /** Xbox Media Audio 1 (XMA1). */
      XMA1("XMA1", AV_CODEC_ID_XMA1),
      /** Xbox Media Audio 2 (XMA2). */
      XMA2("XMA2", AV_CODEC_ID_XMA2),
      /** Direct Stream Transfer (DST) lossless audio. */
      DST("DST", AV_CODEC_ID_DST),
      /** Dolby E audio. */
      DOLBY_E("Dolby E", AV_CODEC_ID_DOLBY_E),
      /** Qualcomm aptX Bluetooth audio codec. */
      APTX("aptX", AV_CODEC_ID_APTX),
      /** Qualcomm aptX HD Bluetooth audio codec. */
      APTX_HD("aptX HD", AV_CODEC_ID_APTX_HD),
      /** Bluetooth Sub-Band Coding (SBC). */
      SBC("SBC", AV_CODEC_ID_SBC),
      /** HCOM audio codec. */
      HCOM("HCOM", AV_CODEC_ID_HCOM),
      /** ACELP.KELVIN speech codec. */
      ACELP_KELVIN("ACELP.KELVIN", AV_CODEC_ID_ACELP_KELVIN),
      /** MPEG-H 3D Audio. */
      MPEGH_3D_AUDIO("MPEG-H 3D Audio", AV_CODEC_ID_MPEGH_3D_AUDIO),
      /** Siren audio codec (G.722.1). */
      SIREN("Siren", AV_CODEC_ID_SIREN),
      /** HCA (High Compression Audio, CRI Middleware). */
      HCA("HCA", AV_CODEC_ID_HCA),
      /** FastAudio codec. */
      FASTAUDIO("FastAudio", AV_CODEC_ID_FASTAUDIO),
      /** MSN Siren audio codec. */
      MSNSIREN("MSN Siren", AV_CODEC_ID_MSNSIREN),
      /** Decoder For Pure Waveform-based Modulation (DFPWM). */
      DFPWM("DFPWM", AV_CODEC_ID_DFPWM),
      /** Bonk lossless audio. */
      BONK("Bonk", AV_CODEC_ID_BONK),
      /** MISC4 audio codec. */
      MISC4("MISC4", AV_CODEC_ID_MISC4),
      /** APAC lossless audio. */
      APAC("APAC", AV_CODEC_ID_APAC),
      /** FTR audio codec. */
      FTR("FTR", AV_CODEC_ID_FTR),
      /** WavArc lossless audio. */
      WAVARC("WavArc", AV_CODEC_ID_WAVARC),
      /** RKA lossless audio. */
      RKA("RKA", AV_CODEC_ID_RKA),
      /** Dolby AC-4 audio. */
      AC4("AC-4", AV_CODEC_ID_AC4),
      /** Original Sound Quality (OSQ) lossless audio. */
      OSQ("OSQ", AV_CODEC_ID_OSQ),
      /** Quite OK Audio (QOA) lossy codec. */
      QOA("QOA", AV_CODEC_ID_QOA),
      /** Low Complexity Communication Codec (LC3). */
      LC3("LC3", AV_CODEC_ID_LC3),
      /** AHX audio codec. */
      AHX("AHX", AV_CODEC_ID_AHX);

      private FFEncoding encoding;
      private boolean pcm;
      private final String name;
      private final int id;

      /**
       * @param name name
       * @param id AVCodecID (should <em>not</em> be a PCM codec)
       */
      Codec(final String name, final int id) {
        this.name = name;
        this.id = id;
      }

      /**
       * @param name name
       * @param id AVCodecID
       * @param pcm indicates whether this codec is a PCM codec like e.g. PCM_S8_PLANAR
       */
      Codec(final String name, final int id, final boolean pcm) {
        this.name = name;
        this.id = id;
        this.pcm = pcm;
      }

      /**
       * Returns the {@link FFEncoding} for this codec, creating it lazily on first call.
       *
       * @return {@link FFEncoding} for this codec
       */
      public synchronized FFEncoding getEncoding() {
        if (encoding == null) {
          encoding = new FFEncoding(this);
        }
        return encoding;
      }

      /**
       * Returns the FFmpeg {@code AVCodecID} integer value for this codec.
       *
       * @return {@code AVCodecID} value
       */
      public int getId() {
        return id;
      }

      /**
       * Returns the human-readable name of this codec.
       *
       * @return codec name
       */
      public String getName() {
        return name;
      }

      /**
       * Returns {@code true} if this codec is a PCM variant.
       *
       * @return {@code true} for PCM codecs
       */
      public boolean isPCM() {
        return pcm;
      }
    }

    private static final Map<Integer, Codec> CODEC_ID_MAP = new HashMap<>();
    private static final Map<String, Codec> NAME_MAP = new HashMap<>();
    private static final Map<String, Codec> PCM_MAP = new HashMap<>();
    private static final Set<FFEncoding> SUPPORTED_ENCODINGS = new HashSet<>();

    private static String toPCMKey(final AudioFormat audioFormat) {
      final StringBuilder sb = new StringBuilder();
      final String encoding = audioFormat.getEncoding().toString();
      if (PCM_SIGNED.toString().equals(encoding)) sb.append("PCM_S");
      else if (PCM_UNSIGNED.toString().equals(encoding)) sb.append("PCM_U");
      else sb.append(encoding);
      sb.append("" + audioFormat.getSampleSizeInBits());
      if (!audioFormat.isBigEndian() || audioFormat.getSampleSizeInBits() <= 8) sb.append("LE");
      else sb.append("BE");
      return sb.toString();
    }

    static {
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 8, -1, -1, -1, false)),
          Codec.PCM_U8);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 16, -1, -1, -1, false)),
          Codec.PCM_U16LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 16, -1, -1, -1, true)),
          Codec.PCM_U16BE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 24, -1, -1, -1, false)),
          Codec.PCM_U24LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 24, -1, -1, -1, true)),
          Codec.PCM_U24BE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 32, -1, -1, -1, false)),
          Codec.PCM_U32LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 32, -1, -1, -1, true)),
          Codec.PCM_U32BE);

      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 8, -1, -1, -1, false)),
          Codec.PCM_S8);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 16, -1, -1, -1, false)),
          Codec.PCM_S16LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 16, -1, -1, -1, true)),
          Codec.PCM_S16BE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 24, -1, -1, -1, false)),
          Codec.PCM_S24LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 24, -1, -1, -1, true)),
          Codec.PCM_S24BE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 32, -1, -1, -1, false)),
          Codec.PCM_S32LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 32, -1, -1, -1, true)),
          Codec.PCM_S32BE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(
                  FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 64, -1, -1, -1, false)),
          Codec.PCM_S64LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 64, -1, -1, -1, true)),
          Codec.PCM_S64BE);

      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 32, -1, -1, -1, false)),
          Codec.PCM_F32LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 32, -1, -1, -1, true)),
          Codec.PCM_F32BE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 64, -1, -1, -1, false)),
          Codec.PCM_F64LE);
      PCM_MAP.put(
          toPCMKey(
              new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 64, -1, -1, -1, true)),
          Codec.PCM_F64BE);

      for (final Codec codec : Codec.values()) {
        CODEC_ID_MAP.put(codec.getEncoding().getCodecId(), codec);
        SUPPORTED_ENCODINGS.add(codec.getEncoding());
        NAME_MAP.put(codec.getName(), codec);
      }

      // make sure we have a neutral encoding for all three PCM kinds
      NAME_MAP.put(FFEncoding.Codec.PCM_SIGNED.getEncoding().toString(), Codec.PCM_SIGNED);
      NAME_MAP.put(FFEncoding.Codec.PCM_UNSIGNED.getEncoding().toString(), Codec.PCM_UNSIGNED);
      NAME_MAP.put(FFEncoding.Codec.PCM_FLOAT.getEncoding().toString(), Codec.PCM_FLOAT);
    }

    private final int codecId;

    /**
     * Creates an {@link FFEncoding} backed by the given {@link Codec}.
     *
     * @param codec the codec this encoding represents
     */
    public FFEncoding(final Codec codec) {
      super(codec.getName());
      this.codecId = codec.getId();
    }

    /**
     * Creates an {@link FFEncoding} with an explicit name and codec ID. Used for codecs not present
     * in the {@link Codec} enum.
     *
     * @param name encoding name
     * @param codecId FFmpeg {@code AVCodecID} integer value
     */
    public FFEncoding(final String name, final int codecId) {
      super(name);
      this.codecId = codecId;
    }

    /**
     * Returns the FFmpeg {@code AVCodecID} integer value for this encoding.
     *
     * @return {@code AVCodecID} value
     */
    public int getCodecId() {
      return codecId;
    }

    /**
     * Returns an unmodifiable set of all encodings backed by a known {@link Codec}.
     *
     * @return unmodifiable set of supported encodings
     */
    public static Set<FFEncoding> getSupportedEncodings() {
      return Collections.unmodifiableSet(SUPPORTED_ENCODINGS);
    }

    /**
     * Returns the {@link FFEncoding} matching the given PCM {@link AudioFormat}.
     *
     * @param audioFormat audio format whose encoding and sample size select the PCM variant
     * @return matching {@link FFEncoding}
     */
    public static FFEncoding getInstance(final AudioFormat audioFormat) {
      return PCM_MAP.get(toPCMKey(audioFormat)).getEncoding();
    }

    /**
     * Returns the {@link FFEncoding} with the given name, or {@code null} if unknown.
     *
     * @param name encoding name
     * @return matching {@link FFEncoding}, or {@code null}
     */
    public static FFEncoding getInstance(final String name) {
      final Codec codec = NAME_MAP.get(name);
      return codec == null ? null : codec.getEncoding();
    }

    /**
     * Returns the {@link FFEncoding} for the given FFmpeg codec ID. If the ID is not in the {@link
     * Codec} enum, a synthetic encoding is returned.
     *
     * @param codecId FFmpeg {@code AVCodecID} integer value
     * @return matching or synthetic {@link FFEncoding}, never {@code null}
     */
    public static FFEncoding getInstance(final int codecId) {
      Codec codec = CODEC_ID_MAP.get(codecId);
      if (codec == null) {
        // fake it
        return new FFEncoding(toString(codecId), codecId);
      }
      return codec.getEncoding();
    }

    /**
     * Returns the {@link Codec} for the given FFmpeg codec ID, or {@code null} if unknown.
     *
     * @param codecId FFmpeg {@code AVCodecID} integer value
     * @return matching {@link Codec}, or {@code null}
     */
    public static Codec getCodec(final int codecId) {
      return CODEC_ID_MAP.get(codecId);
    }

    private static String toString(final int codecId) {
      return new String(
          new char[] {
            (char) (codecId >> 24 & 0xff),
            (char) (codecId >> 16 & 0xff),
            (char) (codecId >> 8 & 0xff),
            (char) (codecId & 0xff)
          });
    }
  }
}
