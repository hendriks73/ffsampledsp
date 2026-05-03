# ffsampledsp-i386-win

Native library module for Windows i386 (32-bit). Packages `ffsampledsp-i386-win.dll`.

**No local C sources.** The compiler is pointed at `../ffsampledsp-x86_64-macos/src/main/c/` — edit C code there.

## Build

```bash
# Requires MSYS2 with MinGW 32-bit GCC toolchain:
mvn --activate-profiles ffsampledsp-i386-win install
```

Tests are skipped for this profile.
