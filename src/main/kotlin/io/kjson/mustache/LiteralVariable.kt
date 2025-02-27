/*
 * @(#) LiteralVariable.kt
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

import io.jstuff.util.IntOutput.appendInt
import io.jstuff.util.IntOutput.appendLong
import io.kstuff.util.CoIntOutput.outputInt
import io.kstuff.util.CoIntOutput.outputLong
import io.kstuff.util.CoOutput

import io.kjson.mustache.Element.Companion.outputString

/**
 * A literal Variable template element - outputs a specified value without HTML escaping.
 *
 * @author  Peter Wall
 */
class LiteralVariable(private val name: String) : Element {

    override fun appendTo(appendable: Appendable, context: Context) {
        context.resolve(name)?.let {
            when (it) {
                is Int -> appendInt(appendable, it)
                is Long -> appendLong(appendable, it)
                else -> appendable.append(it.toString())
            }
        }
    }

    override suspend fun outputTo(out: CoOutput, context: Context) {
        context.resolve(name)?.let {
            when (it) {
                is Int -> out.outputInt(it)
                is Long -> out.outputLong(it)
                else -> out.outputString(it.toString())
            }
        }
    }

}
