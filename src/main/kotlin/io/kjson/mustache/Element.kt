/*
 * @(#) Element.kt
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

import io.kstuff.util.CoOutput
import io.kstuff.util.output

/**
 * A Mustache template element (a Variable, Section _etc._)
 *
 * @author  Peter Wall
 */
sealed interface Element {

    /**
     * Append the result of this element (when evaluated against a specified [Context]) to an [Appendable].
     */
    fun appendTo(appendable: Appendable, context: Context)

    /**
     * Output the result of this element (when evaluated against a specified [Context]) to a non-blocking output
     * function.
     */
    suspend fun outputTo(out: CoOutput, context: Context)

    companion object {

        suspend fun CoOutput.outputString(string: String) {
            for (ch in string)
                output(ch)
        }

    }

}
