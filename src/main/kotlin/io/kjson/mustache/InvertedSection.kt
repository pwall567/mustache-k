/*
 * @(#) InvertedSection.kt
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

import java.math.BigDecimal
import java.math.BigInteger

import io.kjson.JSONBoolean
import io.kjson.JSONNumber
import net.pwall.util.CoOutput

/**
 * An Inverted Section element - an element that is processed only if the value is false, or empty , or zero.
 *
 * @author  Peter Wall
 */
class InvertedSection(private val name: String, children: List<Element>) : ElementWithChildren(children) {

    override fun appendTo(appendable: Appendable, context: Context) {
        when (val value = context.resolve(name)) {
            null -> appendChildren(appendable, context)
            is List<*> -> if (value.isEmpty()) appendChildren(appendable, context)
            is Array<*> -> if (value.isEmpty()) appendChildren(appendable, context)
            is Map<*, *> -> if (value.isEmpty()) appendChildren(appendable, context)
            is CharSequence -> if (value.isEmpty()) appendChildren(appendable, context)
            is Iterable<*> -> if (!value.iterator().hasNext()) appendChildren(appendable, context)
            is Sequence<*> -> if (!value.iterator().hasNext()) appendChildren(appendable, context)
            is Boolean -> if (!value) appendChildren(appendable, context)
            is Int -> if (value == 0) appendChildren(appendable, context)
            is Long -> if (value == 0L) appendChildren(appendable, context)
            is Short -> if (value == 0.toShort()) appendChildren(appendable, context)
            is Byte -> if (value == 0.toByte()) appendChildren(appendable, context)
            is UInt -> if (value == 0U) appendChildren(appendable, context)
            is ULong -> if (value == 0UL) appendChildren(appendable, context)
            is UShort -> if (value == 0.toUShort()) appendChildren(appendable, context)
            is UByte -> if (value == 0.toUByte()) appendChildren(appendable, context)
            is Double -> if (value == 0.0) appendChildren(appendable, context)
            is Float -> if (value == 0.0F) appendChildren(appendable, context)
            is BigInteger -> if (value == BigInteger.ZERO) appendChildren(appendable, context)
            is BigDecimal -> if (value.compareTo(BigDecimal.ZERO) == 0) appendChildren(appendable, context)
            is JSONNumber -> if (value.isZero()) appendChildren(appendable, context)
            is JSONBoolean -> if (!value.value) appendChildren(appendable, context)
        }
    }

    override suspend fun outputTo(out: CoOutput, context: Context) {
        when (val value = context.resolve(name)) {
            null -> outputChildren(out, context)
            is List<*> -> if (value.isEmpty()) outputChildren(out, context)
            is Array<*> -> if (value.isEmpty()) outputChildren(out, context)
            is Map<*, *> -> if (value.isEmpty()) outputChildren(out, context)
            is CharSequence -> if (value.isEmpty()) outputChildren(out, context)
            is Iterable<*> -> if (!value.iterator().hasNext()) outputChildren(out, context)
            is Sequence<*> -> if (!value.iterator().hasNext()) outputChildren(out, context)
            is Boolean -> if (!value) outputChildren(out, context)
            is Int -> if (value == 0) outputChildren(out, context)
            is Long -> if (value == 0L) outputChildren(out, context)
            is Short -> if (value == 0.toShort()) outputChildren(out, context)
            is Byte -> if (value == 0.toByte()) outputChildren(out, context)
            is UInt -> if (value == 0U) outputChildren(out, context)
            is ULong -> if (value == 0UL) outputChildren(out, context)
            is UShort -> if (value == 0.toUShort()) outputChildren(out, context)
            is UByte -> if (value == 0.toUByte()) outputChildren(out, context)
            is Double -> if (value == 0.0) outputChildren(out, context)
            is Float -> if (value == 0.0F) outputChildren(out, context)
            is BigInteger -> if (value == BigInteger.ZERO) outputChildren(out, context)
            is BigDecimal -> if (value.compareTo(BigDecimal.ZERO) == 0) outputChildren(out, context)
            is JSONNumber -> if (value.isZero()) outputChildren(out, context)
            is JSONBoolean -> if (!value.value) outputChildren(out, context)
        }
    }

}
