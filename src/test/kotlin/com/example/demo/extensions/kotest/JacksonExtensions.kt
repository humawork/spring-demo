package com.huma.extensions.kotest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.node.*
import com.example.demo.extensions.json.jackson.AnyNode
import com.example.demo.extensions.json.jackson.AnyNode.*
import com.example.demo.extensions.kotest.shouldHaveSize
import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.*
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import org.assertj.core.internal.Failures
import java.util.UUID

fun beAnyTextNode() = object : Matcher<JsonNode> {
    override fun test(value: JsonNode) = MatcherResult(
        value.isTextual,
        { "Expected node to be textual, but was ${value.nodeType}" },
        { "Expected node to not be textual" }
    )
}

fun JsonNode.shouldBeAnyTextNode() = this should beAnyTextNode()

fun beAnyNumberNode() = object : Matcher<JsonNode> {
    override fun test(value: JsonNode) = MatcherResult(
        value.isNumber,
        { "Expected node to be number, but was ${value.nodeType}" },
        { "Expected node to not be number" }
    )
}

fun JsonNode.shouldBeAnyNumberNode() = this should beAnyNumberNode()

fun beAnyBooleanNode() = object : Matcher<JsonNode> {
    override fun test(value: JsonNode) = MatcherResult(
        value.isBoolean,
        { "Expected node to be boolean, but was ${value.nodeType}" },
        { "Expected node to not be boolean" }
    )
}

fun JsonNode.shouldBeAnyBooleanNode() = this should beAnyBooleanNode()

fun beAnyObjectNode() = object : Matcher<JsonNode> {
    override fun test(value: JsonNode) = MatcherResult(
        value.isObject,
        { "Expected node to be object, but was ${value.nodeType}" },
        { "Expected node to not be object" }
    )
}

fun JsonNode.shouldBeAnyObjectNode() = this should beAnyObjectNode()

fun beAnyArrayNode() = object : Matcher<JsonNode> {
    override fun test(value: JsonNode) = MatcherResult(
        value.isArray,
        { "Expected node to be array, but was ${value.nodeType}" },
        { "Expected node to not be array" }
    )
}

fun JsonNode.shouldBeAnyArrayNode() = this should beAnyArrayNode()

class FieldInJsonNodeMatcher(private val key: String) : Matcher<JsonNode> {
    override fun test(value: JsonNode) = MatcherResult(
        value.get(key) != null,
        { "Node should contain field for key $key" },
        { "Node should contain no field for key $key" }
    )
}

fun haveField(key: String) = FieldInJsonNodeMatcher(key)

fun notHaveField(key: String) = haveField(key).invert()

fun JsonNode.shouldHaveField(key: String, block: ((JsonNode) -> Unit)? = null) {
    this should haveField(key)
    block?.let { this[key].asClue(block) }
}

fun JsonNode.shouldNotHaveField(key: String, block: ((JsonNode) -> Unit)? = null) {
    val field = this[key]
    if (block != null && field != null) {
        field.asClue(block)
    } else {
        this should notHaveField(key)
    }
}

inline fun <reified T : JsonNode> JsonNode.shouldHaveTypedField(key: String, noinline block: ((T) -> Unit)? = null) {
    this should haveField(key)
    val field = this[key]
    field.shouldBeInstanceOf<T>()
    block?.let { field.asClue(block) }
}

fun JsonNode.shouldHaveTextField(key: String, block: ((String) -> Unit)? = null) {
    this should haveField(key)
    val field = this[key]
    field should beInstanceOf<TextNode>()
    block?.let { field.textValue().asClue(block) }
}

fun JsonNode.shouldHaveUUIDField(key: String, block: ((UUID) -> Unit)? = null) {
    this should haveField(key)
    val field = this[key]
    field should beInstanceOf<TextNode>()
    block?.let { UUID.fromString(field.textValue()).asClue(block) }
}

fun JsonNode.shouldHaveIntField(key: String, block: ((Int) -> Unit)? = null) {
    this should haveField(key)
    val field = this[key]
    field should beInstanceOf<NumericNode>()
    block?.let { field.intValue().asClue(block) }
}

fun JsonNode.shouldHaveDoubleField(key: String, block: ((Double) -> Unit)? = null) {
    this should haveField(key)
    val field = this[key]
    field should beInstanceOf<NumericNode>()
    block?.let { field.doubleValue().asClue(block) }
}

fun JsonNode.shouldHaveBooleanField(key: String, block: ((Boolean) -> Unit)? = null) {
    this should haveField(key)
    val field = this[key]
    field should beInstanceOf<BooleanNode>()
    block?.let { field.booleanValue().asClue(block) }
}

fun JsonNode.shouldHaveIdField(key: String, block: ((UUID) -> Unit)? = null) {
    this should haveField(key)
    val field = this[key]
    field should beInstanceOf<TextNode>()
    var asId: UUID? = null
    try {
        asId = UUID.fromString(field.textValue())!!
    } catch (e: Throwable) {
        Failures.instance().failure("Expected \"$key\" field to be coercible to UUID, but an exception was thrown.")
    }
    if (asId != null && block != null) {
        asId.asClue(block)
    }
}

infix fun ArrayNode.shouldContainItemWith(matcher: FieldNodeMatcher) {
    this should containItemWith(matcher)
}

fun containItemWith(matcher: FieldNodeMatcher) = FieldValueInJsonNodeMatcher(matcher)

data class FieldNodeMatcher(
    val key: String,
    val expected: JsonNode,
)

infix fun String.equalToNode(expected: JsonNode) = FieldNodeMatcher(this, expected)
infix fun String.equalToText(expected: String) = equalToNode(TextNode.valueOf(expected))

class FieldValueInJsonNodeMatcher(private val nodeMatcher: FieldNodeMatcher) : Matcher<JsonNode> {
    override fun test(value: JsonNode) = MatcherResult(
        value.map { it[nodeMatcher.key] }.contains(nodeMatcher.expected),
        { "Node should contain item with \"${nodeMatcher.key}\" equal to ${nodeMatcher.expected}" },
        { "Node should not contain item with \"${nodeMatcher.key}\" equal to ${nodeMatcher.expected}" }
    )
}

fun JsonNode.shouldMatchForAllFields(expected: JsonNode) {
    val actualFieldNames = this.fields().asSequence().map { "did not expect node to have a field by name" to it.key }
    val expectedFieldNames = expected.fields().asSequence().map { "expected node to have a field by name" to it.key }
    val mismatchedFieldNames = actualFieldNames.plus(expectedFieldNames)
        .groupBy { it.second } // every key present in both actual and expected forms a two-item list
        .filter { it.value.size == 1 } // filter so we get the keys only found in actual or expected
        .flatMap { it.value } // unwrap the grouping
        .map { "${it.first}: \"${it.second}\"" } // make'em presentable
    this.asClue {
        mismatchedFieldNames should object : Matcher<List<String>> {
            override fun test(value: List<String>) = MatcherResult(
                passed = value.isEmpty(),
                failureMessageFn = { value.joinToString("; ") },
                negatedFailureMessageFn = { "local singleton, never negated" }
            )
        }
    }
    this.shouldMatchValueFieldsAndCallbackForOthers(expected, JsonNode::shouldMatchForAllFields)
}

/** As [shouldMatchForAllFields] except will allow actual to contain fields not specified in expected */
fun JsonNode.shouldMatchForAllFieldsInExpected(expected: JsonNode) {
    this.shouldMatchValueFieldsAndCallbackForOthers(expected, JsonNode::shouldMatchForAllFieldsInExpected)
}

private fun JsonNode.shouldMatchValueFieldsAndCallbackForOthers(
    expected: JsonNode,
    recursion: JsonNode.(JsonNode) -> Unit,
) {
    assertSoftly {
        if (expected is POJONode && expected.pojo is AnyNode) {
            when (expected.pojo) {
                AnyText -> this.shouldBeAnyTextNode()
                AnyNum -> this.shouldBeAnyNumberNode()
                AnyBool -> this.shouldBeAnyBooleanNode()
                AnyArr -> this.shouldBeAnyArrayNode()
                AnyObj -> this.shouldBeAnyObjectNode()
            }
        } else {
            this::class shouldBe expected::class
            when (expected) {
                is ValueNode -> this.value() shouldBe expected.value()
                is ObjectNode -> {
                    expected.fields().forEach { expectedField ->
                        this.shouldHaveField(expectedField.key)
                        val actualField = this[expectedField.key]
                        if (actualField != null) {
                            "At key \"${expectedField.key}\"".asClue {
                                actualField.recursion(expectedField.value)
                            }
                        }
                    }
                }

                is ArrayNode -> {
                    val actualList = this.toList()
                    val expectedSize = expected.size()
                    if (actualList.size != expectedSize) {
                        "actual: ${this.prettyPrint()}\n\nexpected: ${expected.prettyPrint()}".asClue {
                            actualList.shouldHaveSize(expectedSize)
                        }
                    }
                    expected.forEachIndexed { index, expectedItem ->
                        val actualItem = this[index]
                        if (actualItem != null) {
                            "At index $index".asClue {
                                actualItem.recursion(expectedItem)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun JsonNode.shouldEqualJson(expected: JsonNode, options: CompareJsonOptions) {
    this.toPrettyString().shouldEqualJson(expected.toPrettyString(), options)
}

fun JsonNode.shouldContainJsonUnordered(expected: JsonNode) {
    this.shouldEqualJson(expected, CompareJsonOptions(propertyOrder = PropertyOrder.Lenient, arrayOrder = ArrayOrder.Lenient, fieldComparison = FieldComparison.Lenient))
}

private val prettyPrinter: ObjectWriter = ObjectMapper().writerWithDefaultPrettyPrinter()
private fun JsonNode.prettyPrint() = prettyPrinter.writeValueAsString(this)

private fun JsonNode.value() = when (this) {
    is BooleanNode -> booleanValue()
    is NumericNode -> numberValue()
    is MissingNode -> Unit
    is NullNode -> null
    is POJONode -> pojo
    is BinaryNode -> binaryValue()
    else -> textValue()
}
