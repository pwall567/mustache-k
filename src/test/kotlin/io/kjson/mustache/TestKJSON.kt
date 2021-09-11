/*
 * @(#) TestKJSON.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2021 Peter Wall
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

import io.kjson.JSON

class TestKJSON {

    @Test fun `should work with KJSON`() {
        val json = JSON.parse("""{"a":"X","b":23,"c":true,"d":false,"e":[1,2,3],"f":"","g":0}""")
        val template1 = Template.parse("a={{&a}}, b={{&b}}, c={{#c}}true{{/c}}{{^c}}false{{/c}}")
        expect("a=X, b=23, c=true") { template1.render(json) }
        val template2 = Template.parse("c={{#c}}true{{/c}}{{^c}}false{{/c}}, d={{#d}}true{{/d}}{{^d}}false{{/d}}")
        expect("c=true, d=false") { template2.render(json) }
        val template3 = Template.parse("-{{#e}}{{.}};{{/e}}-")
        expect("-1;2;3;-") { template3.render(json) }
        val template4 = Template.parse("{{^f}}OK{{/f}}")
        expect("OK") { template4.render(json) }
        val template5 = Template.parse("{{^g}}OK{{/g}}")
        expect("OK") { template5.render(json) }
        val template6 = Template.parse("#{{#g}}OK{{/g}}#")
        expect("##") { template6.render(json) }
    }

}
