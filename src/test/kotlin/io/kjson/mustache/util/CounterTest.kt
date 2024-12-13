/*
 * @(#) CounterTest.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2023 Peter Wall
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

import io.kstuff.test.shouldBe

class CounterTest {

    @Test fun `should create Counter`() {
        with(Counter()) {
            value shouldBe 0
            toString() shouldBe "0"
        }
        with(Counter(1)) {
            value shouldBe 1
            toString() shouldBe "1"
        }
    }

    @Test fun `should increment Counter`() {
        val counter = Counter()
        counter.value shouldBe 0
        counter.toString() shouldBe "0"
        counter.increment()
        counter.value shouldBe 1
        counter.toString() shouldBe "1"
        counter.increment()
        counter.value shouldBe 2
        counter.toString() shouldBe "2"
    }

    @Test fun `should increment Counter by specified amount`() {
        val counter = Counter()
        counter.value shouldBe 0
        counter.toString() shouldBe "0"
        counter.incrementBy(1)
        counter.value shouldBe 1
        counter.toString() shouldBe "1"
        counter.incrementBy(5)
        counter.value shouldBe 6
        counter.toString() shouldBe "6"
    }

    @Test fun `should reset Counter`() {
        val counter = Counter()
        counter.value shouldBe 0
        counter.toString() shouldBe "0"
        counter.increment()
        counter.value shouldBe 1
        counter.toString() shouldBe "1"
        counter.increment()
        counter.value shouldBe 2
        counter.toString() shouldBe "2"
        counter.clear()
        counter.value shouldBe 0
        counter.toString() shouldBe "0"
    }

    @Test fun `should use units`() {
        val counter = Counter(units = "record")
        counter.value shouldBe 0
        counter.toString() shouldBe "0 records"
        counter.increment()
        counter.value shouldBe 1
        counter.toString() shouldBe "1 record"
        counter.increment()
        counter.value shouldBe 2
        counter.toString() shouldBe "2 records"
    }

    @Test fun `should use units with irregular plurals`() {
        val counter = Counter(units = "entry", plural = "entries")
        counter.value shouldBe 0
        counter.toString() shouldBe "0 entries"
        counter.increment()
        counter.value shouldBe 1
        counter.toString() shouldBe "1 entry"
        counter.increment()
        counter.value shouldBe 2
        counter.toString() shouldBe "2 entries"
    }

}
