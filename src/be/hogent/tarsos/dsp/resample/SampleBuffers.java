/******************************************************************************
 *
 * libresample4j
 * Copyright (c) 2009 Laszlo Systems, Inc. All Rights Reserved.
 *
 * libresample4j is a Java port of Dominic Mazzoni's libresample 0.1.3,
 * which is in turn based on Julius Smith's Resample 1.7 library.
 *      http://www-ccrma.stanford.edu/~jos/resample/
 *
 * License: LGPL -- see the file LICENSE.txt for more information
 *
 *****************************************************************************/
package be.hogent.tarsos.dsp.resample;

/**
 * Callback for producing and consuming samples. Enables on-the-fly conversion between sample types
 * (signed 16-bit integers to floats, for example) and/or writing directly to an output stream.
 */
interface SampleBuffers {
    /**
     * @return number of input samples available
     */

    int getInputBufferLength();

    /**
     * @return number of samples the output buffer has room for
     */
    int getOutputBufferLength();

    /**
     * Copy <code>length</code> samples from the input buffer to the given array, starting at the given offset.
     * Samples should be in the range -1.0f to 1.0f.
     *
     * @param array  array to hold samples from the input buffer
     * @param offset start writing samples here
     * @param length write this many samples
     */
    void produceInput(float[] array, int offset, int length);

    /**
     * Copy <code>length</code> samples from the given array to the output buffer, starting at the given offset.
     *
     * @param array  array to read from
     * @param offset start reading samples here
     * @param length read this many samples
     */
    void consumeOutput(float[] array, int offset, int length);
}
