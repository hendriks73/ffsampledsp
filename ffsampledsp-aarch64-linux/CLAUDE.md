# ffsampledsp-aarch64-linux

Native library module for Linux aarch64 (arm64). Packages `ffsampledsp-aarch64-linux.so`.

**No local C sources.** The compiler is pointed at `../ffsampledsp-x86_64-macos/src/main/c/` — edit C code there.

## Build

```bash
# Cross-compile from x86_64 Linux (requires aarch64-linux-gnu-gcc):
mvn --activate-profiles ffsampledsp-aarch64-linux install

# Debug build:
mvn --activate-profiles ffsampledsp-aarch64-linux install -Dcflags=-DDEBUG
```

Tests are skipped for this profile (no native runner available in CI).
