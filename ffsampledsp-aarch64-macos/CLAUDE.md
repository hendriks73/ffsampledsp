# ffsampledsp-aarch64-macos

Native library module for macOS aarch64 (Apple Silicon). Packages `ffsampledsp-aarch64-macos.dylib`.

**No local C sources.** The compiler is pointed at `../ffsampledsp-x86_64-macos/src/main/c/` — edit C code there.

## Build

```bash
mvn --activate-profiles ffsampledsp-aarch64-macos install

# Debug build:
mvn --activate-profiles ffsampledsp-aarch64-macos install -Dcflags=-DDEBUG
```

Requires Apple Command Line Tools or Xcode on Apple Silicon (or cross-compilation from x86_64).
