/*
 * @(#) ParserTest.kt
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

package io.kjson.mustache.parser

import kotlin.test.Test
import kotlin.test.fail

import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.nio.file.FileSystems

import io.kstuff.test.shouldBe

import io.jstuff.util.IntOutput
import io.kstuff.util.CoOutput
import io.kjson.resource.Resource

import io.kjson.mustache.Context
import io.kjson.mustache.Partial

class ParserTest {

    @Test fun `should create Parser with File directory`() {
        val parser = Parser(File("src/test/resources/templates"))
        val template = parser.parseByName("dummy")
        template.render() shouldBe "Dummy template\n"
    }

    @Test fun `should create Parser with Path directory`() {
        val parser = Parser(FileSystems.getDefault().getPath("src/test/resources/templates"))
        val template = parser.parseByName("dummy")
        template.render() shouldBe "Dummy template\n"
    }

    @Test fun `should create Parser with resource URL directory`() {
        val parser = Parser(Resource.classPathURL("/templates") ?: fail("templates not found"))
        val template = parser.parseByName("dummy")
        template.render() shouldBe "Dummy template\n"
    }

    @Test fun `should create Parser with HTTP URL directory`() {
        val parser =
                Parser(URL("https://raw.githubusercontent.com/pwall567/mustache-k/main/src/test/resources/templates/"))
        val template = parser.parseByName("dummy")
        template.render() shouldBe "Dummy template\n"
    }

    @Test fun `should create Parser with custom resolver`() {
        val parser = Parser { name ->
            when (name) {
                "substitute" -> parse(File("src/test/resources/templates/dummy.mustache").reader())
                else -> fail("Can't resolve name - $name")
            }
        }
        val template = parser.parseByName("substitute")
        template.render() shouldBe "Dummy template\n"
    }

    @Test fun `should use custom extension`() {
        val parser = Parser(Resource.classPathURL("/templates") ?: fail("templates not found"))
        parser.defaultExtension = "hbs"
        val template = parser.parseByName("dummy")
        template.render() shouldBe "Dummy handlebars\n"
    }

    @Test fun `should use specified charset`() {
        val parser = Parser()
        parser.charset = Charsets.ISO_8859_1
        val byteArray = byteArrayOf(0xA1.toByte(), 0x48, 0x6F, 0x6C, 0x61, 0x21)
        val template = parser.parse(ByteArrayInputStream(byteArray))
        template.render() shouldBe "\u00A1Hola!"
    }

    @Test fun `should use custom partial`() {
        val parser = Parser(Resource.classPathURL("/templates") ?: fail("templates not found"))
        parser.addPartial("money", object : Partial {
            override fun appendTo(appendable: Appendable, context: Context) {
                (context.contextObject as? Long)?.let {
                    val dollars = it / 100
                    val cents = (it % 100).toInt()
                    appendable.append('$')
                    IntOutput.appendPositiveLongGrouped(appendable, dollars, ',')
                    appendable.append('.')
                    IntOutput.append2Digits(appendable, cents)
                } ?: appendable.append("**ERROR**")
            }
            override suspend fun outputTo(out: CoOutput, context: Context) {
                throw NotImplementedError()
            }
        })
        val template = parser.parse("{{>money}}")
        template.render(123456789L) shouldBe "$1,234,567.89"
    }

    @Test fun `should get existing partial and add it with a new name`() {
        val parser = Parser(Resource.classPathURL("/templates") ?: fail("templates not found"))
        parser.addPartial("extra", parser.getPartial("dummy"))
        val template = parser.parse("{{>extra}}")
        template.render() shouldBe "Dummy template\n"
    }

    @Test fun `should use second resource directory - File`() {
        val parser = Parser(File("src/test/resources/templates"))
        val data = mapOf("list" to listOf(listOf("world", "moon"), listOf("venus", "mars")))
        parser.parseByName("list").render(data) shouldBe "[world, moon],\n[venus, mars]\n\n"
        parser.addDirectory(File("src/test/resources/templates2"))
        parser.parseByName("list").render(data) shouldBe "2:[world, moon],\n[venus, mars]\n\n"
    }

    @Test fun `should use second resource directory - Path`() {
        val parser = Parser(FileSystems.getDefault().getPath("src/test/resources/templates"))
        val data = mapOf("list" to listOf(listOf("world", "moon"), listOf("venus", "mars")))
        parser.parseByName("list").render(data) shouldBe "[world, moon],\n[venus, mars]\n\n"
        parser.addDirectory(FileSystems.getDefault().getPath("src/test/resources/templates2"))
        parser.parseByName("list").render(data) shouldBe "2:[world, moon],\n[venus, mars]\n\n"
    }

    @Test fun `should use second resource directory - url`() {
        val parser = Parser(Resource.classPathURL("/templates") ?: fail("templates not found"))
        val data = mapOf("list" to listOf(listOf("world", "moon"), listOf("venus", "mars")))
        parser.parseByName("list").render(data) shouldBe "[world, moon],\n[venus, mars]\n\n"
        parser.addDirectory(Resource.classPathURL("/templates2") ?: fail("templates2 not found"))
        parser.parseByName("list").render(data) shouldBe "2:[world, moon],\n[venus, mars]\n\n"
    }

}
