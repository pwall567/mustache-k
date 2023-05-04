/*
 * @(#) Template.kt
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

import java.io.File
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset

import io.kjson.mustache.parser.Parser
import net.pwall.util.CoOutput

/**
 * A Mustache template.
 *
 * @author  Peter Wall
 */
class Template internal constructor(children: List<Element>): ElementWithChildren(children) {

    override fun appendTo(appendable: Appendable, context: Context) {
        appendChildren(appendable, context)
    }

    override suspend fun outputTo(out: CoOutput, context: Context) {
        outputChildren(out, context)
    }

    fun renderTo(appendable: Appendable, contextObject: Any? = null) {
        appendTo(appendable, Context(contextObject))
    }

    suspend fun coRender(contextObject: Any? = null, out: CoOutput) {
        outputTo(out, Context(contextObject))
    }

    fun render(contextObject: Any? = null): String = StringBuilder().also {
        appendTo(it, Context(contextObject))
    }.toString()

    companion object {

        val parser by lazy {
            Parser()
        }

        fun parse(file: File): Template = Parser(file.parentFile).parse(file)

        fun parse(inputStream: InputStream, charset: Charset? = null) = parser.parse(inputStream, charset)

        fun parse(reader: Reader) = parser.parse(reader)

        fun parse(template: String) = parser.parse(template)

    }

}
