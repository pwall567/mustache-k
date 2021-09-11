/*
 * @(#) Parser.kt
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

import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import io.kjson.mustache.Element
import io.kjson.mustache.InvertedSection
import io.kjson.mustache.LiteralVariable
import io.kjson.mustache.Partial
import io.kjson.mustache.Section
import io.kjson.mustache.Template
import io.kjson.mustache.TemplatePartial
import io.kjson.mustache.TextElement
import io.kjson.mustache.Variable

/**
 * Mustache template parser.
 *
 * @author  Peter Wall
 */
class Parser private constructor(
    private val directoryPath: Path?,
    private val directoryURL: URL,
    val resolvePartial: Parser.(String) -> Reader
) {

    /**
     * Construct a `Parser` with the nominated directory (as a [File]) as the location for partials.
     */
    constructor(directory: File) : this(directory.toPath(), directory.toURI().toURL(), Parser::defaultResolvePartial)

    /**
     * Construct a `Parser` with the nominated directory (as a [Path]) as the location for partials.
     */
    constructor(directoryPath: Path) : this(directoryPath, directoryPath.toUri().toURL(), Parser::defaultResolvePartial)

    /**
     * Construct a `Parser` with the nominated directory (as a [URL]) as the location for partials.
     */
    constructor(directoryURL: URL) : this(derivePath(directoryURL), directoryURL, Parser::defaultResolvePartial)

    /**
     * Construct a `Parser` with a lambda for custom partial resolution.
     */
    constructor(resolvePartial: Parser.(String) -> Reader = Parser::defaultResolvePartial) :
            this(currentDirectory.toPath(), currentDirectory.toURI().toURL(), resolvePartial)

    var extension = defaultExtension
        set(newExtension) {
            val trimmedExtension = if (newExtension.startsWith('.')) newExtension.drop(1) else newExtension
            if (!extensionRegex.containsMatchIn(trimmedExtension))
                throw MustacheParserException("Invalid extension - $trimmedExtension")
            field = trimmedExtension
        }

    var charset: Charset = Charsets.UTF_8

    private val partialCache = mutableMapOf<String, Partial>()

    /**
     * Parse a template from a [File].
     */
    fun parse(file: File): Template {
        return parse(file.inputStream())
    }

    /**
     * Parse a template from an [InputStream].
     */
    fun parse(inputStream: InputStream, charset: Charset = this.charset): Template {
        return parse(inputStream.reader(charset))
    }

    /**
     * Parse a template from a [Reader].
     */
    fun parse(reader: Reader): Template {
        return Template(parseNested(reader.buffered(), ParseContext()))
    }

    /**
     * Parse a template from a [String].
     */
    fun parse(template: String): Template = parse(StringReader(template))

    /**
     * Parse a named template using the partial resolution mechanism of this `Parser`.
     */
    fun parseByName(name: String): Template = parse(resolvePartial(name))

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
            it.template = parse(resolvePartial(name))
        }
    }

    private fun defaultResolvePartial(name: String): Reader {
        val extendedName = "$name.$extension"
        directoryPath?.let { path ->
            return Files.newBufferedReader(path.resolve(extendedName))
        }
        return directoryURL.toURI().resolve(extendedName).toURL().openStream().reader()
    }

    companion object {

        const val defaultExtension = "mustache"

        val currentDirectory = File(".")

        val extensionRegex = Regex("^[a-zA-Z0-9][a-zA-Z0-9-+#]*$")

        private val fileSystemCache = mutableMapOf<String, FileSystem>()

        fun derivePath(url: URL): Path? {
            val uri = url.toURI()
            return when (uri.scheme) {
                "jar" -> {
                    val schemeSpecific = uri.schemeSpecificPart
                    val jarName = schemeSpecific.substringAfter(':').substringBefore('!')
                    val fs = fileSystemCache[jarName] ?: FileSystems.newFileSystem(Paths.get(jarName), null).also {
                        fileSystemCache[jarName] = it
                    }
                    fs.getPath(schemeSpecific.substringAfter('!'))
                }
                "file" -> FileSystems.getDefault().getPath(uri.path)
                else -> null
            }
        }

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
