/*
 * @(#) TemplateTest.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2020, 2021, 2024 Peter Wall
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

import kotlin.test.Test
import kotlin.test.expect
import kotlinx.coroutines.runBlocking

import net.pwall.util.CoOutput
import net.pwall.util.output

class TemplateTest {

    @Test fun `should output empty string from empty template`() {
        val template = Template(listOf())
        expect("") { template.render() }
        val sb = StringBuilder()
        template.renderTo(sb)
        expect("") { sb.toString() }
    }

    @Test fun `should output text-only template`() {
        val template = Template(listOf(TextElement("hello")))
        expect("hello") { template.render() }
        val sb = StringBuilder()
        template.renderTo(sb)
        expect("hello") { sb.toString() }
    }

    @Test fun `should output template with variable`() {
        val template = Template(listOf(TextElement("hello, "), Variable("aaa")))
        val data = TestClass("world")
        expect("hello, world") { template.render(data) }
        val sb = StringBuilder()
        template.renderTo(sb, data)
        expect("hello, world") { sb.toString() }
    }

    @Test fun `should output template variables escaped by default`() {
        val template = Template(listOf(TextElement("hello, "), Variable("aaa")))
        val data = TestClass("<world>")
        expect("hello, &lt;world&gt;") { template.render(data) }
        val sb = StringBuilder()
        template.renderTo(sb, data)
        expect("hello, &lt;world&gt;") { sb.toString() }
    }

    @Test fun `should output literal template variables unescaped`() {
        val template = Template(listOf(TextElement("hello, "), LiteralVariable("aaa")))
        val data = TestClass("<world>")
        expect("hello, <world>") { template.render(data) }
        val sb = StringBuilder()
        template.renderTo(sb, data)
        expect("hello, <world>") { sb.toString() }
    }

    @Test fun `should output section for each member of list`() {
        val section = Section("items", listOf(TextElement(" hello, "), Variable("aaa"), TextElement(";")))
        val template = Template(listOf(TextElement("data:"), section))
        val data = mapOf("items" to listOf(TestClass("world"), TestClass("moon")))
        expect("data: hello, world; hello, moon;") { template.render(data) }
    }

    @Test fun `should output list size`() {
        val template = Template(listOf(Variable("list.size")))
        val data = mapOf("list" to listOf(1, 2, 3))
        expect("3") { template.render(data) }
    }

    @Test fun `should output list entries`() {
        val template = Template(listOf(Variable("list.2")))
        val data = mapOf("list" to listOf(111, 222, 333))
        expect("333") { template.render(data) }
    }

    @Test fun `should output string length`() {
        val template = Template(listOf(Variable("string.length")))
        val data = mapOf("string" to "Hello!")
        expect("6") { template.render(data) }
    }

    @Test fun `should output to Appendable`() {
        val template = Template(listOf(TextElement("hello")))
        val stringBuilder = StringBuilder()
        template.renderTo(stringBuilder)
        expect("hello") { stringBuilder.toString() }
    }

    @Test fun `should output to CoOutput`() = runBlocking {
        val template = Template(listOf(TextElement("hello")))
        val coCapture = CoCapture()
        template.coRender { coCapture.output(it) }
        expect("hello") { coCapture.toString() }
    }

    @Test fun `should output list to CoOutput`() = runBlocking {
        val section = Section("items", listOf(TextElement(" hello, "), Variable("aaa"), TextElement(";")))
        val template = Template(listOf(TextElement("data:"), section))
        val data = mapOf("items" to listOf(TestClass("world"), TestClass("moon")))
        val coCapture = CoCapture()
        template.coRender(data) { coCapture.output(it) }
        expect("data: hello, world; hello, moon;") { coCapture.toString() }
    }

    @Test fun `should output to CoOutput with HTML escaping`() = runBlocking {
        val template = Template(listOf(TextElement("hello, "), Variable("aaa")))
        val data = TestClass("<world>")
        val coCapture = CoCapture()
        template.coRender(data, coCapture)
        expect("hello, &lt;world&gt;") { coCapture.toString() }
    }

    class CoCapture(size: Int = 256) : CoOutput {

        private val array = CharArray(size)
        private var index = 0

        override suspend fun invoke(ch: Char) {
            array[index++] = ch
        }

        override fun toString() = String(array, 0, index)

    }

}
