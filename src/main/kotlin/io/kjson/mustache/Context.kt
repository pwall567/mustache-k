/*
 * @(#) Context.kt
 *
 * mustache-k  Mustache template processor for Kotlin
 * Copyright (c) 2020, 2021, 2024 Peter Wall
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

import kotlin.reflect.KCallable
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.full.staticProperties

/**
 * The context for evaluation of a template element.
 *
 * @author  Peter Wall
 */
open class Context private constructor(val contextObject: Any?, private val parent: Context?) {

    constructor(contextObject: Any?) : this(contextObject, null)

    open fun resolve(name: String): Any? {
        val dotIndex = name.indexOf('.')
        return when {
            dotIndex == 0 && name.length == 1 -> contextObject
            dotIndex < 0 -> resolveName(contextObject, name) { parent?.resolve(name) }
            else -> {
                val outerName = name.substring(0, dotIndex)
                val outerObject = resolveName(contextObject, outerName) { parent?.resolve(outerName) }
                nestedResolve(outerObject, name.substring(dotIndex + 1))
            }
        }
    }

    private fun nestedResolve(sourceObject: Any?, name: String): Any? {
        val dotIndex = name.indexOf('.')
        return when {
            dotIndex < 0 -> resolveName(sourceObject, name) { null }
            else -> {
                val outerObject = resolveName(sourceObject, name.substring(0, dotIndex)) { null }
                nestedResolve(outerObject, name.substring(dotIndex + 1))
            }
        }
    }

    private fun resolveName(sourceObject: Any?, name: String, fallback: () -> Any?): Any? {
        when (sourceObject) {
            null -> return null
            is Map<*, *> -> {
                if (sourceObject.containsKey(name))
                    return sourceObject[name]
            }
            is Collection<*> -> {
                if (name == "size")
                    return sourceObject.size
                if (sourceObject is List<*>) {
                    try {
                        val index = name.toInt()
                        if (index in sourceObject.indices)
                            return sourceObject[index]
                    }
                    catch (_: NumberFormatException) {}
                }
            }
            else -> {
                val kClass = sourceObject::class
                kClass.memberProperties.find { it.isPublic() && it.name == name }?.let { return it.call(sourceObject) }
                kClass.staticProperties.find { it.isPublic() && it.name == name }?.let { return it.call() }
            }
        }
        return fallback()
    }

    fun child(contextObject: Any?) = Context(contextObject, this)

    fun iteratorChild(contextObject: Any?, first: Boolean, last: Boolean, index: Int, index1: Int) =
        object : Context(contextObject, this@Context) {
            override fun resolve(name: String): Any? = when (name) {
                "first" -> first
                "last" -> last
                "index" -> index
                "index1" -> index1
                else -> super.resolve(name)
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun enumChild(contextObject: Enum<*>) = object : Context(contextObject, this@Context) {
        val values = (contextObject::class.staticFunctions.find { it.name == "values" }?.call() as? Array<Enum<*>>?)?.
                map { it.name }
        override fun resolve(name: String): Any? {
            if (contextObject.name == name)
                return true
            if (values != null && name in values)
                return false
            return super.resolve(name)
        }
    }

    companion object {
        fun KCallable<*>.isPublic(): Boolean = visibility == KVisibility.PUBLIC
    }

}
