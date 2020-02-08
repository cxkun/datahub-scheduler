/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datahub.api.controller

import datahub.tools.Postman
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
@Suppress("UNCHECKED_CAST")
class LoginControllerTest {
    @Autowired
    lateinit var template: TestRestTemplate
    private lateinit var postman: Postman

    @BeforeAll
    fun rebuildDB() {
        SchemaUtils.rebuildDB()
        this.postman = Postman(template)
    }

    @Test
    fun login() {
        with(postman.post("/api/login", mapOf(
            "username" to "root",
            "password" to "root"
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, String>) {
                Assertions.assertEquals("root", Jwt.getUserName(get("token") ?: ""))
            }
        }

        with(postman.post("/api/login", mapOf(
            "username" to "root",
            "password" to "wrong password"
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("login failed", body?.get("error"))
        }
    }
}