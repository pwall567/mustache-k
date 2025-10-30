/*
 * @(#) TemplateParserTest.kt
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
import kotlin.test.fail

import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader

import io.kstuff.test.shouldBe

import io.kjson.mustache.parser.Parser

class TemplateParserTest {

    @Test fun `should output empty string from empty template`() {
        val template = Template.parse("")
        template.render() shouldBe ""
    }

    @Test fun `should output text-only template`() {
        val template = Template.parse("hello")
        template.render() shouldBe "hello"
    }

    @Test fun `should output template with variable`() {
        val template = Template.parse("hello, {{aaa}}")
        val data = TestClass("world")
        template.render(data) shouldBe "hello, world"
    }

    @Test fun `should output template variables escaped by default`() {
        val template = Template.parse("hello, {{aaa}}")
        val data = TestClass("<\u20ACeuro>\n")
        template.render(data) shouldBe "hello, &lt;&euro;euro&gt;\n"
    }

    @Test fun `should output literal variables unescaped`() {
        val template = Template.parse("hello, {{{aaa}}}!")
        val data = TestClass("<world>\n")
        template.render(data) shouldBe "hello, <world>\n!"
    }

    @Test fun `should output section for each member of list`() {
        val template = Template.parse("data:{{#items}} hello, {{aaa}};{{/items}}")
        val data = mapOf("items" to listOf(TestClass("world"), TestClass("moon")))
        template.render(data) shouldBe "data: hello, world; hello, moon;"
    }

    @Test fun `should output section for each member of list using dot`() {
        val template = Template.parse("data: {{#items}}hello, {{.}};{{/items}}")
        val data = mapOf("items" to listOf("world", "moon"))
        template.render(data) shouldBe "data: hello, world;hello, moon;"
    }

    @Test fun `should output section for each member of nested list`() {
        val template = Template.parse("data: {{#items}}{{#.}}hello, {{.}};{{/.}}{{/items}}")
        val data = mapOf("items" to listOf(listOf("world", "moon"), listOf("venus", "mars")))
        template.render(data) shouldBe "data: hello, world;hello, moon;hello, venus;hello, mars;"
    }

    @Test fun `should output section with iterable variables`() {
        val template = Template.parse("data: {{#items}}{{index1}}.hello, {{.}}{{^last}}; {{/last}}{{/items}}")
        val data = mapOf("items" to listOf("world", "moon", "mars"))
        template.render(data) shouldBe "data: 1.hello, world; 2.hello, moon; 3.hello, mars"
    }

    @Test fun `should output list using partial`() {
        val parser = Parser(TemplateParserTest::class.java.getResource("/templates") ?: fail("templates not found"))
        val template = parser.parseByName("list")
        val data = mapOf("list" to listOf("abc", "def", "ghi"))
        template.render(data) shouldBe "abc,\ndef,\nghi\n\n"
    }

    @Test fun `should locate partial using supplied directory and extension`() {
        val parser = Parser(File("src/test/resources/templates"))
        parser.defaultExtension = "hbs"
        val template = parser.parse("{{#list}}{{>list_item}}{{/list}}")
        val data = mapOf("list" to listOf("abc", "def", "ghi"))
        template.render(data) shouldBe "abc,\ndef,\nghi\n"
    }

    @Test fun `should locate partial using custom resolver`() {
        val parser = Parser { parse(File("src/test/resources/templates", "$it.hbs").reader()) }
        val template = parser.parse("{{#list}}{{>list_item}}{{/list}}")
        val data = mapOf("list" to listOf("abc", "def", "ghi"))
        template.render(data) shouldBe "abc,\ndef,\nghi\n"
    }

    @Test fun `should output section conditionally depending on enum`() {
        val template = Template.parse("data: {{#eee}}{{#A}}Q{{/A}}{{#B}}R{{/B}}{{#C}}S{{/C}}{{/eee}}")
        template.render(mapOf("eee" to TestEnum.A)) shouldBe "data: Q"
        template.render(mapOf("eee" to TestEnum.B)) shouldBe "data: R"
        template.render(mapOf("eee" to TestEnum.C)) shouldBe "data: S"
    }

    @Test fun `should output inverted section conditionally depending on enum`() {
        val template = Template.parse("data: {{#eee}}{{^A}}Q{{/A}}{{^B}}R{{/B}}{{^C}}S{{/C}}{{/eee}}")
        template.render(mapOf("eee" to TestEnum.A)) shouldBe "data: RS"
        template.render(mapOf("eee" to TestEnum.B)) shouldBe "data: QS"
        template.render(mapOf("eee" to TestEnum.C)) shouldBe "data: QR"
    }

    @Test fun `should output section conditionally depending on enum including content`() {
        val template = Template.parse("data: {{#eee}}{{#AA}}Q-{{&extra}}{{/AA}}{{#BB}}R{{/BB}}{{#CC}}S{{/CC}}{{/eee}}")
        template.render(mapOf("eee" to TestEnum2.AA)) shouldBe "data: Q-alpha"
        template.render(mapOf("eee" to TestEnum2.BB)) shouldBe "data: R"
        template.render(mapOf("eee" to TestEnum2.CC)) shouldBe "data: S"
    }

    @Test fun `should use recursive partial`() {
        val template = Template.parse(File("src/test/resources/templates/recursive.mustache"))
        val recursive1 = Recursive("abc", emptyList())
        val recursive2 = Recursive("def", listOf(recursive1))
        template.render(recursive2) shouldBe "(abc)(def)"
        val recursive3 = Recursive("ghi", listOf(recursive1, recursive2))
        template.render(recursive3) shouldBe "(abc)(abc)(def)(ghi)"
    }

    @Test fun `should use shared instance of parser`() {
        val parser = Template.parser
        val template = parser.parse("hello")
        template.render(null) shouldBe "hello"
    }

    @Test fun `should read InputStream`() {
        val templateText = """aaa="{{&aaa}}", bbb="{{&bbb}}""""
        val bais = ByteArrayInputStream(templateText.toByteArray())
        val template = Template.parse(bais)
        template.render(TestClass("Hello", "World")) shouldBe """aaa="Hello", bbb="World""""
    }

    @Test fun `should read Reader`() {
        val templateText = """aaa="{{&aaa}}", bbb="{{&bbb}}""""
        val reader = StringReader(templateText)
        val template = Template.parse(reader)
        template.render(TestClass("Hello", "World")) shouldBe """aaa="Hello", bbb="World""""
    }

    @Test fun `should accept custom delimiters`() {
        val template = Template.parse("{{aaa}}, {{=<% %>=}}<%bbb%>")
        val data = TestClass("Hello", "World")
        template.render(data) shouldBe "Hello, World"
    }

    @Test fun `should find field using dot notation`() {
        val template = Template.parse("Hello {{&xxx.aaa}} (and {{xxx.bbb}})")
        val data = mapOf("xxx" to TestClass("World", "Moon"))
        template.render(data) shouldBe "Hello World (and Moon)"
    }

    @Test fun `should find field using multiple dot notation`() {
        val template = Template.parse("Hello {{&q.xxx.aaa}} (and {{q.xxx.bbb}})")
        val data1 = mapOf("xxx" to TestClass("World", "Moon"))
        val data2 = mapOf("q" to data1)
        template.render(data2) shouldBe "Hello World (and Moon)"
    }

    @Test fun `should accept kebab-case names`() {
        val template = Template.parse("Kebab {{&data.kebab-case-name}}")
        val data = mapOf("data" to KebabCase("correct"))
        template.render(data) shouldBe "Kebab correct"
    }

    data class Recursive(val text: String, val list: List<Recursive>)

    enum class TestEnum { A, B, C }

    enum class TestEnum2(val extra: String) { AA("alpha"), BB("beta"), CC("gamma") }

    data class KebabCase(@Suppress("PropertyName") val `kebab-case-name`: String)

}
