/*
 * @(#) Variable.kt
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

import net.pwall.pipeline.AbstractIntCoAcceptor
import net.pwall.pipeline.AppendableAcceptor
import net.pwall.pipeline.accept
import net.pwall.pipeline.codec.CoCodePoint_UTF16
import net.pwall.pipeline.codec.CodePoint_UTF16
import net.pwall.pipeline.html.HTMLCoEncoder
import net.pwall.pipeline.html.HTMLEncoder
import net.pwall.util.CoOutput
import net.pwall.util.output

/**
 * A Variable template element - outputs a specified value escaped for HTML.
 *
 * @author  Peter Wall
 */
class Variable(private val name: String) : Element {

    override fun appendTo(appendable: Appendable, context: Context) {
        context.resolve(name)?.let {
            HTMLEncoder<Unit>(CodePoint_UTF16(AppendableAcceptor(appendable))).accept(it.toString())
        }
    }

    override suspend fun outputTo(out: CoOutput, context: Context) {
        context.resolve(name)?.let {
            HTMLCoEncoder<Unit>(CoCodePoint_UTF16(OutputAcceptor(out))).accept(it.toString())
        }
    }

    class OutputAcceptor<R>(val out: CoOutput) : AbstractIntCoAcceptor<R>() {
        override suspend fun acceptInt(value: Int) {
            out.output(value.toChar())
        }
    }

}
