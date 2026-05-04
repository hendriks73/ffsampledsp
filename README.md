[![LGPL 2.1](https://img.shields.io/badge/License-LGPL_2.1-blue.svg)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html)
[![Maven Central](https://img.shields.io/maven-central/v/com.tagtraum/ffsampledsp)](https://central.sonatype.com/artifact/com.tagtraum/ffsampledsp)
[![Build and Test](https://github.com/hendriks73/ffsampledsp/workflows/Build%20and%20Test/badge.svg)](https://github.com/hendriks73/ffsampledsp/actions)
[![CodeCov](https://codecov.io/gh/hendriks73/ffsampledsp/branch/main/graph/badge.svg?token=7K9ACGFWY4)](https://codecov.io/gh/hendriks73/ffsampledsp/branch/main)


# FFSampledSP

*FFSampledSP* is an implementation of the
[javax.sound.sampled](https://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/spi/package-summary.html)
service provider interfaces based on [FFmpeg](https://www.ffmpeg.org), a complete, cross-platform solution to record,
convert and stream audio and video.
FFSampledSP is part of the [SampledSP](https://www.tagtraum.com/sampledsp.html) collection of `javax.sound.sampled`
libraries.

Its main purpose is to decode audio files or streams to
[PCM](https://en.wikipedia.org/wiki/Pulse-code_modulation) — signed integer (`PCM_SIGNED`) or
floating-point (`PCM_FLOAT`).

Supported platforms are currently:

- macOS x64 (>=10.10) and aarch64 (>=11)
- Windows i686 and x64
- Linux (Ubuntu 20) x64 and aarch64 (arm64)

FFSampledSP makes use of the [tagtraum FFmpeg package](https://www.tagtraum.com/ffmpeg).

Binaries and more info can be found at its [tagtraum home](https://www.tagtraum.com/ffsampledsp/).


## Installation

FFSampledSP is released via [Maven](https://maven.apache.org).
You can install it via the following dependency:

```xml
<dependencies>
    <dependency>
        <groupId>com.tagtraum</groupId>
        <artifactId>ffsampledsp-complete</artifactId>
    </dependency>
</dependencies>
```


## Usage

To use the library, simply use
[javax.sound.sampled](https://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/spi/package-summary.html)
like you normally would.

Note that opening an `AudioInputStream` of compressed audio (e.g. mp3), does
*not* decode the stream. To obtain PCM you still have to transcode to PCM like this:

```java
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class DecodeExample {
    public static void main(final String[] args) {
        // compressed stream
        final AudioInputStream mp3In = AudioSystem.getAudioInputStream(new File(args[0]));
        // AudioFormat describing the compressed stream
        final AudioFormat mp3Format = mp3In.getFormat();
        // AudioFormat describing the desired decompressed stream 
        final AudioFormat pcmFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            mp3Format.getSampleRate(),
            16,
            mp3Format.getChannels(),
            16 * mp3Format.getChannels() / 8,
            mp3Format.getSampleRate(),
            mp3Format.isBigEndian()
            );
        // actually decompressed stream (signed PCM)
        final AudioInputStream pcmIn = AudioSystem.getAudioInputStream(pcmFormat, mp3In);
        // do something with the raw audio stream pcmIn... 
    }
}
```

To decode to 32-bit float PCM (`PCM_FLOAT`) instead, specify the float encoding and
read the samples via a `FloatBuffer`:

```java
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class FloatDecodeExample {
    public static void main(final String[] args) throws Exception {
        // compressed stream
        final AudioInputStream mp3In = AudioSystem.getAudioInputStream(new File(args[0]));
        // AudioFormat describing the compressed stream
        final AudioFormat mp3Format = mp3In.getFormat();
        // AudioFormat describing 32-bit float PCM output (little-endian, samples in [-1.0, 1.0])
        final AudioFormat pcmFloatFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_FLOAT,
            mp3Format.getSampleRate(),
            32,
            mp3Format.getChannels(),
            32 * mp3Format.getChannels() / 8,
            mp3Format.getSampleRate(),
            false  // little-endian
            );
        // decoded float PCM stream
        final AudioInputStream pcmIn = AudioSystem.getAudioInputStream(pcmFloatFormat, mp3In);
        // read and process samples as float values
        final byte[] buf = new byte[4096];
        int justRead;
        while ((justRead = pcmIn.read(buf)) != -1) {
            final FloatBuffer floats = ByteBuffer.wrap(buf, 0, justRead)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer();
            while (floats.hasRemaining()) {
                final float sample = floats.get(); // value in [-1.0, 1.0]
                // process sample...
            }
        }
    }
}
```


## Build

You can build this library locally on macOS, Windows, or Linux (Ubuntu is tested).
When doing so, only the appropriate native libraries are included in the "complete" jar.
The GitHub-based build also adds native libraries for other platforms.

To do so, you also need:

- [Maven](https://maven.apache.org/)
- For macOS: [Apple Command Line Tools](https://developer.apple.com/)
  or [XCode](https://developer.apple.com/xcode/)
- For Windows: [MSYS2](https://www.msys2.org) with GCC etc.
- a JDK (to run Maven and get the JNI headers)
- [Doxygen](https://www.doxygen.nl), available via [MacPorts](https://www.macports.org), [HomeBrew](https://brew.sh) or [MSYS2](https://www.msys2.org).

Note, that the C sources in the `ffsampledsp-x86_64-macos` module are expected to compile
on all supported platforms. In fact, the very same sources *are* compiled in the modules
for other platforms.


## Release Notes

You can find the release notes/history [here](NOTES.md).
