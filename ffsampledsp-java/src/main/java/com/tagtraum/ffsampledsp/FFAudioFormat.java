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

import javax.sound.sampled.AudioFormat;
import java.util.*;

/**
 * FFSampledSP's {@link AudioFormat} adding a {@link #PROVIDER} property and a special constructor
 * to be called from {@link FFAudioFileFormat}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class FFAudioFormat extends AudioFormat {

    /**
     * Property key to mark where this {@link AudioFormat} came from.
     * @see #FFSAMPLEDSP
     */
    public static final String PROVIDER = "provider";

    /**
     * Special property value for {@link #PROVIDER}, marking FFSampledSP audio formats.
     * This allows us to take some shortcuts.
     * @see #PROVIDER
     * @see FFCodecInputStream
     */
    public static final String FFSAMPLEDSP = "ffsampledsp";

    public FFAudioFormat(final int codecId, final float sampleRate, final int sampleSize, final int channels,
                         final int packetSize, final float frameRate, final boolean bigEndian, final int bitRate, final Boolean vbr) {
        super(FFEncoding.getInstance(codecId), sampleRate, sampleSize, channels, packetSize, frameRate, bigEndian, createProperties(bitRate, vbr));
    }

    private static Map<String, Object> createProperties(final int bitRate, final Boolean vbr) {
        final Map<String, Object> properties = new HashMap<String, Object>();
        if (bitRate > 0) properties.put("bitrate", bitRate);
        if (vbr != null) properties.put("vbr", vbr);
        properties.put(PROVIDER, FFSAMPLEDSP);
        return properties;
    }

    /**
     * Special libavcodec encodings that are aware of their AVCodecID.
     */
    public static class FFEncoding extends Encoding {

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
        private static final int AV_CODEC_ID_PCM_S24LE_PLANAR = 0x18505350;
        private static final int AV_CODEC_ID_PCM_S32LE_PLANAR = 0x20505350;
        private static final int AV_CODEC_ID_PCM_S16BE_PLANAR = 0x50535010;
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
        private static final int AV_CODEC_ID_VIMA = 0x56494d41;
        private static final int AV_CODEC_ID_ADPCM_AFC = 0x41464320;
        private static final int AV_CODEC_ID_ADPCM_IMA_OKI = 0x4f4b4920;
        private static final int AV_CODEC_ID_AMR_NB = 0x12000;
        private static final int AV_CODEC_ID_AMR_WB = 0x12001;
        private static final int AV_CODEC_ID_RA_144 = 0x13000;
        private static final int AV_CODEC_ID_RA_288 = 0x13001;
        private static final int AV_CODEC_ID_ROQ_DPCM = 0x14000;
        private static final int AV_CODEC_ID_INTERPLAY_DPCM = 0x14001;
        private static final int AV_CODEC_ID_XAN_DPCM = 0x14002;
        private static final int AV_CODEC_ID_SOL_DPCM = 0x14003;
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
        private static final int AV_CODEC_ID_VOXWARE = 0x15020;
        private static final int AV_CODEC_ID_APE = 0x15021;
        private static final int AV_CODEC_ID_NELLYMOSER = 0x15022;
        private static final int AV_CODEC_ID_MUSEPACK8 = 0x15023;
        private static final int AV_CODEC_ID_SPEEX = 0x15024;
        private static final int AV_CODEC_ID_WMAVOICE = 0x15025;
        private static final int AV_CODEC_ID_WMAPRO = 0x15026;
        private static final int AV_CODEC_ID_WMALOSSLESS = 0x15027;
        private static final int AV_CODEC_ID_ATRAC3P = 0x15028;
        private static final int AV_CODEC_ID_EAC3 = 0x15029;
        private static final int AV_CODEC_ID_SIPR = 0x1502a;
        private static final int AV_CODEC_ID_MP1 = 0x1502b;
        private static final int AV_CODEC_ID_TWINVQ = 0x1502c;
        private static final int AV_CODEC_ID_TRUEHD = 0x1502d;
        private static final int AV_CODEC_ID_MP4ALS = 0x1502e;
        private static final int AV_CODEC_ID_ATRAC1 = 0x1502f;
        private static final int AV_CODEC_ID_BINKAUDIO_RDFT = 0x15030;
        private static final int AV_CODEC_ID_BINKAUDIO_DCT = 0x15031;
        private static final int AV_CODEC_ID_AAC_LATM = 0x15032;
        private static final int AV_CODEC_ID_QDMC = 0x15033;
        private static final int AV_CODEC_ID_CELT = 0x15034;
        private static final int AV_CODEC_ID_G723_1 = 0x15035;
        private static final int AV_CODEC_ID_G729 = 0x15036;
        private static final int AV_CODEC_ID_8SVX_EXP = 0x15037;
        private static final int AV_CODEC_ID_8SVX_FIB = 0x15038;
        private static final int AV_CODEC_ID_BMV_AUDIO = 0x15039;
        private static final int AV_CODEC_ID_RALF = 0x1503a;
        private static final int AV_CODEC_ID_IAC = 0x1503b;
        private static final int AV_CODEC_ID_ILBC = 0x1503c;
        private static final int AV_CODEC_ID_OPUS_DEPRECATED = 0x1503d;
        private static final int AV_CODEC_ID_COMFORT_NOISE = 0x1503e;
        private static final int AV_CODEC_ID_TAK_DEPRECATED = 0x1503f;
        private static final int AV_CODEC_ID_FFWAVESYNTH = 0x46465753;
        private static final int AV_CODEC_ID_8SVX_RAW = 0x38535658;
        private static final int AV_CODEC_ID_SONIC = 0x534f4e43;
        private static final int AV_CODEC_ID_SONIC_LS = 0x534f4e4c;
        private static final int AV_CODEC_ID_PAF_AUDIO = 0x50414641;
        private static final int AV_CODEC_ID_OPUS = 0x4f505553;
        private static final int AV_CODEC_ID_TAK = 0x7442614b;
        private static final int AV_CODEC_ID_EVRC = 0x73657663;
        private static final int AV_CODEC_ID_SMV = 0x73736d76;

        /**
         * Float PCM - named just like the <code>PCM_FLOAT</code> in {@link javax.sound.sampled.AudioFormat.Encoding}.
         * in Java 7 (not used for compatibility with Java &lt;7).
         */
        private static final String PCM_FLOAT_STRING = "PCM_FLOAT";

        /**
         * Codec supported by libavcodec.
         */
        public enum Codec {
            MP1("MPEG-1, Layer 1", AV_CODEC_ID_MP1),
            MP2("MPEG-1, Layer 2", AV_CODEC_ID_MP2),
            MP3("MPEG-1, Layer 3", AV_CODEC_ID_MP3),

            APPLE_LOSSLESS("Apple Lossless", AV_CODEC_ID_ALAC),

            MPEG4_AAC("MPEG4 AAC", AV_CODEC_ID_AAC),
            MPEG4_AAC_LATM("MPEG4 AAC-LATM", AV_CODEC_ID_AAC_LATM),

            MPEG4_QCELP("QCELP", AV_CODEC_ID_QCELP),
            MPEG4_TWINVQ("MPEG4 TwinVQ", AV_CODEC_ID_TWINVQ),
            MPEG4_ALS("MPEG4 Audio Lossless Coding", AV_CODEC_ID_MP4ALS),

            ULAW(Encoding.ULAW.toString(), AV_CODEC_ID_PCM_MULAW),
            ALAW(Encoding.ALAW.toString(), AV_CODEC_ID_PCM_ALAW),

            PCM_SIGNED(Encoding.PCM_SIGNED.toString(), -1, true),
            PCM_UNSIGNED(Encoding.PCM_UNSIGNED.toString(), -1, true),
            PCM_FLOAT(PCM_FLOAT_STRING, -1, true),

            // signed pcm
            PCM_S8(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S8, true),
            PCM_S16BE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S16BE, true),
            PCM_S16LE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S16LE, true),
            PCM_S24BE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S24BE, true),
            PCM_S24LE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S24LE, true),
            PCM_S32BE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S32BE, true),
            PCM_S32LE(Encoding.PCM_SIGNED.toString(), AV_CODEC_ID_PCM_S32LE, true),

            // unsigned pcm
            PCM_U8(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U8, true),
            PCM_U16BE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U16BE, true),
            PCM_U16LE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U16LE, true),
            PCM_U24BE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U24BE, true),
            PCM_U24LE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U24LE, true),
            PCM_U32BE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U32BE, true),
            PCM_U32LE(Encoding.PCM_UNSIGNED.toString(), AV_CODEC_ID_PCM_U32LE, true),

            // float pcm - named just like the PCM_FLOAT in encoding
            // in Java 7 (not used for compatibility with Java <=6)
            PCM_F32BE(PCM_FLOAT_STRING, AV_CODEC_ID_PCM_F32BE, true),
            PCM_F32LE(PCM_FLOAT_STRING, AV_CODEC_ID_PCM_F32LE, true),
            PCM_F64BE(PCM_FLOAT_STRING, AV_CODEC_ID_PCM_F64BE, true),
            PCM_F64LE(PCM_FLOAT_STRING, AV_CODEC_ID_PCM_F64LE, true),

            // planar pcm
            PCM_S8_PLANAR("PCM S8 Planar", AV_CODEC_ID_PCM_S8_PLANAR, true),
            PCM_S24LE_PLANAR("PCM S24LE Planar", AV_CODEC_ID_PCM_S24LE_PLANAR, true),
            PCM_S32LE_PLANAR("PCM S32LE Planar", AV_CODEC_ID_PCM_S32LE_PLANAR, true),
            PCM_S16BE_PLANAR("PCM S16BE Planar", AV_CODEC_ID_PCM_S16BE_PLANAR, true),
            PCM_S16LE_PLANAR("PCM S16LE Planar", AV_CODEC_ID_PCM_S16LE_PLANAR, true),

            // other pcm
            PCM_S24DAUD("PCM S24 DAUD", AV_CODEC_ID_PCM_S24DAUD, true),
            PCM_ZORK("Zork", AV_CODEC_ID_PCM_ZORK, true),
            PCM_DVD("PCM DVD", AV_CODEC_ID_PCM_DVD, true),
            PCM_BLURAY("PCM BLURAY", AV_CODEC_ID_PCM_BLURAY, true),
            PCM_LXF("PCM LXF", AV_CODEC_ID_PCM_LXF, true),


            I_LBC("iLBC", AV_CODEC_ID_ILBC),
            MICROSOFT_GSM("Microsoft GSM", AV_CODEC_ID_GSM_MS),
            AMR_WB("AMR WB", AV_CODEC_ID_AMR_WB),
            AMR_NB("AMR NB", AV_CODEC_ID_AMR_NB),
            AC3("AC3", AV_CODEC_ID_AC3),
            VORBIS("AC3", AV_CODEC_ID_VORBIS),
            FLAC("AC3", AV_CODEC_ID_FLAC),
            DTS("AC3", AV_CODEC_ID_DTS),

            WMA_V1("WMA 1", AV_CODEC_ID_WMAV1),
            WMA_V2("WMA 2", AV_CODEC_ID_WMAV2),
            WMA_LOSSLESS("WMA Lossless", AV_CODEC_ID_WMALOSSLESS),
            WMA_PRO("WMA Pro", AV_CODEC_ID_WMAPRO),
            WMA_VOICE("WMA Voice", AV_CODEC_ID_WMAVOICE),

            ATRAC_1("ATRAC 1", AV_CODEC_ID_ATRAC1),
            ATRAC_3("ATRAC 3", AV_CODEC_ID_ATRAC3),
            ATRAC_3P("ATRAC 3plus", AV_CODEC_ID_ATRAC3P),

            GSM("GSM", AV_CODEC_ID_GSM),
            TRUE_HD("TrueHD", AV_CODEC_ID_TRUEHD),
            TRUESPEECH("Truespeech", AV_CODEC_ID_TRUESPEECH),

            NELLYMOSER("Nellymoser", AV_CODEC_ID_NELLYMOSER),
            SPEEX("Speex", AV_CODEC_ID_SPEEX),

            S302M("S302M", AV_CODEC_ID_S302M),

            // ADPCM codecs
            ADPCM_SWF("ADPCM SWF", AV_CODEC_ID_ADPCM_SWF),
            ADPCM_IMA_QT("ADPCM IMA QT", AV_CODEC_ID_ADPCM_IMA_QT),
            ADPCM_IMA_WAV("ADPCM IMA WAV", AV_CODEC_ID_ADPCM_IMA_WAV),
            ADPCM_IMA_DK3("ADPCM IMA DK3", AV_CODEC_ID_ADPCM_IMA_DK3),
            ADPCM_IMA_DK4("ADPCM IMA DK4", AV_CODEC_ID_ADPCM_IMA_DK4),
            ADPCM_IMA_WS("ADPCM IMA WS", AV_CODEC_ID_ADPCM_IMA_WS),
            ADPCM_IMA_SMJPEG("ADPCM IMA SMJPEG", AV_CODEC_ID_ADPCM_IMA_SMJPEG),
            ADPCM_MS("ADPCM MS", AV_CODEC_ID_ADPCM_MS),
            ADPCM_4XM("ADPCM 4XM", AV_CODEC_ID_ADPCM_4XM),
            ADPCM_XA("ADPCM XA", AV_CODEC_ID_ADPCM_XA),
            ADPCM_ADX("ADPCM ADX", AV_CODEC_ID_ADPCM_ADX),
            ADPCM_EA("ADPCM EA", AV_CODEC_ID_ADPCM_EA),
            ADPCM_G726("ADPCM G726", AV_CODEC_ID_ADPCM_G726),
            ADPCM_CT("ADPCM CT", AV_CODEC_ID_ADPCM_CT),
            ADPCM_YAMAHA("ADPCM YAMAHA", AV_CODEC_ID_ADPCM_YAMAHA),
            ADPCM_SBPRO_4("ADPCM SBPRO_4", AV_CODEC_ID_ADPCM_SBPRO_4),
            ADPCM_SBPRO_3("ADPCM SBPRO_3", AV_CODEC_ID_ADPCM_SBPRO_3),
            ADPCM_SBPRO_2("ADPCM SBPRO_2", AV_CODEC_ID_ADPCM_SBPRO_2),
            ADPCM_THP("ADPCM THP", AV_CODEC_ID_ADPCM_THP),
            ADPCM_IMA_AMV("ADPCM IMA AMV", AV_CODEC_ID_ADPCM_IMA_AMV),
            ADPCM_EA_R1("ADPCM EA R1", AV_CODEC_ID_ADPCM_EA_R1),
            ADPCM_EA_R3("ADPCM EA R3", AV_CODEC_ID_ADPCM_EA_R3),
            ADPCM_EA_R2("ADPCM EA R2", AV_CODEC_ID_ADPCM_EA_R2),
            ADPCM_IMA_EA_SEAD("ADPCM IMA EA SEAD", AV_CODEC_ID_ADPCM_IMA_EA_SEAD),
            ADPCM_IMA_EA_EACS("ADPCM IMA EA EACS", AV_CODEC_ID_ADPCM_IMA_EA_EACS),
            ADPCM_EA_XAS("ADPCM EA XAS", AV_CODEC_ID_ADPCM_EA_XAS),
            ADPCM_EA_MAXIS_XA("ADPCM EA MAXIS XA", AV_CODEC_ID_ADPCM_EA_MAXIS_XA),
            ADPCM_IMA_ISS("ADPCM IMA ISS", AV_CODEC_ID_ADPCM_IMA_ISS),
            ADPCM_G722("ADPCM G722", AV_CODEC_ID_ADPCM_G722),
            ADPCM_IMA_APC("ADPCM IMA APC", AV_CODEC_ID_ADPCM_IMA_APC),
            ADPCM_AFC("ADPCM AFC", AV_CODEC_ID_ADPCM_AFC),
            ADPCM_IMA_OKI("ADPCM IMA OKI", AV_CODEC_ID_ADPCM_IMA_OKI),

            VIMA("VIMA", AV_CODEC_ID_VIMA),
            RA_144("RA 144", AV_CODEC_ID_RA_144),
            RA_288("RA_288", AV_CODEC_ID_RA_288),
            ROQ_DPCM("ROQ DPCM", AV_CODEC_ID_ROQ_DPCM),
            INTERPLAY_DPCM("INTERPLAY DPCM", AV_CODEC_ID_INTERPLAY_DPCM),
            XAN_DPCM("XAN DPCM", AV_CODEC_ID_XAN_DPCM),
            SOL_DPCM("SOL DPCM", AV_CODEC_ID_SOL_DPCM),

            DVAUDIO("DVAUDIO", AV_CODEC_ID_DVAUDIO),
            MACE3("MACE3", AV_CODEC_ID_MACE3),
            MACE6("MACE6", AV_CODEC_ID_MACE6),
            VMDAUDIO("VMDAUDIO", AV_CODEC_ID_VMDAUDIO),
            MP3ADU("MP3ADU", AV_CODEC_ID_MP3ADU),
            MP3ON4("MP3ON4", AV_CODEC_ID_MP3ON4),
            SHORTEN("SHORTEN", AV_CODEC_ID_SHORTEN),
            WESTWOOD_SND1("WESTWOOD SND1", AV_CODEC_ID_WESTWOOD_SND1),
            QDM2("QDM2", AV_CODEC_ID_QDM2),
            COOK("COOK", AV_CODEC_ID_COOK),
            TTA("TTA", AV_CODEC_ID_TTA),
            SMACKAUDIO("SMACKAUDIO", AV_CODEC_ID_SMACKAUDIO),
            WAVPACK("WAVPACK", AV_CODEC_ID_WAVPACK),
            DSICINAUDIO("DSICINAUDIO", AV_CODEC_ID_DSICINAUDIO),
            IMC("IMC", AV_CODEC_ID_IMC),
            MUSEPACK7("MUSEPACK7", AV_CODEC_ID_MUSEPACK7),
            MLP("MLP", AV_CODEC_ID_MLP),
            VOXWARE("VOXWARE", AV_CODEC_ID_VOXWARE),
            APE("APE", AV_CODEC_ID_APE),
            MUSEPACK8("MUSEPACK8", AV_CODEC_ID_MUSEPACK8),
            EAC3("EAC3", AV_CODEC_ID_EAC3),
            SIPR("SIPR", AV_CODEC_ID_SIPR),
            BINKAUDIO_RDFT("BINKAUDIO RDFT", AV_CODEC_ID_BINKAUDIO_RDFT),
            BINKAUDIO_DCT("BINKAUDIO DCT", AV_CODEC_ID_BINKAUDIO_DCT),
            QDMC("QDMC", AV_CODEC_ID_QDMC),
            CELT("CELT", AV_CODEC_ID_CELT),
            G723_1("G723 1", AV_CODEC_ID_G723_1),
            G729("G729", AV_CODEC_ID_G729),
            _8SVX_EXP("8SVX EXP", AV_CODEC_ID_8SVX_EXP),
            _8SVX_FIB("8SVX FIB", AV_CODEC_ID_8SVX_FIB),
            BMV_AUDIO("BMV_AUDIO", AV_CODEC_ID_BMV_AUDIO),
            RALF("RALF", AV_CODEC_ID_RALF),
            IAC("IAC", AV_CODEC_ID_IAC),
            OPUS_DEPRECATED("OPUS DEPRECATED", AV_CODEC_ID_OPUS_DEPRECATED),
            COMFORT_NOISE("COMFORT NOISE", AV_CODEC_ID_COMFORT_NOISE),
            TAK_DEPRECATED("TAK DEPRECATED", AV_CODEC_ID_TAK_DEPRECATED),
            FFWAVESYNTH("FFWAVESYNTH", AV_CODEC_ID_FFWAVESYNTH),
            _8SVX_RAW("8SVX RAW", AV_CODEC_ID_8SVX_RAW),
            SONIC("SONIC", AV_CODEC_ID_SONIC),
            SONIC_LS("SONIC_LS", AV_CODEC_ID_SONIC_LS),
            PAF_AUDIO("PAF_AUDIO", AV_CODEC_ID_PAF_AUDIO),
            OPUS("OPUS", AV_CODEC_ID_OPUS),
            TAK("TAK", AV_CODEC_ID_TAK),
            EVRC("EVRC", AV_CODEC_ID_EVRC),
            SMV("SMV", AV_CODEC_ID_SMV);

            private FFEncoding encoding;
            private boolean pcm;
            private String name;
            private int id;

            /**
             * @param name name
             * @param id AVCodecID (should <em>not</em> be a PCM codec)
             */
            private Codec(final String name, final int id) {
                this.name = name;
                this.id = id;
            }

            /**
             * @param name name
             * @param id AVCodecID
             * @param pcm indicates whether this codec is a PCM codec like e.g. PCM_S8_PLANAR
             */
            private Codec(final String name, final int id, final boolean pcm) {
                this.name = name;
                this.id = id;
                this.pcm = pcm;
            }

            public synchronized FFEncoding getEncoding() {
                if (encoding == null) {
                    encoding = new FFEncoding(this);
                }
                return encoding;
            }

            public int getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            public boolean isPCM() {
                return pcm;
            }

        }

        private static Map<Integer, Codec> CODEC_ID_MAP = new HashMap<Integer, Codec>();
        private static Map<String, Codec> NAME_MAP = new HashMap<String, Codec>();
        private static Map<String, Codec> PCM_MAP = new HashMap<String, Codec>();
        private static Set<FFEncoding> SUPPORTED_ENCODINGS = new HashSet<FFEncoding>();

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
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 8, -1, -1, -1, false)), Codec.PCM_U8);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 16, -1, -1, -1, false)), Codec.PCM_U16LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 16, -1, -1, -1, true)), Codec.PCM_U16BE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 24, -1, -1, -1, false)), Codec.PCM_U24LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 24, -1, -1, -1, true)), Codec.PCM_U24BE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 32, -1, -1, -1, false)), Codec.PCM_U32LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_UNSIGNED.getEncoding(), -1, 32, -1, -1, -1, true)), Codec.PCM_U32BE);

            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 8, -1, -1, -1, false)), Codec.PCM_S8);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 16, -1, -1, -1, false)), Codec.PCM_S16LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 16, -1, -1, -1, true)), Codec.PCM_S16BE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 24, -1, -1, -1, false)), Codec.PCM_S24LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 24, -1, -1, -1, true)), Codec.PCM_S24BE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 32, -1, -1, -1, false)), Codec.PCM_S32LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_SIGNED.getEncoding(), -1, 32, -1, -1, -1, true)), Codec.PCM_S32BE);

            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 32, -1, -1, -1, false)), Codec.PCM_F32LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 32, -1, -1, -1, true)), Codec.PCM_F32BE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 64, -1, -1, -1, false)), Codec.PCM_F64LE);
            PCM_MAP.put(toPCMKey(new AudioFormat(FFEncoding.Codec.PCM_FLOAT.getEncoding(), -1, 64, -1, -1, -1, true)), Codec.PCM_F64BE);

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

        private int codecId;

        public FFEncoding(final Codec codec) {
            super(codec.getName());
            this.codecId = codec.getId();
        }

        public FFEncoding(final String name, final int codecId) {
            super(name);
            this.codecId = codecId;
        }

        public int getCodecId() {
            return codecId;
        }

        public static Set<FFEncoding> getSupportedEncodings() {
            return Collections.unmodifiableSet(SUPPORTED_ENCODINGS);
        }

        public static FFEncoding getInstance(final AudioFormat audioFormat) {
            return PCM_MAP.get(toPCMKey(audioFormat)).getEncoding();
        }

        public static FFEncoding getInstance(final String name) {
            final Codec codec = NAME_MAP.get(name);
            return codec == null ? null : codec.getEncoding();
        }

        public static FFEncoding getInstance(final int codecId) {
            Codec codec = CODEC_ID_MAP.get(codecId);
            if (codec == null) {
                // fake it
                return new FFEncoding(toString(codecId), codecId);
            }
            return codec.getEncoding();
        }

        public static Codec getCodec(final int codecId) {
            return CODEC_ID_MAP.get(codecId);
        }

        private static String toString(final int codecId) {
            return new String(
                    new char[]{(char) (codecId >> 24 & 0xff), (char) (codecId >> 16 & 0xff),
                            (char) (codecId >> 8 & 0xff), (char) (codecId & 0xff)}
            );
        }

    }
}
