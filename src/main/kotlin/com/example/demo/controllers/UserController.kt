package com.example.demo.controllers

import com.example.demo.model.User
import com.example.demo.repositories.OrganizationRepository
import com.example.demo.repositories.UserRepository
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@CrossOrigin
@RequestMapping("/organization/{organizationId}/users")
class UserController(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository
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

    @GetMapping("/{userId}")
    fun find(
        @PathVariable organizationId: String,
        @PathVariable userId: String,
    ): User {
        return userRepository.findById(userId).orElseThrow { Exception() }
    }

}