package com.example.demo.extensions.kotest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.inspectors.ElementFail
import io.kotest.inspectors.ElementPass
import io.kotest.inspectors.buildAssertionError
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import java.util.StringJoiner

infix fun <T> Iterable<T>.shouldHaveSize(size: Int) = this should haveSize(size)
fun <T> haveSize(size: Int): Matcher<Iterable<T>> = haveSizeMatcher(size)
fun <T> haveSizeMatcher(size: Int) = object : Matcher<Iterable<T>> {
    override fun test(value: Iterable<T>): MatcherResult {
        val count = value.count()
        return MatcherResult(
            count == size,
            { "Collection should have size $size but has size $count" },
            { "Collection should not have size $size" }
        )
    }
}

fun <T> Iterable<T>.shouldBeEmpty() = this should beEmpty()
fun <T> Iterable<T>.shouldNotBeEmpty() = this shouldNot beEmpty()
fun <T> beEmpty(): Matcher<Iterable<T>> = object : Matcher<Iterable<T>> {
    override fun test(value: Iterable<T>): MatcherResult {
        val count = value.count()
        return MatcherResult(
            count == 0,
            { "Node should be empty but contained $count children" },
            { "Node should not be empty" }
        )
    }
}

fun <T> Iterable<T>.forNone(f: (T) -> Unit) = forExactly(0, this, f)
fun <T> Iterable<T>.forExactly(k: Int, fn: (T) -> Unit) = forExactly(k, this, fn)
fun <T> forExactly(
    expectedPasses: Int,
    itemsUnderTest: Iterable<T>,
    itemTest: (T) -> Unit,
) {
    val results = runTests(itemsUnderTest, itemTest)
    val passed = results.filterIsInstance<ElementPass<T>>()
    if (passed.size != expectedPasses) {
        buildAssertionError("${passed.size} elements passed but expected $expectedPasses", results)
    }
}

fun <T> runTests(
    itemsUnderTest: Iterable<T>,
    itemTest: (T) -> Unit,
) = itemsUnderTest.map { item ->
    try {
        itemTest(item)
        ElementPass(item)
    } catch (e: Throwable) {
        ElementFail(item, e)
    }
}

infix fun <T, C : Iterable<T>> C.shouldContainExactlyInAnyOrder(expected: C) = this should containExactlyInAnyOrder(expected)
fun <T, C : Iterable<T>> containExactlyInAnyOrder(expected: C): Matcher<C> = object : Matcher<C> {
    override fun test(value: C): MatcherResult {
        val passed = value.count() == expected.count() && expected.all { value.contains(it) }
        return MatcherResult(
            passed,
            { "Collection should contain ${stringRepr(expected)} in any order, but was ${stringRepr(value)}" },
            { "Collection should not contain exactly ${stringRepr(expected)} in any order" }
        )
    }
}

private fun stringRepr(obj: Any?): String = when (obj) {
    is Float -> "${obj}f"
    is Long -> "${obj}L"
    is Char -> "'$obj'"
    is String -> "\"$obj\""
    is Array<*> -> obj.map { recursiveRepr(obj, it) }.toString()
    is BooleanArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is IntArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is ShortArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is FloatArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is DoubleArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is LongArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is ByteArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is CharArray -> obj.map { recursiveRepr(obj, it) }.toString()
    is ArrayNode -> "${obj::class.java.simpleName}${obj.map { recursiveRepr(obj, it) }}"
    is ObjectNode -> StringJoiner(", ", "{", "}").apply {
        for (field in obj.fields()) {
            add("${field.key}: ${recursiveRepr(obj, field.value)}")
        }
    }.toString()

    is JsonNode -> "${obj::class.java.simpleName}($obj)"
    is Iterable<*> -> obj.map { recursiveRepr(obj, it) }.toString()
    is Map<*, *> -> obj.map { (k, v) -> recursiveRepr(obj, k) to recursiveRepr(obj, v) }.toMap().toString()
    else -> obj.toString()
}

private fun recursiveRepr(root: Any, node: Any?): String {
    return if (root == node) "(this ${root::class.java.simpleName})" else stringRepr(node)
}