package com.example.demo.extensions.json.jackson

import com.example.demo.JsonBuilder
import com.fasterxml.jackson.databind.node.ObjectNode

enum class AnyNode {
    AnyText, AnyNum, AnyBool, AnyObj, AnyArr
}

class JsonTestBuilder : JsonBuilder() {
    infix fun String.to(value: AnyNode) {
        prototype.putPOJO(this, value)
    }

    fun anyText() = AnyNode.AnyText
    fun anyNum() = AnyNode.AnyNum
    fun anyBool() = AnyNode.AnyBool
    fun anyObj() = AnyNode.AnyObj
    fun anyArr() = AnyNode.AnyArr
}


fun jsonWithAny(init: JsonTestBuilder.() -> Unit): ObjectNode =
    JsonTestBuilder().apply(init).build()
