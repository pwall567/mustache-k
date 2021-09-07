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

class Parser private constructor(
    private val directoryPath: Path?,
    private val directoryURL: URL,
    val resolvePartial: Parser.(String) -> Reader
) {

    constructor(directory: File) : this(directory.toPath(), directory.toURI().toURL(), Parser::defaultResolvePartial)

    constructor(directoryPath: Path) : this(directoryPath, directoryPath.toUri().toURL(), Parser::defaultResolvePartial)

    constructor(directoryURL: URL) : this(derivePath(directoryURL), directoryURL, Parser::defaultResolvePartial)

    constructor(resolvePartial: Parser.(String) -> Reader = Parser::defaultResolvePartial) :
            this(currentDirectory.toPath(), currentDirectory.toURI().toURL(), resolvePartial)

    var extension = defaultExtension
        set(newExtension) {
            val trimmedExtension = if (newExtension.startsWith('.')) newExtension.drop(1) else newExtension
            if (!extensionRegex.containsMatchIn(trimmedExtension))
                throw MustacheParserException("Invalid extension - $trimmedExtension")
            field = trimmedExtension
        }

    private val partialCache = mutableMapOf<String, Partial>()

    fun parse(inputStream: InputStream): Template {
        return parse(inputStream.reader())
    }

    fun parse(reader: Reader): Template {
        return Template(parseNested(reader.buffered(), ParseContext()))
    }

    fun parse(template: String): Template = parse(StringReader(template))

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
        if (tag.endsWith('=') && tag.contains(' ')) {
            val strippedTag = tag.substring(1, tag.length - 1).trim()
            val open = strippedTag.substringBefore(' ')
            val close = strippedTag.substringAfter(' ').trim()
            if (open.isNotEmpty() && close.isNotEmpty())
                return context.copy(openDelimiter = open, closeDelimiter = close)
        }
        throw MustacheParserException("Incorrect delimiter tag")
    }

    fun addPartial(name: String, partial: Partial) {
        partialCache[name] = partial
    }

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

        val extensionRegex = Regex("^[0-9a-zA-Z]+$")

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

    // We want a resolvePartial function to take a partial name and resolve it against:
    // a. a Path representing a directory in a Zip file
    // b. a Path representing a directory in the main file system
    // c. an HTML URL

    // Do we need to set the directory? If we take the directory from the initial template file, we can just resolve
    // peer locations.

    // Probably still good to specify directory, and possibly specify initial template by name (like a partial)

    // Proposal:
    // Primary constructor takes URL
    // Secondary constructors take File or Path, and convert it to file: URL
    // Default constructor equivalent to File(".")
    // Initialiser takes the URL and creates Path - except in the case of http URL, which leaves Path as null
    // resolvePartial uses Path.resolveSibling if Path available, otherwise URL.resolve

}
