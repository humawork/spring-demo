package com.example.demo.controllers

import com.example.demo.model.Organization
import com.example.demo.repositories.OrganizationRepository
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@CrossOrigin
@RequestMapping("/organization")
class OrganizationController(
    private val organizationRepository: OrganizationRepository
) {

    @PostMapping("")
    fun create(): Organization {
        val org = Organization(UUID.randomUUID().toString(), "My Org")
        return organizationRepository.save(org)
    }

}