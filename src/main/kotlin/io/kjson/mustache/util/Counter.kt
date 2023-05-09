/*
 * @(#) Counter.java
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2023 Peter Wall
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

package io.kjson.mustache.util

/**
 * A class to maintain a counter suitable for output using Mustache.
 *
 * @author  Peter Wall
 */
class Counter(
    var value: Int = 0,
    val units: String? = null,
    val plural: String? = null,
) {

    fun clear() {
        value = 0
    }

    fun increment() {
        value++
    }

    fun incrementBy(number: Int) {
        value += number
    }

    override fun toString(): String = if (units == null) value.toString() else buildString {
        append(value)
        append(' ')
        when {
            value == 1 -> append(units)
            plural == null -> {
                append(units)
                append('s')
            }
            else -> append(plural)
        }
    }

}
