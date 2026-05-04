# ffsampledsp-complete

Distribution artifact. Bundles:
- Java sources copied from `ffsampledsp-java` (by the Maven resources plugin at build time)
- The native library embedded from whichever platform profile is active

This is the jar users add as a Maven dependency.

## Build

Requires a platform profile to be active (otherwise only Java sources are present, no native lib):

```bash
mvn --activate-profiles ffsampledsp-aarch64-macos install
```

After editing Java sources in `ffsampledsp-java`, fix formatting before the build:

```bash
mvn spotless:apply -pl ffsampledsp-complete
```

## Test Suite

All tests live in `src/test/java/com/tagtraum/ffsampledsp/`. They require the native library to be present (i.e. a platform build must have run first).

| Test class | What it covers |
|---|---|
| `TestAudioSystemIntegration` | End-to-end via `AudioSystem`; includes `testDecodeMp3ToFloatPCM` mirroring the README float sample |
| `TestFFAudioFileReader` | Format detection: encoding, sample rate, channels, frame size, duration, bitrate for all supported formats |
| `TestFFAudioFileFormat` | Supported encoding lookup, codec mapping |
| `TestFFAudioFormat` | `FFEncoding` and `Codec` enum, PCM map, name map |
| `TestFFFormatConversionProvider` | `getTargetEncodings`, `getTargetFormats`, `isConversionSupported`, end-to-end conversions including `PCM_FLOAT` via standard `AudioFormat.Encoding` |
| `TestFFCodecInputStream` | Direct `FFCodecInputStream` conversion tests; PCM_SIGNED, PCM_UNSIGNED, PCM_FLOAT 32-bit and 64-bit with sample-value assertions |
| `TestFFURLInputStream` | URL opening, emoji/special-character paths |
| `TestFFStreamInputStream` | Stream-based decoding, reference sample values |
| `TestFFStreamInputStreamExtended` | Short files, concurrent reads |
| `TestFFNativeLibraryLoader` | Library extraction and loading |

### Test resources

`src/test/resources/com/tagtraum/ffsampledsp/` contains audio files for testing:
- `test.wav` — PCM_SIGNED S16, stereo, 44100 Hz (primary reference file)
- `test24bit.wav` / `test_long24bit.wav` — 24-bit WAV
- `test.mp3` / `test_cbr256.mp3` / `test_vbr130.mp3` — MP3
- `test.flac` / `test24bit.flac` — FLAC (standard and 24-bit)
- `test.m4a` / `test_cbr.m4a` / `test_vbr.m4a` / `test_48k_alac.m4a` — M4A/AAC and ALAC
- `test.w64` — Sony Wave64, PCM_FLOAT 64-bit, stereo 44100 Hz (primary 64-bit float reference)
- `test.ogg` / `test.aiff` / `test.wma` — OGG, AIFF, WMA
- `test_adpcm.wav` — ADPCM WAV
- `test.stem.mp4` — multi-stream Stems file

### PCM_FLOAT test coverage

- **32-bit float**: `testConvertWavToFloat32SamplesInRange`, `testConvertMp3ToFloat32SamplesFiniteAndReasonable`, `testConvertFlac24ToFloat32SamplesInRange` (in `TestFFCodecInputStream`); `testConvertWavToFloat32ViaStandardEncoding` (in `TestFFFormatConversionProvider`)
- **64-bit float**: `testConvertWavToFloat64SamplesInRange`, `testConvertFlac24ToFloat64SamplesInRange` (in `TestFFCodecInputStream`); `testConvertWavToFloat64ViaProvider`, `testReadW64ToFloat64ViaStandardEncoding` (in `TestFFFormatConversionProvider`)
- Float samples from lossless sources (WAV, FLAC) are asserted to be in `[-1.0, 1.0]`; lossy sources (MP3) assert `Float.isFinite` only, as inter-sample peaks slightly above 1.0 are expected
