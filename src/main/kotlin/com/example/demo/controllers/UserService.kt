package com.example.demo.controllers

import com.example.demo.model.*
import com.example.demo.repositories.OrganizationRepository
import com.example.demo.repositories.UserRepository
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Service
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val neo4jTemplate: Neo4jTemplate,
) {

    fun create(
        organizationId: String,
        body: UserInput,
    ): User {
        val organization = organizationRepository.findById(organizationId).orElseThrow { Exception() }
        val user = User(UUID.randomUUID().toString(), body.givenName, body.familyName)
        user.belongsTo = organization

        body.supervisorId?.let { supervisorId ->
            val supervisor = userRepository.findById(supervisorId).orElseThrow { Exception() }
            user.supervisedBy = supervisor
        }

        return userRepository.save(user)
    }

    fun updateWholeUser(
        organizationId: String,
        userId: String,
        updateRequest: UserInput,
    ): UserProjection {
        val existingUser = userRepository.findByIdCustomQuery(userId).get()
        println("after findProjectionById")
        val user = User(
            userId,
            existingUser.givenName,
            existingUser.familyName,
        )
        user.belongsTo = existingUser.belongsTo
        user.supervisedBy = existingUser.supervisedBy?.id?.let { User(it) }
        updateRequest.givenName?.let { user.givenName = it }
        updateRequest.familyName?.let { user.familyName = it }
        updateRequest.supervisorId?.let {
            val newSupervisor = userRepository.findByIdCustomQuery(it).get()
            println("after findByIdCustomQuery")
            user.supervisedBy = newSupervisor
        }
        return neo4jTemplate.saveAs(user, UserProjection::class.java)
    }

    fun updateWithProjectionsNoRelations(
        organizationId: String,
        userId: String,
        body: UserInput,
//    ): UserProjectionNoRelations {
    ): User {
        val projection = userRepository.findProjectionById(userId).orElseThrow { Exception() }
        body.givenName?.let { projection.givenName = it }
        body.familyName?.let { projection.familyName = it }
        val user = User(projection.id, projection.givenName, projection.familyName)
//        return neo4jTemplate.saveAs(user, UserProjectionNoRelations::class.java)
        println("before saveas")
        return neo4jTemplate.saveAs(user) { _, property -> !property.isRelationship }
//        return neo4jTemplate.save(user)
    }

    fun createWithProjectionsA(
        organizationId: String,
        body: UserInput,
    ): UserProjection {
        val organization = organizationRepository.findById(organizationId).orElseThrow { Exception() }
        val user = User(UUID.randomUUID().toString(), body.givenName, body.familyName)
        user.belongsTo = organization

        val createdUser = userRepository.save(user)
        var createdUserProjection = userRepository.findProjectionById(createdUser.id).orElseThrow { Exception() }

        val supervisorId = body.supervisorId
        if (supervisorId != null) {
            val supervisorProjection = userRepository.findJustIdProjectionById(supervisorId).orElseThrow { Exception() }
            createdUserProjection.supervisedBy = supervisorProjection
            createdUserProjection = neo4jTemplate.save(User::class.java).one(createdUserProjection)
        }

        return createdUserProjection
    }

    fun createWithProjectionB(
        organizationId: String,
        body: UserInput,
    ): UserProjection {
        val organization = organizationRepository.findById(organizationId).orElseThrow { Exception() }
        val supervisorId = body.supervisorId
        val supervisor = supervisorId?.let {
            userRepository.findJustIdProjectionById(supervisorId).orElseThrow { Exception() }
        }
        val user = object : UserProjection {
            override var id = UUID.randomUUID().toString()
            override var givenName = body.givenName
            override var familyName = body.familyName
            override var belongsTo = organization
            override var supervisedBy = supervisor
        }

        return neo4jTemplate.save(User::class.java).one(user)
    }

    fun createWithProjectionC(
        organizationId: String,
        body: UserInput,
    ): UserDTOProjection {
        val organization = organizationRepository.findById(organizationId).orElseThrow { Exception() }
        val supervisorId = body.supervisorId
        val supervisorDTO = supervisorId?.let {
            val supervisor = userRepository.findJustIdProjectionById(supervisorId).orElseThrow { Exception() }
            UserJustIDDTOProjection(supervisor.id)
        }
        val user = UserDTOProjection(
            id = UUID.randomUUID().toString(),
            givenName = body.givenName,
            familyName = body.familyName,
            belongsTo = organization,
            supervisedBy = supervisorDTO,
        )

        return neo4jTemplate.save(User::class.java).one(user)
    }

    fun createWithProjectionD(
        organizationId: String,
        body: UserInput,
    ): UserProjection {
        val organization = organizationRepository.findById(organizationId).orElseThrow { Exception() }
        val supervisorId = body.supervisorId
        val supervisorDTO = supervisorId?.let {
            userRepository.findById(supervisorId)
//            val supervisor = userRepository.findJustIdProjectionById(supervisorId).orElseThrow { Exception() }
//            UserJustIDDTOProjection(supervisor.id)
        }
//        val projection = UserDTOProjection(
//            id = UUID.randomUUID().toString(),
//            givenName = body.givenName,
//            familyName = body.familyName,
//            belongsTo = organization,
//            supervisedBy = supervisorDTO,
//        )
        val user = User(
            UUID.randomUUID().toString(),
            body.givenName,
            body.familyName,
        )
        user.belongsTo = organization
        user.supervisedBy = supervisorDTO?.getOrNull()


        return neo4jTemplate.saveAs(user, UserProjection::class.java)
    }

    fun findUserUsingCrudRepository(userId: UUID): User = userRepository.findById(userId.toString()).get()

    fun findProjection(
        organizationId: String,
        userId: String,
    ): UserProjection? {
        return userRepository.findProjectionById(userId).orElseThrow { Exception() }
    }

    fun findProjectionCustomQuery(
        organizationId: String,
        userId: String,
    ): UserProjection? {
        return userRepository.findProjectionByIdCustomQuery(userId).orElseThrow { Exception() }
    }

}