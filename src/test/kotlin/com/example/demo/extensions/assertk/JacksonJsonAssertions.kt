package com.example.demo.extensions.assertk

import assertk.Assert
import assertk.assertions.extracting
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode

fun Assert<JsonNode>.hasFieldWithValue(expectedKey: String, expectedValue: String) = given {
    if (it.path(expectedKey).textValue() != expectedValue) {
        expected("node to have text field at ${show(expectedKey)} with value ${show(expectedValue)} \n${showJson(it)}")
    }
}

fun Assert<ArrayNode>.containsNodeWithValue(expectedValue: String) = given {
    if (it.none { node -> node.textValue() == expectedValue }) {
        expected("array node to contain a node with value ${show(expectedValue)} but was \n${showJson(it)}")
    }
}

fun Assert<JsonNode>.extractingText(key: String) = extracting { it[key].textValue() }

fun Assert<JsonNode>.hasSize(expected: Int) = hasChildren(expected)

fun Assert<JsonNode>.hasChildren(expected: Int) = given {
    val actual = it.size()
    if (actual != expected) {
        expected("node to have exactly ${show(expected)} children but it has ${show(actual)}")
    }
}

private val prettyWriter = ObjectMapper().writerWithDefaultPrettyPrinter()

private fun showJson(it: JsonNode) = "(${prettyWriter.writeValueAsString(it)})"