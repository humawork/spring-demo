package com.example.demo

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.HttpStatus.OK
import java.util.UUID
import kotlin.reflect.KClass

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {
    private val restTemplate = TestRestTemplate()

    @LocalServerPort
    private val port: Int = 0

    @Test
    fun createOrganization() {
        "/organization".POST(
            json {
                "name" to "MyOrg"
            }
        ).asClue { response ->
            response.statusCode shouldBe OK
        }
    }

    @Test
    fun createOrganizationWithUsers() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request

        val olaId = createUser(organizationId, "Ola", "Nordmann") // 6 cypher requests
        val kariId = createUser(organizationId, "Kari", "Nordmann", supervisor = olaId) // 15 cypher requests
        val hansId = createUser(organizationId, "Hans", "Nordmann", supervisor = kariId) // 22 cypher requests
        val siriId = createUser(organizationId, "Siri", "Nordmann", supervisor = hansId) // 29 cypher requests

        findUser(organizationId, siriId) // 10 cypher requests

        println("Done")
    }

    private fun findUser(organizationId: UUID, userId: UUID): JsonNode {
        return "/organization/$organizationId/users/$userId".GET().asClue { response ->
            response.statusCode shouldBe OK
            response.body!!
        }
    }

    private fun createUser(organizationId: UUID, givenName: String, familyName: String, supervisor: UUID? = null): UUID {
        return "/organization/$organizationId/users".POST(
            json {
                "givenName" to givenName
                "familyName" to familyName
                "supervisorId" to supervisor
            }
        ).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        }
    }

    @Suppress("TestFunctionName")
    fun String.GET(accessToken: String? = null) = request(
        method = HttpMethod.GET,
        path = this,
        responseType = JsonNode::class,
    )

    @Suppress("TestFunctionName")
    fun <T : Any> String.GET(responseType: KClass<T>, accessToken: String? = null) = request(
        method = HttpMethod.GET,
        path = this,
        responseType = responseType,
    )

    @Suppress("TestFunctionName")
    fun String.PUT(body: Any? = null, accessToken: String? = null) = request(
        method = HttpMethod.PUT,
        path = this,
        input = body,
        responseType = JsonNode::class,
    )

    @Suppress("TestFunctionName")
    fun String.POST(body: Any? = null, accessToken: String? = null) = request(
        method = HttpMethod.POST,
        path = this,
        input = body,
        responseType = JsonNode::class,
    )

    @Suppress("TestFunctionName")
    fun String.PATCH(body: Any? = null, accessToken: String? = null) = request(
        method = HttpMethod.PATCH,
        path = this,
        input = body,
        responseType = JsonNode::class,
    )

    private val urlBase by lazy { "http://localhost:$port" }
    private fun <T : Any> request(
        method: HttpMethod,
        path: String,
        input: Any? = null,
        responseType: KClass<T>,
        contentType: MediaType = MediaType.APPLICATION_JSON,
    ): ResponseEntity<T> {
        val headers = HttpHeaders()
        headers.contentType = contentType
        return restTemplate.exchange("$urlBase$path", method, HttpEntity(input, headers), responseType.java)
    }
}
