package com.huma.extensions.kotest

import io.kotest.assertions.asClue
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf

inline fun <reified T : Any> Any?.shouldBeInstanceOfAndClue(block: (T) -> Unit) {
    this.shouldBeInstanceOf<T>()
    this.asClue(block)
}

fun <T> T?.shouldNotBeNull(block: (T) -> Unit) {
    this.shouldNotBeNull()
    this.asClue(block)
}

fun List<String>.shouldBeSortedAlphabetically() {
    this should AlphabeticSortingMatcher
}

private object AlphabeticSortingMatcher : Matcher<List<String>> {
    override fun test(value: List<String>) = MatcherResult(
        value.zipWithNext { a, b -> a <= b }.all { it },
        { "List should be sorted alphabetically: $value" },
        { "List should not be sorted alphabetically: $value" }
    )
}
