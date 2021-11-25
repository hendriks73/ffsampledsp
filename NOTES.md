- 0.9.38
  - Added automatic deployment to tagtraum site
  - Fixed native library loading issues
  - Improved test coverage


- 0.9.35 - 0.9.37
  - Battling deployment issues 

 
- 0.9.34
  - Added support for macOS aarch64 (arm64) 
  - Added support for Linux x86_64
  - Moved to FFmpeg 4.0.3


- 0.9.33
  - Change AudioFileFormat.getFrameLength() so that it reflects the correct number
    of frames even for compressed formats.
  - Updated some doc links to use HTTPS rather than HTTP
  - Moved to profile-based builds
  - Build/deployment via GitHub actions


- 0.9.32
  - Detect and report 'drms'-files.
  - Added channel count sanity check.
  - Expose "encrypted" flag for DRM-crippled files in AudioFormat properties.


- 0.9.31
  - Fix for crash on copying from empty buffer (e.g. for WMA).


- 0.9.30
  - Fixed some docs and links.
  - Log individual packet decode errors.


- 0.9.29
  - Fixed some javadocs.
  - Fixed decoding of ADPCM.
  - Exclude snapshot releases from tmp dir caching.
  - Dropped support for 32 bit on macOS.


- 0.9.28
  - Attached sources and javadocs to complete module.


- 0.9.27
  - Deployment to maven central.
  - Fixed path to project.properties.
  - Fixed embedded library loading.
  - Removed unnecessary dependency on -java module.


- 0.9.26
  - Switch to JNI header generation via javac -h instead of javah.
  - Moved to FFmpeg 4.0.
  - Changed naming scheme.
  - Now requires Java 7.
  - Embedded dylibs/dlls into ffsampledsp-complete artifact.


- 0.9.25
  - Moved to tagtraum FFmpeg package 1.13.1 (fix for dither bug).


- 0.9.24
  - Moved to FFmpeg 3.4.2.
  - Updated docs a little.


- 0.9.23
  - Moved to FFmpeg 3.4.1.
  - Added support for mp3.


- 0.9.22
  - Ensure that we can still read the whole file after seeking.


- 0.9.21
  - Ensure that we can still read the whole file after seeking.


- 0.9.20
  - Removed compile time dependency to libswresample/swresample_internal.h.
  - Updated header paths for macOS 10.12.
  - Set library prefix and suffix automatically, depending on platform (thanks Jonas Hartwig).


- 0.9.19
  - Moved to FFmpeg 3.3.1


- 0.9.18
  - Added additional plausibility check for detected audio formats.


- 0.9.17
  - Fixed bad sample size in AudioFormat for m4a.


- 0.9.16
  - Moved to FFmpeg 3.2


- 0.9.15
  - Fixed SSL related Maven deployment issue with Wagon.
  - Fixed site deployment issues.
  - Ensure that stream is still open when seeking.


- 0.9.14
  - Fixed potential ArrayIndexOutOfBoundsException in FFAudioFileReader.getAudioFileFormat().


- 0.9.13
  - Fixed library loading issues when the classpath contains a + char.


- 0.9.12
  - Fixed low probe score test.


- 0.9.11
  - Added (intptr_t) casts to avoid warnings
  - Removed references to deprecated FFmpeg APIs
  - Removed useless -s as Clang parameter
  - Fixed Java8-related Javadoc problems
  - Specified UTF-8 as source encoding
  - Moved to FFmpeg 2.7.2


- 0.9.10
  - Fixed direct links to native libs on site
  - Fixed direct links to release repo on site
  - Moved to .dylib instead of .jnilib.
  - Improved HTTP error reporting
  - Moved to FFmpeg 2.6.1


- 0.9.9
  - Fixed tagtraum banner size in sub-modules
  - Made sure DRM protected m4p files are not decoded


- 0.9.8
  - Moved to FFmpeg 2.4
  - Changed Maven skin to Fluido
  - Added GitHub ribbon


- 0.9.7
  - Moved to FFmpeg 2.3.3


- 0.9.6
  - Moved to FFmpeg 2.3
  - Fixed handling of UNC paths on Windows


- 0.9.5
  - Moved to FFmpeg 2.2.4


- 0.9.4
  - Enabled Apple Lossless (alac) test cases
  - Require a minimum probe score to avoid misdetection


- 0.9.3
  - Moved to FFmpeg 2.2.1


- 0.9.2
  - Moved to FFmpeg 2.1


- 0.9.1
  - Improved documentation


- 0.9.0
  - First release
