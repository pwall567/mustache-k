/*
 * @(#) Indent.kt
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

/**
 * A class to help with indentation during template expansion.
 *
 * When the context object for Mustache contains: `val indent: Indent()`, then any references in the template to
 * `{{&indent}}` will cause nothing to be output.  But if the [increment] value accessor is invoked as a section,
 * creating a nested context, then within that context a reference to `{{&indent}}` will cause 4 spaces (or the number
 * specified as the `size`) to be output.  This may be repeated to any required depth, and is particularly useful for
 * templates that employ recursion.
 *
 * Example:
 * ```
 * {{&indent}}This will not be indented.
 * {{#indent.increment}}{{&indent}}This will be indented 4 spaces.
 * {{#indent.increment}}{{&indent}}This will be indented 8 spaces.
 * {{/indent.increment}}{{/indent.increment}}
 * ```
 *
 * @author  Peter Wall
 */
class Indent(
    val level: Int = 0,
    val size: Int = 4,
) {

    val spaces: String = String(CharArray(level * size) { ' ' })

    val increment: Indent
        get() = Indent(level + 1, size)

    val indent: Indent
        get() = this

    override fun toString(): String = spaces

}
