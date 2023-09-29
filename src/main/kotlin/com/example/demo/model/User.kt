package com.example.demo.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship

@Node
class User(
    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator::class)
    var id: String,

    var givenName: String? = null,
    var familyName: String? = null,
) {

    @Relationship(type = "BELONGS_TO")
    lateinit var belongsTo: Organization

    @Relationship(type = "SUPERVISED_BY")
    var supervisedBy: User? = null

}