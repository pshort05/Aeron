/*
 * Copyright 2014 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.aeron.common;

import uk.co.real_logic.aeron.common.concurrent.AtomicBuffer;

import java.util.Arrays;

/**
 * Builder for appending buffers that grows capacity as necessary.
 */
public class BufferBuilder
{
    public static final int RESIZE_ALIGNMENT = 1024;
    private final int resizeAlignment;
    private final AtomicBuffer atomicBuffer;

    private byte[] buffer;
    private int limit = 0;
    private int capacity;

    /**
     * Construct a buffer builder with a default growth increment of {@link #RESIZE_ALIGNMENT}
     */
    public BufferBuilder()
    {
        this(RESIZE_ALIGNMENT);
    }

    /**
     * Construct a buffer builder with a given growth increment that must be a power of 2.
     *
     * @param resizeAlignment by which the buffer will grow capacity as the limit increases.
     */
    public BufferBuilder(final int resizeAlignment)
    {
        if (!BitUtil.isPowerOfTwo(resizeAlignment))
        {
            throw new IllegalArgumentException("Increment multiple must be power of two.");
        }

        this.resizeAlignment = resizeAlignment;
        capacity = resizeAlignment;
        buffer = new byte[resizeAlignment];
        atomicBuffer = new AtomicBuffer(buffer);
    }

    /**
     * The resize alignment multiple to be used for growth.
     *
     * @return resize alignment multiple to be used for growth.
     */
    public int resizeAlignment()
    {
        return resizeAlignment;
    }

    /**
     * The current capacity of the buffer.
     *
     * @return the current capacity of the buffer.
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * The current limit of the buffer that has been used by append operations.
     *
     * @return the current limit of the buffer that has been used by append operations.
     */
    public int limit()
    {
        return limit;
    }

    /**
     * The {@link AtomicBuffer} that encapsulates the internal buffer.
     *
     * @return the {@link AtomicBuffer} that encapsulates the internal buffer.
     */
    public AtomicBuffer buffer()
    {
        return atomicBuffer;
    }

    /**
     * Reset the builder to restart append operations. The internal buffer does not shrink.
     *
     * @return the builder for fluent API usage.
     */
    public BufferBuilder reset()
    {
        limit = 0;
        return this;
    }

    /**
     * Append a source buffer to the end of the internal buffer, resizing the internal buffer as required.
     *
     * @param srcBuffer from which to copy.
     * @param srcOffset in the source buffer from which to copy.
     * @param length in bytes to copy from the source buffer.
     * @return the builder for fluent API usage.
     */
    public BufferBuilder append(final AtomicBuffer srcBuffer, final int srcOffset, final int length)
    {
        ensureCapacity(length);

        srcBuffer.getBytes(srcOffset, buffer, limit, length);
        limit += length;

        return this;
    }

    private void ensureCapacity(final int additionalCapacity)
    {
        final int requiredCapacity = limit + additionalCapacity;

        if (requiredCapacity < 0)
        {
            final String s = String.format("Insufficient capacity: limit=%d additional=%d", limit, additionalCapacity);
            throw new IllegalStateException(s);
        }

        if (requiredCapacity > capacity)
        {
            final int newCapacity = BitUtil.align(requiredCapacity, resizeAlignment);
            final byte[] newBuffer = Arrays.copyOf(buffer, newCapacity);

            capacity = newCapacity;
            buffer = newBuffer;
            atomicBuffer.wrap(newBuffer);
        }
    }
}
