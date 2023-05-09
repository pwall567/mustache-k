/*
 * @(#) IndentTest.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2020, 2023 Peter Wall
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

import kotlin.test.Test
import kotlin.test.expect

import java.io.StringReader

import io.kjson.mustache.Template
import kotlin.test.assertSame

class IndentTest {

    @Test fun `should create initial Indent`() {
        with (Indent()) {
            expect(0) { level }
            expect(4) { size }
            expect(0) { spaces.length }
        }
        with (Indent(size = 2)) {
            expect(0) { level }
            expect(2) { size }
            expect(0) { spaces.length }
        }
        with (Indent(3)) {
            expect(3) { level }
            expect(4) { size }
            expect(12) { spaces.length }
        }
    }

    @Test fun `should increment level`() {
        val i = Indent(5, 2)
        with (i.increment) {
            expect(6) { level }
            expect(2) { size }
            expect(12) { spaces.length }
        }
    }

    @Test fun `should return self as property indent`() {
        val i = Indent(5, 2)
        assertSame(i, i.indent)
    }

    @Test fun `should output no indent initially`() {
        val template = Template.parse(StringReader("[{{&indent}}]"))
        expect("[]") { template.render(Indent()) }
    }

    @Test fun `should output incremented indent`() {
        val template = Template.parse(StringReader("[{{#indent.increment}}{{&indent}}{{/indent.increment}}]"))
        expect("[    ]") { template.render(Indent()) }
    }

    @Test fun `should output doubly incremented indent`() {
        val template = Template.parse(StringReader(
                "[{{#indent.increment}}{{#indent.increment}}{{&indent}}{{/indent.increment}}{{/indent.increment}}]"))
        expect("[        ]") { template.render(Indent()) }
    }

}
