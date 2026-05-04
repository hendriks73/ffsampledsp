# ffsampledsp-x86_64-linux

Native library module for Linux x86_64. Packages `ffsampledsp-x86_64-linux.so`.

**No local C sources.** The compiler is pointed at `../ffsampledsp-x86_64-macos/src/main/c/` — edit C code there.

## Build

```bash
mvn --activate-profiles ffsampledsp-x86_64-linux install

# Debug build:
mvn --activate-profiles ffsampledsp-x86_64-linux install -Dcflags=-DDEBUG
```

Requires GCC. Tested on Ubuntu 20+.
