/*
 * =================================================
 * Copyright 2021 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.ffsampledsp;

import org.junit.Test;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import static org.junit.Assert.assertEquals;

/**
 * TestFFAudioFileFormat.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFAudioFileFormat {

    @Test
    public void testHTTPURL() throws UnsupportedAudioFileException {
        final FFAudioFileFormat fileFormat = new FFAudioFileFormat("http://www.cnn.com/some.mp3", 0, 44100, 16, 1, 16, 44100, 1, true, 5, 160, false, false);
        assertEquals(-1, fileFormat.getByteLength());
        assertEquals("mp3", fileFormat.getType().getExtension());
        assertEquals(5L, fileFormat.properties().get("duration"));
    }

    @Test(expected = UnsupportedAudioFileException.class)
    public void testHTTPURLNoFile() throws UnsupportedAudioFileException {
        new FFAudioFileFormat("http://www.cnn.com/", 0, 44100, 16, 1, 16, 44100, 1, true, 5, 160, false, false);
    }

    @Test
    public void testDetermineFrameRate() throws UnsupportedAudioFileException {
        final FFAudioFileFormat fileFormat = new FFAudioFileFormat("http://www.cnn.com/some.mp3", 0, 44100, 16, 1, 16, AudioSystem.NOT_SPECIFIED, 1, true, 5, 160, false, false);
        assertEquals((float)AudioSystem.NOT_SPECIFIED, fileFormat.getFormat().getFrameRate(), 0.01f);
    }
}
