package datahub.api.controller

import datahub.api.Postman
import datahub.api.auth.Jwt
import datahub.dao.SchemaUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginControllerTest {
    @Autowired
    lateinit var template: TestRestTemplate
    lateinit var postman: Postman

    @BeforeAll
    fun rebuildDB() {
        SchemaUtils.rebuildDB()
        this.postman = Postman(template)
    }

    @Test
    fun login() {
        val loginSuccess = postman.post("/api/login", mapOf(
            "username" to "root",
            "password" to "root"
        ))
        Assertions.assertEquals(loginSuccess.statusCode, HttpStatus.OK)
        val successBody = loginSuccess.body ?: mapOf()
        Assertions.assertEquals(successBody["status"], "success")
        val data = successBody["data"]
        Assertions.assertTrue(data != null)
        data as Map<String, String>
        val token = data["token"]
        Assertions.assertTrue(token is String)
        token as String
        Assertions.assertEquals(Jwt.getUserName(token), "root")


        val loginFailed = postman.post("/api/login", mapOf(
            "username" to "root",
            "password" to "roo"
        ))
        val failedBody = loginFailed.body ?: mapOf()
        Assertions.assertEquals(loginFailed.statusCode, HttpStatus.OK)
        Assertions.assertEquals(failedBody["status"], "failed")
        Assertions.assertEquals(failedBody["error"], "login failed")
    }


}