package com.example.demo

import com.example.demo.controllers.UserInput
import com.example.demo.controllers.UserService
import com.example.demo.model.UserProjection
import com.fasterxml.jackson.databind.JsonNode
import com.huma.extensions.kotest.shouldHaveTextField
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.HttpStatus.OK
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*
import kotlin.reflect.KClass

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {
    private val restTemplate = TestRestTemplate()

    private val printLogs = false

    @Autowired
    private lateinit var userService: UserService

    @LocalServerPort
    private val port: Int = 0

    lateinit var printStream: PrintStream
    private val outputStream = ByteArrayOutputStream()
    private val originalStream: PrintStream = System.out

    @BeforeEach
    fun setUp() {
        outputStream.reset()
        printStream = PrintStream(outputStream)
        System.setOut(printStream)
    }

    @AfterEach
    fun tearDown() {
        countAndPrintDebugLogs("teardown", true)
    }

    private fun resetOutput() {
        outputStream.reset()
    }

    private fun countAndPrintDebugLogs(description: String, printLogs: Boolean = this.printLogs) {
        val text = ByteArrayInputStream(outputStream.toByteArray()).bufferedReader().readText()
        val logs = ByteArrayInputStream(outputStream.toByteArray()).bufferedReader().readLines()
        System.setOut(originalStream)
        if (printLogs) println("QUERIES: \n$text")
        println("$description -> queries: ${logs.count { it.contains(" DEBUG ") }}")
        println("------------------------------")
        originalStream.flush()
        outputStream.flush()
        outputStream.reset()
        System.setOut(printStream)
    }

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

        countAndPrintDebugLogs("after setting up")
        val user = userService.findUserUsingCrudRepository(siriId) // 10 cypher requests
        println(user)
        countAndPrintDebugLogs("find user using crud repository")

        // projection is fully filled but 4 queries are fired
        val projection = userService.findProjection(organizationId.toString(), siriId.toString())
        countAndPrintDebugLogs("find user using projection", true)
        println("using projection: ${string(projection!!)}")

        // projection is filled fully when custom query is used (@Query antruenotation)
        val projectionCustomQuery = userService.findProjectionCustomQuery(organizationId.toString(), siriId.toString())
        countAndPrintDebugLogs("find user using projection and custom query", )
        println("using projection with custom query: ${string(projectionCustomQuery!!)}")
        string(projection) shouldBe string(projectionCustomQuery)
    }

    fun string(it: UserProjection): String {
        return "UserProjection(id='${it.id}', givenName=${it.givenName}," +
                " familyName=${it.familyName}, belongsTo=${it.belongsTo.id}," +
                " supervisedBy=${it.supervisedBy?.id})"
    }

    @Test
    fun mutateRelationshipUsingProjection() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request

        val olaId = createUser(organizationId, "Ola", "Nordmann") // 6 cypher requests
        val kariId = createUser(organizationId, "Kari", "Nordmann", supervisor = olaId) // 15 cypher requests
        val hansId = createUser(organizationId, "Hans", "Nordmann", supervisor = kariId) // 22 cypher requests
        val siriId = createUser(organizationId, "Siri", "Nordmann", supervisor = hansId) // 29 cypher requests

        resetOutput()
        val userBeforeUpdate = userService.findUserUsingCrudRepository(siriId)
        println(userBeforeUpdate)
        countAndPrintDebugLogs("find user using crud repository")

        userService.updateWholeUser(
            organizationId.toString(),
            siriId.toString(),
            UserInput(supervisorId = kariId.toString())
        )
        countAndPrintDebugLogs("update supervisor")

        val userAfterUpdate = userService.findUserUsingCrudRepository(siriId)
        println(userAfterUpdate)
        countAndPrintDebugLogs("find user using crud repository")
        userAfterUpdate.belongsTo.id shouldBe organizationId.toString()
        userAfterUpdate.supervisedBy!!.id shouldBe kariId.toString()
        userAfterUpdate.givenName!! shouldBe userBeforeUpdate.givenName
        userAfterUpdate.familyName!! shouldBe userBeforeUpdate.familyName
        userAfterUpdate.supervisedBy!!.supervisedBy!!.id shouldBe olaId.toString()
    }

    @Test
    fun mutateWholeUserUsingProjection() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request

        val olaId = createUser(organizationId, "Ola", "Nordmann") // 6 cypher requests
        val kariId = createUser(organizationId, "Kari", "Nordmann", supervisor = olaId) // 15 cypher requests
        val hansId = createUser(organizationId, "Hans", "Nordmann", supervisor = kariId) // 22 cypher requests
        val siriId = createUser(organizationId, "Siri", "Nordmann", supervisor = hansId) // 29 cypher requests

        resetOutput()
        var user = userService.findUserUsingCrudRepository(siriId)
        println(user)
        countAndPrintDebugLogs("find user using crud repository")

        val newGivenName = "new"
        val newFamilyName = "name"
        userService.updateWholeUser(
            organizationId.toString(),
            siriId.toString(),
            UserInput(
                givenName = newGivenName,
                familyName = newFamilyName,
                supervisorId = kariId.toString()
            )
        )
        countAndPrintDebugLogs("update user")

        user = userService.findUserUsingCrudRepository(siriId)
        println(user)
        countAndPrintDebugLogs("find user using crud repository")
        user.belongsTo.id shouldBe organizationId.toString()
        user.supervisedBy!!.id shouldBe kariId.toString()
        user.givenName shouldBe newGivenName
        user.familyName shouldBe newFamilyName
    }

    @Test
    fun shouldNotChangeAnyFieldsIfNotUpdated() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request

        val olaId = createUser(organizationId, "Ola", "Nordmann") // 6 cypher requests
        val kariId = createUser(organizationId, "Kari", "Nordmann", supervisor = olaId) // 15 cypher requests
        val hansId = createUser(organizationId, "Hans", "Nordmann", supervisor = kariId) // 22 cypher requests
        val siriId = createUser(organizationId, "Siri", "Nordmann", supervisor = hansId) // 29 cypher requests

        countAndPrintDebugLogs("after setting up")
        var user = userService.findUserUsingCrudRepository(siriId)
        println(user)
        countAndPrintDebugLogs("find user using crud repository")

        val newGivenName = "new"
        val newFamilyName = "name"
        userService.updateWholeUser(
            organizationId.toString(),
            siriId.toString(),
            UserInput(
                givenName = newGivenName,
                familyName = newFamilyName
            )
        )
        countAndPrintDebugLogs("update supervisor")

        user = userService.findUserUsingCrudRepository(siriId)
        println(user)
        countAndPrintDebugLogs("find user using crud repository")
        user.belongsTo.id shouldBe organizationId.toString()
        user.supervisedBy!!.id shouldBe hansId.toString()
        user.givenName shouldBe newGivenName
        user.familyName shouldBe newFamilyName
    }

    @Test
    fun mutateUsersUsingProjections() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        }

        val kariId = createUser(organizationId, "Kari", "Nordmann") // 15 cypher requests
        val olaId = createUser(organizationId, "Ola", "Ola", supervisor = kariId)
        println("saved Ola: ${findUser(organizationId, olaId).toPrettyString()}")

        countAndPrintDebugLogs("before update")
        userService.updateWithProjectionsNoRelations(
            organizationId.toString(),
            olaId.toString(),
            UserInput(familyName = "Nordmann")
        )
        countAndPrintDebugLogs("after update")

        val ola = findUser(organizationId, olaId)

        ola.shouldHaveTextField("familyName") { it shouldBe "Nordmann" }
        println(ola.toPrettyString())
        countAndPrintDebugLogs("after")
    }

    @Test
    @Disabled
    fun createOrganizationWithUsersUsingProjectionsA() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request

        val olaId = createUserWithProjection("a", organizationId, "Ola", "Nordmann")
        val kariId = createUserWithProjection("a", organizationId, "Kari", "Nordmann", supervisor = olaId)
        val hansId = createUserWithProjection("a", organizationId, "Hans", "Nordmann", supervisor = kariId)
        val siriId = createUserWithProjection("a", organizationId, "Siri", "Nordmann", supervisor = hansId)

        findUser(organizationId, siriId)

        println("Done")
    }

    @Test
    @Disabled
    fun createOrganizationWithUsersUsingProjectionsB() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request

        val olaId = createUserWithProjection("b", organizationId, "Ola", "Nordmann")
        val kariId = createUserWithProjection("b", organizationId, "Kari", "Nordmann", supervisor = olaId)
        val hansId = createUserWithProjection("b", organizationId, "Hans", "Nordmann", supervisor = kariId)
        val siriId = createUserWithProjection("b", organizationId, "Siri", "Nordmann", supervisor = hansId)

        findUser(organizationId, siriId)

        println("Done")
    }

    @Test
    @Disabled
    fun createOrganizationWithUsersUsingProjectionsC() {
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request

        val olaId = createUserWithProjection("c", organizationId, "Ola", "Nordmann")
        val kariId = createUserWithProjection("c", organizationId, "Kari", "Nordmann", supervisor = olaId)
        val hansId = createUserWithProjection("c", organizationId, "Hans", "Nordmann", supervisor = kariId)
        val siriId = createUserWithProjection("c", organizationId, "Siri", "Nordmann", supervisor = hansId)

        findUser(organizationId, siriId)

        println("Done")
    }

    @Test
    fun createOrganizationWithUsersUsingProjectionD() {
        countAndPrintDebugLogs("before")
        val organizationId = "/organization".POST(json { "name" to "MyOrg" }).asClue { response ->
            response.statusCode shouldBe OK
            response.body!!.path("id").toUUID()
        } // 1 cypher request
        countAndPrintDebugLogs("after create org ")

        val projectionApproach = "d"
        val olaId = createUserWithProjection(projectionApproach, organizationId, "Ola", "Nordmann")
        countAndPrintDebugLogs("after create user 1")
        val kariId =
            createUserWithProjection(projectionApproach, organizationId, "Kari", "Nordmann", supervisor = olaId)
        countAndPrintDebugLogs("after create user 2")
        val hansId =
            createUserWithProjection(projectionApproach, organizationId, "Hans", "Nordmann", supervisor = kariId)
        countAndPrintDebugLogs("after create user 3")
        val siriId =
            createUserWithProjection(projectionApproach, organizationId, "Siri", "Nordmann", supervisor = hansId)
        countAndPrintDebugLogs("after create user 4")

        val user = findUser(organizationId, siriId)
        countAndPrintDebugLogs("after find user")

        val user2 = findUserUsingProjection(organizationId, siriId)
        countAndPrintDebugLogs("after find user 2")


        println(user.toPrettyString())
        println(user2)
        println("Done")
    }

    private fun findUser(organizationId: UUID, userId: UUID): JsonNode {
        return "/organization/$organizationId/users/$userId".GET().asClue { response ->
            response.statusCode shouldBe OK
            response.body!!
        }
    }

    private fun findUserUsingProjection(organizationId: UUID, userId: UUID): UserProjection? {
        return userService.findProjection(organizationId.toString(), userId.toString())
    }

    private fun createUser(
        organizationId: UUID,
        givenName: String,
        familyName: String,
        supervisor: UUID? = null
    ): UUID {
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

    private fun createUserWithProjection(
        projectionApproach: String,
        organizationId: UUID,
        givenName: String,
        familyName: String,
        supervisor: UUID? = null
    ): UUID {
        val userInput = UserInput(givenName, familyName, supervisor?.toString())
        val orgId = organizationId.toString()
        val id = when (projectionApproach) {
            "a" -> userService.createWithProjectionsA(orgId, userInput).id
            "b" -> userService.createWithProjectionB(orgId, userInput).id
            "c" -> userService.createWithProjectionC(orgId, userInput).id
            "d" -> userService.createWithProjectionD(orgId, userInput).id
            else -> throw IllegalStateException()
        }
        return UUID.fromString(id)
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
