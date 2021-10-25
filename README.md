# FFSampledSP

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.tagtraum/ffsampledsp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.tagtraum/ffsampledsp)

*FFSampledSP* is an implementation of the
[javax.sound.sampled](http://docs.oracle.com/javase/10/docs/api/javax/sound/sampled/spi/package-summary.html)
service provider interfaces based on [FFmpeg](https://www.ffmpeg.org), a complete, cross-platform solution to record,
convert and stream audio and video.
FFSampledSP is part of the [SampledSP](https://www.tagtraum.com/sampledsp.html) collection of `javax.sound.sampled`
libraries.

Its main purpose is to decode audio files or streams to signed linear pcm.

FFSampledSP makes use of the ["tagtraum FFmpeg package"](https://www.tagtraum.com/ffmpeg).

Binaries and more info can be found at its [tagtraum home](https://www.tagtraum.com/ffsampledsp/).


## Build

Currently you can only build this library on macOS.

To do so, you also need:

- Maven 3.0.5, http://maven.apache.org/
- a MinGW-w64 crosscompiler, http://mingw-w64.sourceforge.net, e.g. via [MacPorts](http://mingw-w64.org/doku.php/download/macports)
- Apple Command Line Tools, available via https://developer.apple.com/,
  or XCode, https://developer.apple.com/xcode/
- the Windows JNI header files
- a JDK (to run Maven and get the macOS JNI headers)
- [Doxygen](http://www.doxygen.org), available via [MacPorts](https://www.macports.org) or [HomeBrew](https://brew.sh)

Once you have all this, you need to adjust some properties in the parent `pom.xml`.
Or.. simply override them using `-Dname=value` notation. E.g. to point to your
Windows JNI headers, add

    -Dmingw.headers.jni=/mywindowsjdk/include

to your mvn call. If you didn't add the `bin` folder of your crosscompiler to the
`PATH`, you might also want to set `-Dmingw.i386.path=...` and `-Dmingw.x86_64.path=...`
You might also need to change `mmacosx-version-min` and `isysroot`, if you
don't have an OS X 10.11 SDK installed.

So all in all, something like the following might work for you, depending on where
you installed the Windows JNI headers, MinGW-w64, and the macOS JDK:

    mvn -Ddarwin.headers.jni=/Library/Java/JavaVirtualMachines/jdk1.8.0_66.jdk/Contents/Home/include/ \
        -Dmingw.headers.jni=/Users/YOUR_ID/mywindowsjdk/include \
        -Dmingw.i386.path=/Users/YOUR_ID/mingw/mingw-w32-i686/bin \
        -Dmingw.x86_64.path=/Users/YOUR_ID/mingw/mingw-w32-i686/bin \
        -Dmmacosx-version-min=10.10 \
        -Disysroot=/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.7.sdk/ \
        clean install

Note, that the C sources in the ffsampledsp-x86_64-macos module are expected to compile
on all supported platforms. In fact, the very same sources *are* compiled in the modules
for other platforms.


## Warranty

This library comes with absolutely no support, warranty etc. you name it.
Please see LICENSE.txt for licensing details.


Enjoy
