# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Task Execution Guidelines

Follow these practices for all non-trivial work in this repository.

### Plan before coding

Before writing any code, think through the approach. For any task beyond a trivial fix:
1. Draft a plan — identify the files to change, the interfaces to add/modify, and the test cases needed.
2. Review the plan against the constraints in this file.
3. Only start editing once the plan is coherent. A bad plan caught early is far cheaper than a bad implementation caught late.

### Write tests first

When adding new functionality, write the failing test before writing the implementation. A failing test creates an immediate, objective feedback loop. This dramatically improves reliability — run the test after every meaningful change to stay oriented.

### Break work into small increments

Prefer many small, verifiable steps over one large change. Each increment should:
- Leave the codebase in a working state (tests pass, linter clean).
- Be independently reviewable.

If a task feels too large to hold in one session, decompose it further.

### Run Spotless after editing Java

A Spotless formatter is configured for all Java modules. It runs automatically during `install`/`verify`, but a failed format check blocks the build. After editing Java sources run:

```bash
mvn spotless:apply
# or for a single module:
mvn spotless:apply -pl ffsampledsp-java
mvn spotless:apply -pl ffsampledsp-complete
```

---

## Project Overview

FFSampledSP is a Java/JNI library that implements `javax.sound.sampled` service provider interfaces (SPIs) backed by FFmpeg. It decodes audio files/streams to PCM — signed integer (`PCM_SIGNED`) or floating-point (`PCM_FLOAT`). Licensed under LGPL 2.1. Requires Java 8 or later.

## Build Commands

The build requires Maven 3.6+, a JDK 8+, and Doxygen. Native compilation requires platform-specific toolchains. **A platform profile must be activated** — native modules are not built by default.

### macOS (native for current platform)
```bash
# Build and test for aarch64 (Apple Silicon)
mvn --activate-profiles ffsampledsp-aarch64-macos install

# Build and test for x86_64
mvn --activate-profiles ffsampledsp-x86_64-macos install
```

### Linux
```bash
mvn --activate-profiles ffsampledsp-x86_64-linux install
# For aarch64 cross-compile (requires aarch64-linux-gnu-gcc, tests are skipped):
mvn --activate-profiles ffsampledsp-aarch64-linux install
```

### Windows (requires MSYS2 with MinGW toolchain)
```bash
mvn --activate-profiles ffsampledsp-x86_64-win install   # 64-bit
mvn --activate-profiles ffsampledsp-i386-win install     # 32-bit (tests skipped)
```

### Java-only (no native compilation)
```bash
mvn install  # builds ffsampledsp-java and ffsampledsp-complete only
```

### Run tests
Tests live in `ffsampledsp-complete/src/test/java/`. They require the native library to be built first (via a platform profile).

```bash
# Run all tests (after native build)
mvn --activate-profiles ffsampledsp-aarch64-macos test

# Run a single test class
mvn --activate-profiles ffsampledsp-aarch64-macos test \
  -pl ffsampledsp-complete \
  -Dtest=TestFFAudioFileReader

# Run specific test methods
mvn --activate-profiles ffsampledsp-aarch64-macos test \
  -pl ffsampledsp-complete \
  -Dtest="TestFFCodecInputStream#testConvertWavToFloat32SamplesInRange"
```

### Debug builds
Pass `-Dcflags=-DDEBUG` to enable C-level debug output to stdout.

## Architecture

### Module Structure

- **`ffsampledsp-java/`** — Pure Java SPI implementations. The authoritative Java source; compiled standalone but also copied into `ffsampledsp-complete` at build time.
- **`ffsampledsp-x86_64-macos/`** — Canonical C source module. All C sources live here; all other platform modules reference the same `src/main/c/` directory via their pom.
- **`ffsampledsp-{arch}-{host}/`** — Per-platform native modules (`aarch64-macos`, `x86_64-linux`, `aarch64-linux`, `x86_64-win`, `i386-win`). Each packages a `.dylib`/`.so`/`.dll` built from the canonical C sources.
- **`ffsampledsp-complete/`** — The distribution artifact. Copies Java sources from `ffsampledsp-java` and embeds the native library from whichever platform profile is active. This is the jar users depend on.

### Java/JNI Layer

The Java classes in `com.tagtraum.ffsampledsp` implement the `javax.sound.sampled.spi` interfaces:

- **`FFAudioFileReader`** — implements `AudioFileReader`. Opens URLs/files/streams via FFmpeg. Caches results (LRU, 20 entries). Has a `getAudioFileFormats()` extension returning multiple formats for multi-stream files (e.g. Stems). The three-argument overloads `getAudioInputStream(file/url, streamIndex, fileBufferSize)` accept an explicit I/O buffer size. Calls into native via two `native` methods: `getAudioFileFormatsFromURL` and `getAudioFileFormatsFromBuffer`.
- **`FFFormatConversionProvider`** — implements `AudioFormatConversionProvider`. Transcodes compressed streams to PCM (`PCM_SIGNED`, `PCM_UNSIGNED`, `PCM_FLOAT`). Standard `AudioFormat.Encoding` constants (e.g. `AudioFormat.Encoding.PCM_FLOAT`) work interchangeably with `FFAudioFormat.FFEncoding` values via string-based resolution. `getAudioInputStream(Encoding, AudioInputStream)` defaults to 32-bit for `PCM_FLOAT` (never inherits a sub-32-bit source depth).
- **`FFNativePeerInputStream`** — abstract base for native-backed `InputStream`s. Holds a `long pointer` to the native C struct and a direct `ByteBuffer` (`nativeBuffer`) that the C side fills.
  - **`FFURLInputStream`** — decodes from a URL/file path. Configures the FFmpeg I/O buffer size (`AVFormatContext.io_buffer_size`) passed as `fileBufferSize`. Default for `file:` URLs: 1 MB (override with `-Dffsampledsp.fileBufferSize=N`). Default for other URLs: 64 KB (override with `-Dffsampledsp.urlBufferSize=N`).
  - **`FFStreamInputStream`** — decodes from a Java `InputStream` (reads into a buffer, probes format, then decodes).
  - **`FFCodecInputStream`** — handles format conversion (resampling/channel mapping/sample-format) using `libswresample`. Supports `PCM_SIGNED` (8/16/24/32-bit), `PCM_UNSIGNED` (8/16/24/32-bit), and `PCM_FLOAT` (32-bit and 64-bit). Float output is normalized to `[-1, 1]` by libswresample for integer sources; lossy codecs (MP3, AAC) may produce inter-sample peaks slightly outside this range.
- **`FFAudioFormat`** — defines `FFEncoding` (extends `AudioFormat.Encoding`) and the `Codec` enum. All float codec variants (`PCM_F32LE/BE`, `PCM_F64LE/BE`, etc.) use `Encoding.PCM_FLOAT.toString()` as their encoding name — no hardcoded string constant.
- **`FFAudioInputStream`** — wraps an `FFNativePeerInputStream`, implements seeking via `FFGlobalLock`.
- **`FFGlobalLock`** — a single `ReentrantLock` (`LOCK`) used to serialize FFmpeg calls that are not thread-safe (`avcodec_open2`, etc.).
- **`FFNativeLibraryLoader`** — extracts the embedded native library to `java.io.tmpdir` and loads it. Naming convention: `ffsampledsp-{arch}-{host}.{ext}` (e.g. `ffsampledsp-aarch64-macos.dylib`).

All native sources live in one directory — all platforms share them: `ffsampledsp-x86_64-macos/src/main/c/`

Java language/compiler target is `release=8`, set in the root `pom.xml`.

### C Native Layer (`ffsampledsp-x86_64-macos/src/main/c/`)

- **`FFUtils.c` / `FFUtils.h`** — shared helpers: JNI field/method ID caching, buffer management, FFmpeg context lifecycle, DRM detection (`CODEC_TAG_DRMS`). Minimum probe score of 5 prevents misdetecting files that other `javax.sound.sampled` providers should handle.
- **`FFAudioFileReader.c`** — native implementation of the two `FFAudioFileReader` native methods. Probes format, fills Java `FFAudioFileFormat` / `FFAudioFormat` objects.
- **`FFURLInputStream.c`** — opens an `AVFormatContext` from a URL, configures `AVFormatContext.io_buffer_size` from the Java-side `fileBufferSize`, decodes packets into the Java `nativeBuffer`.
- **`FFStreamInputStream.c`** — uses FFmpeg's custom I/O (`AVIOContext` with read callbacks) to pull data from a Java `InputStream`.
- **`FFCodecInputStream.c`** — wraps `libswresample` for PCM conversion. Output sample format is selected from the Java-side `AudioFormat`: `AV_SAMPLE_FMT_S16` for 16-bit signed, `AV_SAMPLE_FMT_FLT` for 32-bit float, `AV_SAMPLE_FMT_DBL` for 64-bit float, etc. Uses the modern `av_opt_set_*()` API (not the deprecated `swr_alloc_set_opts()`).

### Key Design Points

- The native library is embedded inside `ffsampledsp-complete.jar` and extracted to a temp file on first load. The extracted filename includes the version to allow side-by-side installs; SNAPSHOT builds are always re-extracted.
- All calls touching FFmpeg's non-thread-safe API are wrapped in `FFGlobalLock.LOCK`.
- Windows URLs require a special format for libav: `file:C:/path/file` (not `file:///C:/path/file`). UNC paths use `file://server/path`. This conversion happens in `FFAudioFileReader.urlToString()`.
- The `fileToURL` method explicitly decodes then re-encodes file URIs to preserve `+` characters in paths (a known edge case).
- JNI headers are auto-generated by `javac -h` during the `compile` phase into `target/native/include/`, then consumed by the platform-specific native module.
- `FFCodecInputStream` uses a `((Buffer) nativeBuffer).limit(0)` cast to work around the covariant `ByteBuffer.limit(int)` return type introduced in Java 9.
- `PCM_FLOAT` support works with the standard `AudioFormat.Encoding.PCM_FLOAT` constant (Java 7+) because all provider methods resolve encodings by calling `FFAudioFormat.FFEncoding.getInstance(encoding.toString())`.
