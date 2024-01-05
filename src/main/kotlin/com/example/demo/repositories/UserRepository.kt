package com.example.demo.repositories

import com.example.demo.model.User
import com.example.demo.model.UserJustIDProjection
import com.example.demo.model.UserProjection
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : Neo4jRepository<User, String> {
    @Query(
        """
        MATCH (u:User{id: ${'$'}id})-[b:BELONGS_TO]->(o:Organization)
        OPTIONAL MATCH (u)-[sb:SUPERVISED_BY]->(s:User)
        RETURN u,collect(b),collect(o), collect(sb), collect(s)
    """
    )
    fun findProjectionByIdCustomQuery(id: String): Optional<UserProjection>

    @Query(
        """
        MATCH (u:User{id: ${'$'}id})-[b:BELONGS_TO]->(o:Organization)
        OPTIONAL MATCH (u)-[sb:SUPERVISED_BY]->(s:User)
        RETURN u,collect(b),collect(o), collect(sb), collect(s)
    """
    )
    fun findByIdCustomQuery(id: String): Optional<User>


    fun findProjectionById(id: String): Optional<UserProjection>

    @Query("""
        MATCH (u:User)
        WHERE u.id = ${'$'}id
        RETURN u
    """)
    fun findJustIdProjectionById(id: String): Optional<UserJustIDProjection>
}