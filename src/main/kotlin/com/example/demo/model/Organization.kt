package com.example.demo.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node
class Organization(
    @Id @GeneratedValue(GeneratedValue.UUIDGenerator::class)
    val id: String,
    var name: String
)