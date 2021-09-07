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

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.expect
import kotlin.test.fail
import java.io.File
import java.net.URL
import java.nio.file.FileSystems

class ParserTest {

    @Test fun `should create Parser with File directory`() {
        val parser = Parser(File("src/test/resources/templates"))
        val template = parser.parseByName("dummy")
        expect("Dummy template\n") { template.render() }
    }

    @Test fun `should create Parser with Path directory`() {
        val parser = Parser(FileSystems.getDefault().getPath("src/test/resources/templates"))
        val template = parser.parseByName("dummy")
        expect("Dummy template\n") { template.render() }
    }

    @Test fun `should create Parser with resource URL directory`() {
        val parser = Parser(ParserTest::class.java.getResource("/templates") ?: fail("templates not found"))
        val template = parser.parseByName("dummy")
        expect("Dummy template\n") { template.render() }
    }

    @Ignore // TODO enable this when source is uploaded
    @Test fun `should create Parser with HTTP URL directory`() {
        val parser = Parser(URL("https://github.com/pwall567/mustache-k/blob/main/src/test/resources/templates/"))
        val template = parser.parseByName("dummy")
        expect("Dummy template\n") { template.render() }
    }

    @Test fun `should create Parser with custom resolver`() {
        val parser = Parser { name ->
            when (name) {
                "dummy" -> File("src/test/resources/templates/dummy.mustache").inputStream().reader()
                else -> fail("Can't resolve name - $name")
            }
        }
        val template = parser.parseByName("dummy")
        expect("Dummy template\n") { template.render() }
    }

    @Test fun `should use custom extension`() {
        val parser = Parser(ParserTest::class.java.getResource("/templates") ?: fail("templates not found"))
        parser.extension = "hbs"
        val template = parser.parseByName("dummy")
        expect("Dummy handlebars\n") { template.render() }
    }

}
