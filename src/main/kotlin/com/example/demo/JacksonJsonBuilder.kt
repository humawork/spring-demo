package com.example.demo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

open class JsonBuilder {
    protected val prototype: ObjectNode = JsonNodeFactory.instance.objectNode()

    fun build(): ObjectNode = prototype.deepCopy()

    infix fun String.to(value: String?) {
        prototype.put(this, value)
    }

    infix fun String.to(value: Int?) {
        prototype.put(this, value)
    }

    infix fun String.to(value: Boolean?) {
        prototype.put(this, value)
    }

    infix fun String.to(value: JsonNode?) {
        prototype.set<JsonNode>(this, value)
    }

    infix fun String.to(value: UUID?) {
        prototype.put(this, value?.toString())
    }

    infix fun String.to(value: Double?) {
        prototype.put(this, value)
    }

    infix fun String.to(value: Float?) {
        prototype.put(this, value)
    }

    @Deprecated(
        message = """Key must be string. 
Value should be one of the types supported by the more specific JsonBuilder methods. 
This method will simply stringify both key and value, which is unlikely to be correct.""",
        replaceWith = ReplaceWith("to(value.toString())"),
        level = DeprecationLevel.ERROR
    )
    /** Created to shadow the Pair-constructing to-method in a [JsonBuilder] context,
     * and reporting it as an error to avoid unintentionally constructing a Pair
     * when you intended to build json. */
    infix fun Any?.to(value: Any?) {
        prototype.put(this.toString(), value.toString())
    }
}

fun json(init: JsonBuilder.() -> Unit = {}) = JsonBuilder().apply(init).build()

fun array(): ArrayNode = JsonNodeFactory.instance.arrayNode()

fun array(vararg items: JsonNode): ArrayNode = array().apply {
    repeat(items.count()) { add(items[it]) }
}

fun array(vararg items: Int): ArrayNode = array().apply {
    repeat(items.count()) { add(items[it]) }
}

fun array(vararg items: String): ArrayNode = array().apply {
    repeat(items.count()) { add(items[it]) }
}

fun array(items: List<String>): ArrayNode = array().apply {
    repeat(items.count()) { add(items[it]) }
}

fun array(items: Set<String>): ArrayNode = array().apply {
    repeat(items.count()) { add(items.toList()[it]) }
}

fun objectArray(items: List<JsonNode>): ArrayNode = array().apply {
    repeat(items.count()) { add(items[it]) }
}

fun JsonNode.toUUID(): UUID = UUID.fromString(this.textValue())