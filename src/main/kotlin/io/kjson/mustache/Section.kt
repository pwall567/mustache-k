/*
 * @(#) Section.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2020, 2021, 2023 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson.mustache

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

import java.math.BigDecimal
import java.math.BigInteger

import io.kjson.JSONBoolean
import io.kjson.JSONNumber
import io.kstuff.util.CoOutput
import io.kstuff.util.CoOutputFlushable

/**
 * A  Section element - an element that is processed conditionally depending on the contents of the value.
 *
 * @author  Peter Wall
 */
class Section(private val name: String, children: List<Element>) : ElementWithChildren(children)  {

    override fun appendTo(appendable: Appendable, context: Context) {
        when (val value = context.resolve(name)) {
            null -> {}
            is Iterable<*> -> iterate(appendable, context, value.iterator())
            is Sequence<*> -> iterate(appendable, context, value.iterator())
            is Array<*> -> iterate(appendable, context, value.iterator())
            is Map<*, *> -> iterate(appendable, context, value.entries.iterator())
            is CharSequence -> if (value.isNotEmpty()) appendChildren(appendable, context.child(value))
            is Boolean -> if (value) appendChildren(appendable, context.child(value))
            is Int -> if (value != 0) appendChildren(appendable, context.child(value))
            is Long -> if (value != 0L) appendChildren(appendable, context.child(value))
            is Short -> if (value != 0.toShort()) appendChildren(appendable, context.child(value))
            is Byte -> if (value != 0.toByte()) appendChildren(appendable, context.child(value))
            is UInt -> if (value != 0U) appendChildren(appendable, context.child(value))
            is ULong -> if (value != 0UL) appendChildren(appendable, context.child(value))
            is UShort -> if (value != 0.toUShort()) appendChildren(appendable, context.child(value))
            is UByte -> if (value != 0.toUByte()) appendChildren(appendable, context.child(value))
            is Double -> if (value != 0.0) appendChildren(appendable, context.child(value))
            is Float -> if (value != 0.0F) appendChildren(appendable, context.child(value))
            is BigInteger -> if (value != BigInteger.ZERO) appendChildren(appendable, context.child(value))
            is BigDecimal -> if (value.compareTo(BigDecimal.ZERO) != 0) appendChildren(appendable, context.child(value))
            is JSONNumber -> if (value.isNotZero()) appendChildren(appendable, context.child(value))
            is JSONBoolean -> if (value.value) appendChildren(appendable, context.child(value))
            is Enum<*> -> appendChildren(appendable, context.enumChild(value))
            else -> { // any other types? callable?
                appendChildren(appendable, context.child(value))
            }
        }
    }

    override suspend fun outputTo(out: CoOutput, context: Context) {
        when (val value = context.resolve(name)) {
            null -> {}
            is Flow<*> -> iterateOverFlow(out, context, value)
            is Channel<*> -> iterateOverChannel(out, context, value)
            is Iterable<*> -> iterate(out, context, value.iterator())
            is Sequence<*> -> iterate(out, context, value.iterator())
            is Array<*> -> iterate(out, context, value.iterator())
            is Map<*, *> -> iterate(out, context, value.entries.iterator())
            is CharSequence -> if (value.isNotEmpty()) outputChildren(out, context.child(value))
            is Boolean -> if (value) outputChildren(out, context.child(value))
            is Int -> if (value != 0) outputChildren(out, context.child(value))
            is Long -> if (value != 0L) outputChildren(out, context.child(value))
            is Short -> if (value != 0.toShort()) outputChildren(out, context.child(value))
            is Byte -> if (value != 0.toByte()) outputChildren(out, context.child(value))
            is UInt -> if (value != 0U) outputChildren(out, context.child(value))
            is ULong -> if (value != 0UL) outputChildren(out, context.child(value))
            is UShort -> if (value != 0.toUShort()) outputChildren(out, context.child(value))
            is UByte -> if (value != 0.toUByte()) outputChildren(out, context.child(value))
            is Double -> if (value != 0.0) outputChildren(out, context.child(value))
            is Float -> if (value != 0.0F) outputChildren(out, context.child(value))
            is BigInteger -> if (value != BigInteger.ZERO) outputChildren(out, context.child(value))
            is BigDecimal -> if (value.compareTo(BigDecimal.ZERO) != 0) outputChildren(out, context.child(value))
            is JSONNumber -> if (value.isNotZero()) outputChildren(out, context.child(value))
            is JSONBoolean -> if (value.value) outputChildren(out, context.child(value))
            is Enum<*> -> outputChildren(out, context.enumChild(value))
            else -> { // any other types? callable?
                outputChildren(out, context.child(value))
            }
        }
    }

    private fun iterate(appendable: Appendable, context: Context, iterator: Iterator<*>) {
        var index = 0
        while (iterator.hasNext()) {
            val item = iterator.next()
            val iteratorContext = context.iteratorChild(item, index == 0, !iterator.hasNext(), index, index + 1)
            appendChildren(appendable, iteratorContext)
            index++
        }
    }

    private suspend fun iterate(out: CoOutput, context: Context, iterator: Iterator<*>) {
        var index = 0
        while (iterator.hasNext()) {
            val item = iterator.next()
            val iteratorContext = context.iteratorChild(item, index == 0, !iterator.hasNext(), index, index + 1)
            outputChildren(out, iteratorContext)
            index++
        }
    }

    private suspend fun iterateOverFlow(out: CoOutput, context: Context, flow: Flow<*>) {
        var index = 0
        if (out is CoOutputFlushable)
            out.flush()
        flow.collect {
            val flowContext = context.iteratorChild(it, index == 0, false, index, index + 1)
            outputChildren(out, flowContext)
            if (out is CoOutputFlushable)
                out.flush()
            index++
        }
    }

    private suspend fun iterateOverChannel(out: CoOutput, context: Context, channel: Channel<*>) {
        val iterator = channel.iterator()
        var index = 0
        if (out is CoOutputFlushable)
            out.flush()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val channelContext = context.iteratorChild(item, index == 0, false, index, index + 1)
            outputChildren(out, channelContext)
            if (out is CoOutputFlushable)
                out.flush()
            index++
        }
    }

}
