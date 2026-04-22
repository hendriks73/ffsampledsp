/*
 * =================================================
 * Copyright 2021 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.ffsampledsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;
import org.junit.Test;

/**
 * TestFFAudioFormat.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestFFAudioFormat {

  @Test
  public void testSupportedEncodings() {
    final Set<FFAudioFormat.FFEncoding> supportedEncodings =
        FFAudioFormat.FFEncoding.getSupportedEncodings();
    assertNotNull(supportedEncodings);
  }

  @Test
  public void testGetCodec() {
    final FFAudioFormat.FFEncoding.Codec codec =
        FFAudioFormat.FFEncoding.getCodec(0x10000); // AV_CODEC_ID_PCM_S16LE
    assertEquals("PCM_SIGNED", codec.getName());
  }
}
