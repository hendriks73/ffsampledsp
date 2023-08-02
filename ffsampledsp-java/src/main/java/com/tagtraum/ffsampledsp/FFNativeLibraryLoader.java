/*
 * =================================================
 * Copyright 2007 tagtraum industries incorporated
 * This file is part of FFSampledSP.
 *
 * FFSampledSP is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFSampledSP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFSampledSP; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * =================================================
 */
package com.tagtraum.ffsampledsp;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * First tries to load a library the default way using {@link System#loadLibrary(String)},
 * upon failure falls back to the base directory of the given class package or the jar the class
 * is in. This way, a native library is found, if it is located in the same directory as a particular jar, identified
 * by a specific class from that jar.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public final class FFNativeLibraryLoader {

    private static final Logger LOG = Logger.getLogger(FFNativeLibraryLoader.class.getName());
    private static final String JAR_PROTOCOL = "jar";
    private static final String FILE_PROTOCOL = "file";
    private static final String CLASS_FILE_EXTENSION = ".class";
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String HOST = OS_NAME.contains("mac")
            ? "macos" : (OS_NAME.contains("win") ? "win" : "linux");
    private static final String ARCH = arch();
    private static final String VERSION = readProjectVersion();

    private static final Set<String> LOADED = new HashSet<>();

    private static Boolean ffSampledSPLibraryLoaded;

    private static final String NATIVE_LIBRARY_PREFIX;
    private static final String NATIVE_LIBRARY_EXTENSION;
    static {
        final String systemLibraryName = System.mapLibraryName("");
        final int dot = systemLibraryName.lastIndexOf('.');

        NATIVE_LIBRARY_EXTENSION = systemLibraryName.substring(dot + 1);
        NATIVE_LIBRARY_PREFIX = systemLibraryName.substring(0, dot);

        LOG.fine("NATIVE_LIBRARY_EXTENSION: " + NATIVE_LIBRARY_EXTENSION);
        LOG.fine("NATIVE_LIBRARY_PREFIX: " + NATIVE_LIBRARY_PREFIX);
    }

    private FFNativeLibraryLoader() {
    }

    /**
     * Loads the FFSampledSP library.
     *
     * @return true, if loading was successful
     */
    public static synchronized boolean loadLibrary() {
        if (ffSampledSPLibraryLoaded != null) {
            return ffSampledSPLibraryLoaded;
        }
        boolean loaded = false;
        try {
            FFNativeLibraryLoader.loadLibrary("ffsampledsp");
            loaded = true;
        } catch (Error e) {
            LOG.severe("Failed to load native library 'ffsampledsp'. Please check your library path. FFSampledSP will be dysfunctional.");
        }
        ffSampledSPLibraryLoaded = loaded;
        return ffSampledSPLibraryLoaded;
    }

    /**
     * Loads a library.
     *
     * @param libName name of the library, as described in {@link System#loadLibrary(String)} );
     */
    public static synchronized void loadLibrary(final String libName) {
        loadLibrary(libName, FFNativeLibraryLoader.class);
    }

    /**
     * Loads a library.
     * First tries {@code libname-arch-host}, via the library path,
     * then {@code (lib)libname-arch-*.ext} via the classpath,
     * then {@code libname} via the library path,
     * and finally {@code (lib)libname*.ext} via the classpath.
     *
     * @param libName name of the library, as described in {@link System#loadLibrary(String)} );
     * @param baseClass class that identifies the jar
     */
    public static synchronized void loadLibrary(final String libName, final Class<?> baseClass) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("loadLibrary(\"" + libName + "\", " + baseClass + ")");
        }
        final String key = libName + "|" + baseClass.getName();
        if (LOADED.contains(key)) {
            return;
        }
        // in the jar, we already know the version, so no need there...
        final String packagedNativeLib = libName + "-" + ARCH + "-" + HOST + "." + NATIVE_LIBRARY_EXTENSION;
        // but extracted, we want to keep things separate
        final String extractedNativeLibFilename = libName + "-" + ARCH + "-" + HOST + "-" + VERSION + "." + NATIVE_LIBRARY_EXTENSION;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("packagedNativeLib: " + packagedNativeLib);
        }
        final File extractedNativeLib = new File(System.getProperty("java.io.tmpdir") + "/" + extractedNativeLibFilename);
        if (!extractedNativeLib.exists() || extractedNativeLib.toString().contains("SNAPSHOT")) {
            extractResourceToFile(baseClass, "/" + packagedNativeLib, extractedNativeLib);
        }
        if (extractedNativeLib.exists()) {
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Trying Runtime.getRuntime().load(\"" + extractedNativeLib + "\")");
                }
                Runtime.getRuntime().load(extractedNativeLib.toString());
                LOADED.add(key);
                return;
            } catch (Error e) {
                // failed to extract and load, will try other ways
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Failed to load " + extractedNativeLib + " (will try other ways): " + e);
                }
            }
        }

        try {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Trying System.loadLibrary(\"" + libName + "-" + ARCH + "-" + HOST + "\")");
            }
            System.loadLibrary(libName + "-" + ARCH + "-" + HOST);
            LOADED.add(key);
        } catch (Error e) {
            try {
                final String libFilename = findFile(libName, baseClass, new LibFileFilter(libName, ARCH));
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Trying Runtime.getRuntime().load(\"" + libFilename + "\")");
                }
                Runtime.getRuntime().load(libFilename);
                LOADED.add(key);
            } catch (FileNotFoundException e1) {
                try {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Trying System.loadLibrary(\"" + libName + "\")");
                    }
                    System.loadLibrary(libName);
                    LOADED.add(key);
                } catch (Error e0) {
                    try {
                        final String libFilename = findFile(libName, baseClass, new LibFileFilter(libName));
                        LOG.fine("Trying Runtime.getRuntime().load(\"" + libFilename + "\")");
                        Runtime.getRuntime().load(libFilename);
                        LOADED.add(key);
                    } catch (FileNotFoundException e2) {
                        throw e;
                    }
                }
            }
        }
        LOG.fine("Successfully loaded " + libName);
    }

    /**
     * Extracts the given resource and writes it to the specified file.
     * Note that this method fails silently.
     *
     * @param baseClass class to use as base class for the resource lookup
     * @param sourceResource resource name
     * @param targetFile target file
     */
    private static void extractResourceToFile(final Class<?> baseClass, final String sourceResource, final File targetFile) {
        try (final InputStream in = baseClass.getResourceAsStream(sourceResource)) {
            if (in != null) {
                try (final OutputStream out = new FileOutputStream(targetFile)) {
                    final byte[] buf = new byte[1024 * 8];
                    int justRead;
                    while ((justRead = in.read(buf)) != -1) {
                        out.write(buf, 0, justRead);
                    }
                }
                if (LOG.isLoggable(Level.FINE)) LOG.fine("Created " + targetFile + " from resource " + sourceResource);
            } else {
                LOG.warning("Failed to find resource " + sourceResource);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                "Failed to extract native lib using base class " + baseClass +
                    " and resource name " + sourceResource + " to file " + targetFile, e);
        }
    }

    /**
     * Finds a file that is either in the classpath or in the same directory as a given class's jar.
     *
     * @param name (partial) filename
     * @param baseClass base class
     * @param filter filter that determines whether a file is a match
     * @return file
     * @throws FileNotFoundException if a matching file cannot be found
     */
    public static String findFile(final String name, final Class<?> baseClass, final FileFilter filter)
            throws FileNotFoundException {
        try {
            final String filename;
            final String fullyQualifiedBaseClassName = baseClass.getName();
            final String baseClassName = fullyQualifiedBaseClassName.substring(fullyQualifiedBaseClassName.lastIndexOf('.') + 1);
            final URL url = baseClass.getResource(baseClassName + CLASS_FILE_EXTENSION);
            if (url == null) {
                throw new FileNotFoundException("Failed to get URL of " + fullyQualifiedBaseClassName);
            } else {
                File directory;
                final String path = decodeURL(url.getPath());
                if (JAR_PROTOCOL.equals(url.getProtocol())) {
                    final String jarFileName = new URL(path.substring(0, path.lastIndexOf('!'))).getPath();
                    directory = new File(jarFileName).getParentFile();
                } else if (FILE_PROTOCOL.equals(url.getProtocol())) {
                    directory = new File(path.substring(0, path.length()
                            - fullyQualifiedBaseClassName.length() - CLASS_FILE_EXTENSION.length()));
                } else {
                    throw new FileNotFoundException("Base class was not loaded via jar: or file: protocol.");
                }
                final File[] libs = directory.listFiles(filter);
                if (libs == null || libs.length == 0) {
                    throw new FileNotFoundException("No matching files in " + directory);
                }
                filename = libs[0].toString();
            }
            return filename;
        } catch (MalformedURLException e) {
            final FileNotFoundException fnfe = new FileNotFoundException(name + ": " + e);
            fnfe.initCause(e);
            throw fnfe;
        }
    }

    private static class LibFileFilter implements FileFilter {
        private final String libName;
        private final String arch;

        public LibFileFilter(final String libName) {
            this(libName, null);
        }

        public LibFileFilter(final String libName, final String arch) {
            this.libName = libName;
            this.arch = arch == null ? "" : "-" + arch;
        }

        public boolean accept(final File file) {
            final String fileString = file.toString();
            final String fileName = file.getName();

            return file.isFile()
                    && (fileName.startsWith(libName + arch) || fileName.startsWith(NATIVE_LIBRARY_PREFIX + libName + arch))
                    && fileString.endsWith(NATIVE_LIBRARY_EXTENSION);
        }
    }

    /**
     * Effective architecture name. I.e. either {@code i386} or {@code x86_64}.
     *
     * @return {@code "i386"} or {@code "x86_64"}
     */
    private static String arch() {
        final String arch = System.getProperty("os.arch");
        final boolean x84_64 = "x86_64".equals(arch) || "amd64".equals(arch);
        final boolean i386 = "x86".equals(arch) || "i386".equals(arch) || "i486".equals(arch) || "i586".equals(arch) || "i686".equals(arch);
        final boolean aarch64 = "aarch64".equals(arch) || "arm64".equals(arch);

        final String resultingArch = x84_64
            ? "x86_64"
            : (i386 ? "i386" : (aarch64 ? "aarch64" : arch));
        if (LOG.isLoggable(Level.INFO)) LOG.info("Using arch=" + resultingArch);
        return resultingArch;
    }

    /**
     * Decode % encodings in URLs.
     * The common {@link java.net.URLDecoder#decode(String, String)} method also converts {@code +} to {@code space},
     * which is not what we want.
     *
     * @param s url
     * @return decoded URL
     */
    static String decodeURL(final String s) {
        boolean needToChange = false;
        final int numChars = s.length();
        final StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);

            if (c == '%') {
                /*
                 * Starting with this instance of %, process all
                 * consecutive substrings of the form %xy. Each
                 * substring %xy will yield a byte. Convert all
                 * consecutive  bytes obtained this way to whatever
                 * character(s) they represent in the provided
                 * encoding.
                 */

                try {

                    // (numChars-i)/3 is an upper bound for the number
                    // of remaining bytes
                    if (bytes == null) {
                        bytes = new byte[(numChars - i) / 3];
                    }
                    int pos = 0;

                    while (((i+2) < numChars) && (c=='%')) {
                        int v = Integer.parseInt(s.substring(i+1,i+3),16);
                        if (v < 0) {
                            throw new IllegalArgumentException("FFNativeLibraryLoader: Illegal hex characters in escape (%) pattern - negative value");
                        }
                        bytes[pos++] = (byte) v;
                        i+= 3;
                        if (i < numChars) {
                            c = s.charAt(i);
                        }
                    }

                    // A trailing, incomplete byte encoding such as
                    // "%x" will cause an exception to be thrown
                    if ((i < numChars) && (c=='%')) {
                        throw new IllegalArgumentException("FFNativeLibraryLoader: Incomplete trailing escape (%) pattern");
                    }

                    sb.append(new String(bytes, 0, pos, UTF_8));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("FFNativeLibraryLoader: Illegal hex characters in escape (%) pattern - " + e.getMessage());
                }
                needToChange = true;
            } else {
                sb.append(c);
                i++;
            }
        }
        return needToChange ? sb.toString() : s;
    }

    /**
     * Read project version, injected by Maven.
     *
     * @return project version or <code>unknown</code>, if not found.
     */
    private static String readProjectVersion() {
        try {
            final Properties properties = new Properties();
            properties.load(FFNativeLibraryLoader.class.getResourceAsStream("project.properties"));
            return properties.getProperty("version", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

}
