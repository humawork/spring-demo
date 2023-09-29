package com.example.demo.repositories

import com.example.demo.model.Organization
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRepository : Neo4jRepository<Organization, String>