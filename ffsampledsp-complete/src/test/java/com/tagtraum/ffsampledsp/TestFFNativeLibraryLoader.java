/*
 * =================================================
 * Copyright 2021 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.ffsampledsp;

import org.junit.Test;

import java.io.*;

import static com.tagtraum.ffsampledsp.FFNativeLibraryLoader.decodeURL;
import static org.junit.Assert.assertEquals;

/**
 * TestFFNativeLibraryLoader.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFNativeLibraryLoader {

    @Test(expected = FileNotFoundException.class)
    public void testFindNonExistingFile() throws FileNotFoundException {
        FFNativeLibraryLoader.findFile("testFindFile", FFNativeLibraryLoader.class, new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return false;
            }
        });
    }

    @Test
    public void testDecodeURL() throws UnsupportedEncodingException {
        assertEquals("someString", decodeURL("someString"));
        assertEquals("someString some", decodeURL("someString%20some"));
        assertEquals("  ", decodeURL("%20%20"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeURLIncompleteTrailingEscapePattern() throws UnsupportedEncodingException {
        decodeURL("someString%h");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeURLIllegalHex() throws UnsupportedEncodingException {
        decodeURL("someString%ah");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeURLNegativeValue() throws UnsupportedEncodingException {
        decodeURL("someString%-1");
    }
}
