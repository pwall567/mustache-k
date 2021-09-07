/*
 * @(#) TemplateParserTest.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2020, 2021 Peter Wall
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
import java.io.StringReader

class TemplateParserTest {

    @Test fun `should output empty string from empty template`() {
        val template = Template.parse(StringReader(""))
        expect("") { template.render() }
    }

    @Test fun `should output text-only template`() {
        val template = Template.parse(StringReader("hello"))
        expect("hello") { template.render() }
    }

    @Test fun `should output template with variable`() {
        val template = Template.parse(StringReader("hello, {{aaa}}"))
        val data = TestClass("world")
        expect("hello, world") { template.render(data) }
    }

    @Test fun `should output template variables escaped by default`() {
        val template = Template.parse(StringReader("hello, {{aaa}}"))
        val data = TestClass("<\u20ACeuro>")
        expect("hello, &lt;&euro;euro&gt;") { template.render(data) }
    }

    @Test fun `should output literal variables unescaped`() {
        val template = Template.parse(StringReader("hello, {{{aaa}}}!"))
        val data = TestClass("<world>")
        expect("hello, <world>!") { template.render(data) }
    }

    @Test fun `should output section for each member of list`() {
        val template = Template.parse(StringReader("data:{{#items}} hello, {{aaa}};{{/items}}"))
        val data = mapOf("items" to listOf(TestClass("world"), TestClass("moon")))
        expect("data: hello, world; hello, moon;") { template.render(data) }
    }

}
