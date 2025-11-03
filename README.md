# mustache-k

[![Build Status](https://github.com/pwall567/mustache-k/actions/workflows/build.yml/badge.svg)](https://github.com/pwall567/mustache-k/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v2.0.21&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v2.0.21)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/mustache-k?label=Maven%20Central)](https://central.sonatype.com/artifact/io.kjson/mustache-k)

[Mustache](https://mustache.github.io/mustache.5.html) template processor for Kotlin

Why is this included among a set of Kotlin JSON libraries?
Mustache is often used in conjunction with JSON &ndash; even if only for debugging &ndash; and it seemed like a good
fit.

This library also operates on structures created by the [`kjson-core`](https://github.com/pwall567/kjson-core) library,
and that library is therefore a dependency of this one.

The `mustache-k` library is derived from the earlier [`kotlin-mustache`](https://github.com/pwall567/kotlin-mustache)
library; most of the template functionality is the same but the handling of partial resolution is much improved.

- [Usage](#usage)
- [Reference](#reference)
- [Dependency Specification](#dependency-specification)


## Usage

Templates are loaded into memory using the `Parser` class, and there are several option for constructing a `Parser`,
depending on how the templates are stored.
Templates may invoke other templates (the term used by Mustache is &ldquo;Partial&rdquo;), and the Parser needs to have
a means of resolving the Partial by name.
(For simplicity, the term template is used here to refer to a template or a partial &ndash; in effect they are the
same.)

The `Parser` class has four alternative public constructors.

### Directory Specified by File

If the templates are held in a directory in the main file system, the Parser may be constructed with a `File` describing
the directory, as follows:
```kotlin
    val parser = Parser(File("/path/to/directory"))
```
Partial references will cause the parser to look for a file in the nominated directory.

### Directory Specified by Path

The `java.nio.file.Path` class may be used to specify the directory containing the templates:
```kotlin
    val parser = Parser(FileSystems.getDefault().getPath("/path/to/directory"))
```

### Directory Specified by URL

The `Parser` may also take a URL as a directory specifier.
The most familiar form of a URL is the web reference, for example `https://github.com/pwall567/mustache-k`, and the
parser will take a URL of this form and use it to create a specific locator for the template.

```kotlin
    val parser = Parser(URL("https://raw.githubusercontent.com/pwall567/mustache-k/main/src/test/resources/templates/"))
```
(Note the &lsquo;/&rsquo; on the end of the URL &ndash; this will cause this location to be regarded as a directory.)

There is also the `file:` form of URL, and the parser will take this type of URL instead of a file or path reference as
described above.

```kotlin
    val parser = Parser(URL("file:/path/to/directory/"))
```

But perhaps the most useful form of URL is the type returned by the `Class.getResource()` function &ndash; this will
look up the specified directory on the classpath, and return either a `file:` URL for a directory in the main file
system, or a `jar:` URL for the directory within the JAR file where the templates have been combined with the classes.
This means that a combined JAR can be created containing the program classes and the templates, and the same code will
read the templates regardless of whether they are in the main file system or in a JAR.

To use this type of URL:
```kotlin
    val parser = Parser(Resource.classPathURL("/templates") ?: throw RuntimeException("templates not found"))
```
The `throw` is required because the `getResource()` function may return null.

### Directory Specified by Custom Lambda

Finally, if none of the above lookup mechanisms are suitable, the `Parser` may be constructed with a lambda to resolve a
partial name to a `Reader` which is used to read the partial:
```kotlin
    val parser = Parser { name ->
        when (name) {
            "alpha" -> File("alpha.template").reader()
            "beta" -> File("beta.template").reader()
            else -> throw IllegalArgumentException("Unrecognised name - $name")
        }
    }
```

### Parser Functions

Having constructed a `Parser`, there are several functions to parse a template, taking a `String`, a `Reader`, an
`InputStream` or a `File`.

```kotlin
    val template = parser.parse(string)
```

```kotlin
    val template = parser.parse(reader)
```

```kotlin
    val template = parser.parse(inputStream)
```

```kotlin
    val template = parser.parse(file)
```

And to parse a template using the same naming system as for partials, using the directory specified in the constructor:
```kotlin
    val template = parser.parseByName(name)
```

When searching for partial definitions, regardless of how the directory is specified, the parser will add the extension
`mustache`.
To change this to use a different extension, use:
```kotlin
    parser.extension = "hbs"
```

The functions which read from an `InputStream` take an optional `Charset` parameter.
The parser will use a default of `Charsets.UTF_8` if the parameter is not supplied; to change this default:
```kotlin
    parser.charset = Charsets.ISO_8859_1
```

### Template functions

To render a template to a `String`:
```kotlin
    val resultString = template.render(contextObject)
```
The context object is the object that will be used to resolve variables and the controlling values of sections.

To render to an `Appendable` (this is often more efficient than rendering to a string since it avoids the creation of an
intermediate output object):
```kotlin
    template.renderTo(appendable, contextObject)
```

And from version 1.4, you can render to a non-blocking data stream:
```kotlin
    template.coRender(contextObject) { /* output to non-blocking channel */ }
```

The `Template` companion object also has functions to simplify template parsing, avoiding the need to create a `Parser`.
To parse a template from a `String`:
```kotlin
    val template = Template.parse("This is a template.\n")
```
From a `Reader`:
```kotlin
    val template = Template.parse(reader)
```
From an `InputStream`:
```kotlin
    val template = Template.parse(inputStream)
```
Or from a `File`:
```kotlin
    val template = Template.parse(File("/path/to/template"))
```
In this last case, a `Parser` will be created with the parent directory of the file as its location for partials.


## Reference

The reference [Mustache Specification](https://github.com/mustache/spec) contains the definitive description of the
Mustache template system.
This implementation does not include all features covered by the specification; the differences are detailed below.

A Mustache template is a block of text to be copied to the output, interspersed with &ldquo;tags&rdquo; that control the
substitution of values from a supplied object &ndash; the &ldquo;context object&rdquo;.
All text that is not part of a tag is copied without modification to the generated output.
The tags are described below.

### Tags

To distinguish tags from general text, the original authors chose the open brace (mustache) character.
Tags are opened by two left brace characters (`{{`) and closed by two right brace characters (`}}`).
(These delimiters may be changed if the use of these characters would cause a conflict &ndash; see
[below](#set-delimiter).)

#### Variables

The simplest tag type is the variable, which is replaced in the output by the result of resolving the variable name in
the context object.
If the context object is a `Map`, the variable name is used as a key to locate an entry; otherwise the variable name is
used to locate a property in the Kotlin object.
See [Name Resolution](#name-resolution) below for more detail.

For example, given the template:
```handlebars
Hello {{who}}
```

Then either:
```kotlin
    val contextObject = mapOf("who" to "World")
    template.processToString(contextObject)
```
Or:
```kotlin
    data class ContextObject(val who: String)

    val contextObject = ContextObject("World")
    template.processToString(contextObject)
```
Will cause the generated output to be:
```text
Hello World
```

If the name is not located &ndash; the key is not found in the map or the class does not have a variable with the
specified name &ndash; no text will be output.
The name `.` will select the entire context object.

By default, Mustache performs HTML escaping on the substituted text &ndash; that is, HTML special characters are
converted to the HTML entity references for those characters.
For example, `<` is converted to `&lt;` and `&` is converted to `&amp;`.

If this HTML escaping is not required, the literal form of the variable tag may be used &ndash; either `{{{name}}}`
(three braces instead of two) or `{{&name}}` (ampersand before the variable name).

The name may be a structured name &ndash; see [Name Resolution](#name-resolution) below.

#### Sections

A section is a block of text to be processed zero or more times depending on the value nominated by the section name.
Sections are introduced by a opening tag which has a `#` following the two left braces, and closed by a tag with a `/`
following the left braces, for example `{{#person}}...{{/person}}`.
Sections may be nested, and closing tags must match the most recent unmatched opening tag.

The content of the section is processed conditionally, depending on the type of the value (the value is determined using
[the rules detailed below](#name-resolution)):

- `Iterable` (e.g. `List`), `Array`: the content of the section is processed for each entry in the collection, with the
  individual entry used as the context object for the nested content
- `Map`: the content of the section is processed for each entry in the map, with the `Map.Entry` used as the context
  object for the nested content
- `Enum`: the content of the section is processed with the enum as the context object, allowing the individual enum
  values to be used to control nested sections (see [Enums](#enums) below)
- `Enum` values: the content of the section is processed if the context object is an enum with the specified value (see
  [Enums](#enums) below)
- `String` (and other forms of `CharSequence`): the content of the section is processed if the string is not empty
- `Int`, `Long`, `Short`, `Byte`, `Double`, `Float`, `BigInteger` and `BigDecimal`: the content of the section is
  processed if the value is non-zero
- `Boolean`: the content of the section is processed if the value is `true`
- anything else: the content of the section is processed if the value is not `null`, with the value used as the context
  object for the nested content (this is how properties of nested objects may be accessed)

When processing an `Iterable`, additional names are available in the context for each item processed:
- `first` (`Boolean`) &ndash; `true` if the item is the first in the collection
- `last` (`Boolean`) &ndash; `true` if the item is the last in the collection
- `index` (`Int`) &ndash; the index (zero-based) of the item in the collection
- `index1` (`Int`) &ndash; the index (one-based) of the item in the collection

When rendering to a non-blocking data stream, the section tag can iterate over a `Channel` or a `Flow`.
In this case, the rendering function will suspend until the next element becomes available, and will terminate only when
the `Channel` or `Flow` is closed.
The additional names in the context are available as for `Iterable`, but the `last` property will never be set to `true`
(since that would require lookahead to see if another element was going to be available).

#### Inverted Sections

An inverted section is a block of text to be processed once, if the value is null or the following:

- `Iterable` (`List` _etc._), `Array`, `Map`: if the collection or map is empty
- `Enum` values: if the enum does not have the specified value
- `String` _etc._: if the string is empty
- number types: if the number is zero
- `Boolean`: if the value is `false`

Inverted sections are introduced by a opening tag which has a `^` following the two left braces, and closed by a tag
with a `/` following the left braces.
Sections and inverted sections may nest within each other.

#### Partials

&ldquo;Partial&rdquo; is the term given to a nested section &ndash; for example a template in a separate file.
Partials are introduced by a tag with '>' following the two left braces &ndash; the remainder of the tag is the name of
an external file.

In the most common usage, the directory and extension of the first template will be stored and used as the directory and
extension for any partials encountered in that template.

Recursive data structures may be processed by recursive templates.

#### Set Delimiter

For cases where the standard double-brace delimiters clash with the text being processed, the delimiters may be changed
to some other combination of characters for the remainder of the current template.
A &ldquo;Set Delimiter&rdquo; tag consists of the current opening delimiter, immediately followed by an equals sign '=',
then the new opening delimiter, one or more spaces, the new closing delimiter, an equals sign '=' again, and finally the
current closing delimiter.

For example, to change the delimiters to `<%` and `%>`:
```
{{=<% %>=}}
```

The delimiters may not contain spaces or the equals sign.

### Name Resolution

The variable and section (including inverted section) tags use the following rules to determine the value to be used for
the tag:
- If the context object is `null`, the result value is `null`
- If the context object is a `Map` containing a key with the tag name, the value associated with that key is used
- If the context object has a property with the tag name, that property is used
- If the current context is a nested context (part of a section or inverted section), the context object of the outer
  context is searched using these same rules (repeatedly up to the outermost context)
- If nothing is found, `null` is returned

The variable or section may be specified in a structured form, e.g. `person.firstName`.
In this case, the above rules are used for the first part of the name (the part before the first dot), but for the
subsequent parts only the result of the first part is searched and the enclosing contexts do not form part of the
resolution process.

### Whitespace

This implementation treats all whitespace as significant, and copies it to the output.
This includes the newline at the end of a file, so if, for example, you have a partial to substitute a word or phrase
into the middle of a line, then to preserve line formatting the partial must not have a newline at the end of the file.

### Enums

One very powerful way in which this library differs from other Mustache implementations is in its handling of enums.
If an enum value is used in a [Section](#sections), the contents of the section are processed with the context object
for the nested content set to a special object that yields `true` for the name corresponding to the value of the enum,
and `false` for all other values.

For example:
```kotlin
    enum class Type { CREDIT, DEBIT }
    data class Transaction(val type: Type, val amount: Int)

    val template = Template.parse("{{#type}}{{#CREDIT}}+{{/CREDIT}}{{#DEBIT}}-{{/DEBIT}}{{/type}}{{&amount}}")
    println(template.render(Transaction(Type.DEBIT, 100))) // will print -100
```


## Dependency Specification

The latest version of the library is 3.13, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>mustache-k</artifactId>
      <version>3.13</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:mustache-k:3.13'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:mustache-k:3.13")
```

Peter Wall

2025-11-03
