package com.example.demo.controllers

import com.example.demo.model.User
import com.example.demo.model.UserDTOProjection
import com.example.demo.model.UserJustIDDTOProjection
import com.example.demo.model.UserProjection
import com.example.demo.repositories.OrganizationRepository
import com.example.demo.repositories.UserRepository
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@CrossOrigin
@RequestMapping("/organization/{organizationId}/users")
class UserController(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val neo4jTemplate: Neo4jTemplate,
) {

    @PostMapping("")
    fun create(
        @PathVariable organizationId: String,
        @RequestBody body: UserInput,
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

    @PutMapping("/withprojections/a/{userId}")
    fun updateWithProjectionsA(
        @PathVariable organizationId: String,
        @PathVariable userId: String,
        @RequestBody body: UserInput,
    ): UserProjection {
        val user = userRepository.findProjectionById(userId).orElseThrow { Exception() }
        body.givenName?.let { user.givenName = it }
        body.familyName?.let { user.familyName = it }
        return neo4jTemplate.save(User::class.java).one(user)
    }

    @PostMapping("/withprojections/a")
    fun createWithProjectionsA(
        @PathVariable organizationId: String,
        @RequestBody body: UserInput,
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

    @PostMapping("/withprojections/b")
    fun createWithProjectionB(
        @PathVariable organizationId: String,
        @RequestBody body: UserInput,
    ): UserProjection {
        val organization = organizationRepository.findById(organizationId).orElseThrow { Exception() }
        val supervisorId = body.supervisorId
        val supervisor = supervisorId?.let {
            userRepository.findJustIdProjectionById(supervisorId).orElseThrow { Exception() }
        }
        val user = object: UserProjection {
            override var id = UUID.randomUUID().toString()
            override var givenName = body.givenName
            override var familyName = body.familyName
            override var belongsTo = organization
            override var supervisedBy = supervisor
        }

        return neo4jTemplate.save(User::class.java).one(user)
    }

    @PostMapping("/withprojections/c")
    fun createWithProjectionC(
        @PathVariable organizationId: String,
        @RequestBody body: UserInput,
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

    @GetMapping("/{userId}")
    fun find(
        @PathVariable organizationId: String,
        @PathVariable userId: String,
    ): User {
        return userRepository.findById(userId).orElseThrow { Exception() }
    }

}