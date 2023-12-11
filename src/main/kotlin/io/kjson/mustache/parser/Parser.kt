/*
 * @(#) Parser.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2020, 2021, 2023 Peter Wall
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

import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path

import io.kjson.mustache.Element
import io.kjson.mustache.InvertedSection
import io.kjson.mustache.LiteralVariable
import io.kjson.mustache.Partial
import io.kjson.mustache.Section
import io.kjson.mustache.Template
import io.kjson.mustache.TemplatePartial
import io.kjson.mustache.TextElement
import io.kjson.mustache.Variable
import io.kjson.resource.Resource
import io.kjson.resource.ResourceNotFoundException

/**
 * Mustache template parser.
 *
 * @author  Peter Wall
 */
class Parser(
    val resolver: Parser.(String) -> Template = Parser::defaultResolver,
) {

    private val mustacheLoader = MustacheLoader(this)

    private val directories = mutableListOf<Resource<Template>>()

    /**
     * Construct a `Parser` with the nominated directory (as a [File]) as the location for templates.
     */
    constructor(directory: File) : this(Parser::defaultResolver) {
        addDirectory(directory)
    }

    /**
     * Construct a `Parser` with the nominated directory (as a [Path]) as the location for templates.
     */
    constructor(directoryPath: Path) : this(Parser::defaultResolver) {
        addDirectory(directoryPath)
    }

    /**
     * Construct a `Parser` with the nominated directory (as a [URL]) as the location for templates.
     */
    constructor(directoryPath: URL) : this(Parser::defaultResolver) {
        addDirectory(directoryPath)
    }

    private val partialCache: MutableMap<String, Partial> = mutableMapOf()

    /** The default extension (initialised to `mustache`) */
    var defaultExtension = standardDefaultExtension
        set(newExtension) {
            val trimmedExtension = if (newExtension.startsWith('.')) newExtension.drop(1) else newExtension
            if (!extensionRegex.matches(trimmedExtension))
                throw MustacheParserException("Invalid extension - $trimmedExtension")
            field = trimmedExtension
            mustacheLoader.defaultExtension = trimmedExtension
        }

    /** The `Charset` to be used to read templates (if `null`, the character set will be determined dynamically) */
    var charset: Charset? = null

    fun addDirectory(directory: File) {
        directories.add(0, mustacheLoader.resource(directory))
    }

    fun addDirectory(directoryPath: Path) {
        directories.add(0, mustacheLoader.resource(directoryPath))
    }

    fun addDirectory(directoryURL: URL) {
        directories.add(0, mustacheLoader.resource(directoryURL))
    }

    /**
     * Parse a template from a [File].
     */
    fun parse(file: File) = parse(file.inputStream())

    /**
     * Parse a template from an [InputStream] with an optional [Charset].
     */
    // The DynamicReader class is causing unexplained problems, particular when used in a ktor application.
    // Until these problems are rectified, a more simple implementation is being substituted.
    //fun parse(inputStream: InputStream, charset: Charset? = this.charset) = parse(DynamicReader(inputStream, charset))
    fun parse(inputStream: InputStream, charset: Charset? = this.charset) =
            parse(inputStream.reader(charset ?: Charsets.UTF_8))

    /**
     * Parse a template from a [Reader].
     */
    fun parse(reader: Reader) = Template(parseNested(reader.buffered(), ParseContext()))

    /**
     * Parse a template from a [String].
     */
    fun parse(template: String) = parse(StringReader(template))

    /**
     * Parse a named template using the partial resolution mechanism of this `Parser`.
     */
    fun parseByName(name: String) = resolver(name)

    private fun parseNested(reader: Reader, context: ParseContext): List<Element> {
        var currentContext = context
        val elements = mutableListOf<Element>()
        val sb = StringBuilder()
        while (reader.readUntilDelimiter(currentContext.openDelimiter, sb)) {
            if (sb.isNotEmpty())
                elements.add(TextElement(sb.getAndReset()))
            reader.read().let {
                when {
                    it < 0 -> throw MustacheParserException("Unclosed tag at end of document")
                    it == '{'.code -> {
                        if (!reader.readUntilDelimiter("}${currentContext.closeDelimiter}", sb))
                            throw MustacheParserException("Unclosed literal tag at end of document")
                        val tag = sb.getAndReset().trim()
                        if (tag.isEmpty())
                            throw MustacheParserException("Illegal empty literal tag")
                        elements.add(LiteralVariable(tag))
                    }
                    else -> {
                        sb.append(it.toChar())
                        if (!reader.readUntilDelimiter(currentContext.closeDelimiter, sb))
                            throw MustacheParserException("Unclosed tag at end of document")
                        val tag = sb.getAndReset().trim()
                        if (tag.isEmpty())
                            throw MustacheParserException("Illegal empty literal tag")
                        when (tag[0]) {
                            '&' -> elements.add(LiteralVariable(tag.drop(1).trim()))
                            '#' -> {
                                val section = tag.drop(1).trim()
                                elements.add(Section(section,
                                        parseNested(reader, currentContext.copy(section = section))))
                            }
                            '^' -> {
                                val section = tag.drop(1).trim()
                                elements.add(
                                    InvertedSection(section,
                                        parseNested(reader, currentContext.copy(section = section)))
                                )
                            }
                            '/' -> {
                                val section = tag.drop(1).trim()
                                if (section != currentContext.section)
                                    throw MustacheParserException("Unmatched section close tag - $section")
                                return elements
                            }
                            '>' -> {
                                val name = tag.drop(1).trim()
                                elements.add(getPartial(name))
                            }
                            '=' -> currentContext = setDelimiters(tag, currentContext)
                            '!' -> {}
                            else -> elements.add(Variable(tag))
                        }
                    }
                }
            }
        }
        if (currentContext.section != null)
            throw MustacheParserException("Unclosed section at end of document")
        if (sb.isNotEmpty())
            elements.add(TextElement(sb.toString()))
        return elements
    }

    private fun setDelimiters(tag: String, context: ParseContext): ParseContext {
        val eq = tag.indexOf('=', 1)
        if (eq == tag.length - 1) {
            val strippedTag = tag.substring(1, eq)
            val sp = strippedTag.indexOf(' ')
            if (sp > 0) {
                val open = strippedTag.substring(0, sp)
                val close = strippedTag.substring(sp + 1).trimStart()
                if (close.isNotEmpty())
                    return context.copy(openDelimiter = open, closeDelimiter = close)
            }
        }
        throw MustacheParserException("Incorrect delimiter tag")
    }

    /**
     * Add a custom Partial.
     */
    fun addPartial(name: String, partial: Partial) {
        partialCache[name] = partial
    }

    /**
     * Get a Partial by name.
     */
    fun getPartial(name: String): Partial {
        partialCache[name]?.let { return it }
        return TemplatePartial().also {
            partialCache[name] = it
            it.template = resolver(name)
        }
    }

    private fun defaultResolver(name: String): Template {
        if (directories.isEmpty())
            directories.add(mustacheLoader.resource(currentDirectory))
        for (directory in directories) {
            try {
                return directory.resolve(name).load()
            }
            catch (_: ResourceNotFoundException) { // do nothing
            }
        }
        throw MustacheParserException("Can't locate template - $name")
    }

    companion object {

        const val standardDefaultExtension = "mustache"

        val currentDirectory = File(".")

        val extensionRegex = Regex("^[a-zA-Z0-9][a-zA-Z0-9-+#]*$")

        fun Reader.readUntilDelimiter(delimiter: String, sb: StringBuilder): Boolean {
            val n = delimiter.length - 1
            val stopper = delimiter.last().code
            while (true) {
                val ch = read()
                if (ch < 0)
                    return false
                if (ch == stopper && sb.length >= n && delimiterMatches(delimiter, sb)) {
                    sb.setLength(sb.length - n)
                    return true
                }
                sb.append(ch.toChar())
            }
        }

        private fun delimiterMatches(delimiter: String, sb: StringBuilder): Boolean {
            var i = delimiter.length - 1
            var j = sb.length
            while (i > 0) {
                if (delimiter[--i] != sb[--j])
                    return false
            }
            return true
        }

        fun StringBuilder.getAndReset(): String {
            return toString().also { setLength(0) }
        }

    }

}
