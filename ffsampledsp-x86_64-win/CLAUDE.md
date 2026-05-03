# ffsampledsp-x86_64-win

Native library module for Windows x86_64. Packages `ffsampledsp-x86_64-win.dll`.

**No local C sources.** The compiler is pointed at `../ffsampledsp-x86_64-macos/src/main/c/` — edit C code there.

## Build

```bash
# Requires MSYS2 with MinGW-w64 GCC toolchain:
mvn --activate-profiles ffsampledsp-x86_64-win install

# Debug build:
mvn --activate-profiles ffsampledsp-x86_64-win install -Dcflags=-DDEBUG
```

Windows URL handling note: `file:` URLs must follow the libav style (`file:C:/path/file`, not `file:///C:/path/file`). UNC paths use `file://server/path`. This conversion is done in `FFAudioFileReader.urlToString()`.
