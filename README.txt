README.txt
==========

FFSampledSP is an implementation of the javax.sound.sampled service provider interfaces
based on FFmpeg (http://www.ffmpeg.org), a complete, cross-platform solution to record,
convert and stream audio and video.

Its main purpose is to decode audio files or streams to signed linear pcm.

FFSampledSP makes use of the "tagtraum FFmpeg package", http://www.tagtraum.com/ffmpeg


Warranty
========

This library comes with absolutely no support, warranty etc. you name it.
Please see LICENSE.txt for licensing details.


Build
=====

Currently you can only build this library on OS X.

To do so, you also need:
- Maven 3.0.5, http://maven.apache.org/
- a MinGW-w64 crosscompiler, http://mingw-w64.sourceforge.net
- Apple Command Line Tools, available via https://developer.apple.com/,
  or XCode, https://developer.apple.com/xcode/
- the Windows JNI header files
- a JDK (to run Maven and get the OSX JNI headers)
- Doxygen, available via MacPorts

Once you have all this, you need to adjust some properties in the parent pom.xml.
Or.. simply override them using -Dname=value notation. E.g. to point to your
Windows JNI headers, add

-Dmingw.headers.jni=/mywindowsjdk/include

to your mvn call. If you didn't add the bin folder of your crosscompiler to the
PATH, you might also want to set -Dmingw.i386.path=... and -Dmingw.x86_64.path=...

So all in all, something like the following might work for you, depending on where
you installed the Windows JNI headers, MinGW-w64, and the OS X JDK:

mvn -Ddarwin.headers.jni=/Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/include/ \
    -Dmingw.headers.jni=/Users/YOUR_ID/mywindowsjdk/include \
    -Dmingw.i386.path=/Users/YOUR_ID/mingw/mingw-w32-i686/bin \
    -Dmingw.x86_64.path=/Users/YOUR_ID/mingw/mingw-w32-i686/bin \
    clean install

Note, that the C sources in ffsampledsp-x86_64-darwin are expected to compile on
all supported platforms. In fact, the very same sources *are* compiled in the modules
for other platforms.


Enjoy,

-hendrik
hs@tagtraum.com
