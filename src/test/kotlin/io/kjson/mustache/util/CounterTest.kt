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
import kotlin.test.expect

class CounterTest {

    @Test fun `should create Counter`() {
        with (Counter()) {
            expect(0) { value }
            expect("0") { toString() }
        }
        with (Counter(1)) {
            expect(1) { value }
            expect("1") { toString() }
        }
    }

    @Test fun `should increment Counter`() {
        val counter = Counter()
        expect(0) { counter.value }
        expect("0") { counter.toString() }
        counter.increment()
        expect(1) { counter.value }
        expect("1") { counter.toString() }
        counter.increment()
        expect(2) { counter.value }
        expect("2") { counter.toString() }
    }

    @Test fun `should increment Counter by specified amount`() {
        val counter = Counter()
        expect(0) { counter.value }
        expect("0") { counter.toString() }
        counter.incrementBy(1)
        expect(1) { counter.value }
        expect("1") { counter.toString() }
        counter.incrementBy(5)
        expect(6) { counter.value }
        expect("6") { counter.toString() }
    }

    @Test fun `should reset Counter`() {
        val counter = Counter()
        expect(0) { counter.value }
        expect("0") { counter.toString() }
        counter.increment()
        expect(1) { counter.value }
        expect("1") { counter.toString() }
        counter.increment()
        expect(2) { counter.value }
        expect("2") { counter.toString() }
        counter.clear()
        expect(0) { counter.value }
        expect("0") { counter.toString() }
    }

    @Test fun `should use units`() {
        val counter = Counter(units = "record")
        expect(0) { counter.value }
        expect("0 records") { counter.toString() }
        counter.increment()
        expect(1) { counter.value }
        expect("1 record") { counter.toString() }
        counter.increment()
        expect(2) { counter.value }
        expect("2 records") { counter.toString() }
    }

    @Test fun `should use units with irregular plurals`() {
        val counter = Counter(units = "entry", plural = "entries")
        expect(0) { counter.value }
        expect("0 entries") { counter.toString() }
        counter.increment()
        expect(1) { counter.value }
        expect("1 entry") { counter.toString() }
        counter.increment()
        expect(2) { counter.value }
        expect("2 entries") { counter.toString() }
    }

}
