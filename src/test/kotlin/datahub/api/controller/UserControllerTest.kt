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

import datahub.api.auth.Jwt
import datahub.tools.Postman
import datahub.dao.SchemaUtils
import datahub.tools.eq
import org.junit.jupiter.api.*
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
class UserControllerTest {

    @Autowired
    lateinit var template: TestRestTemplate
    private lateinit var postman: Postman

    @BeforeEach
    fun initEnvironment() {
        SchemaUtils.rebuildDB()
        SchemaUtils.loadTable("datahub.users", this.javaClass.classLoader.getResource("tables/users.txt")!!.path)
        this.postman = Postman(template)
        this.postman.login()
    }

    @Test
    fun listing() {
        val validUserCount = 143
        with(postman.get("/api/user")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(validUserCount, get("count"))
                val users = get("users") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(validUserCount, users.size)
            }
        }

        val pageSize = 13
        val queryTimes = validUserCount / pageSize + 1
        val lastPageUserCount = validUserCount % pageSize
        for (page in 1..queryTimes) {
            with(postman.get("/api/user", mapOf("page" to page, "pageSize" to pageSize))) {
                Assertions.assertEquals(HttpStatus.OK, statusCode)
                Assertions.assertEquals("success", body?.get("status"))
                with(body?.get("data") as Map<String, Any>) {
                    Assertions.assertEquals(validUserCount, get("count"))
                    val users = get("users") as List<LinkedHashMap<String, Any>>
                    Assertions.assertEquals(if (page == queryTimes) lastPageUserCount else pageSize, users.size)
                }
            }
        }
    }

    @Test
    fun find() {
        with(postman.get("/api/user/66")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val user = get("user") as LinkedHashMap<String, Any>
                Assertions.assertEquals(true, setOf(8, 7, 3, 6, 2, 1) eq user["groupIds"])
                Assertions.assertEquals("WjWUMovObM", user["name"])
                Assertions.assertEquals("WjWUMovObM@139.com", user["email"])
                Assertions.assertEquals("2042-06-02 09:25:38", user["createTime"])
                Assertions.assertEquals("2043-01-26 13:59:27", user["updateTime"])
            }
        }

        with(postman.get("/api/user/67")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("user 67 not found", body?.get("error"))
        }
    }

    @Test
    fun create() {
        val nextUserId = 180
        val name = "test_create"
        val password = "test_password"
        val groupIds = setOf(131, 127)
        val email = "test_create@datahub.com"

        with(postman.post("/api/user", mapOf(
            "name" to name,
            "password" to password,
            "groupIds" to groupIds,
            "email" to email))
        ) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(nextUserId, get("userId"))
            }
        }

        with(postman.get("/api/user/$nextUserId")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val user = get("user") as LinkedHashMap<String, Any>
                Assertions.assertEquals(groupIds eq user["groupIds"], true)
                Assertions.assertEquals(name, user["name"])
                Assertions.assertEquals(email, user["email"])
            }
        }

        with(postman.post("/api/login", mapOf(
            "username" to name,
            "password" to password
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, String>) {
                Assertions.assertEquals(name, Jwt.getUserName(get("token") ?: ""))
            }
        }
    }

    @Test
    fun updateName() {
        val oldName = "root"
        val newName = "root_new_name"

        with(postman.put("/api/user/1", mapOf("name" to newName))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("user 1 has been update", body?.get("message"))
        }

        // old user name should login failed
        with(postman.post("/api/login", mapOf(
            "username" to oldName,
            "password" to "root"
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("login failed", body?.get("error"))
        }

        // new user name should login success
        with(postman.post("/api/login", mapOf(
            "username" to newName,
            "password" to "root"
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, String>) {
                Assertions.assertEquals(newName, Jwt.getUserName(get("token") ?: ""))
            }
        }
    }

    @Test
    fun updatePassword() {
        val oldPassword = "root"
        val newPassword = "root_new_password"

        with(postman.put("/api/user/1", mapOf("password" to newPassword))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("user 1 has been update", body?.get("message"))
        }

        // old password should login failed
        with(postman.post("/api/login", mapOf(
            "username" to "root",
            "password" to oldPassword
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("login failed", body?.get("error"))
        }

        // new password should login success
        with(postman.post("/api/login", mapOf(
            "username" to "root",
            "password" to newPassword
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, String>) {
                Assertions.assertEquals("root", Jwt.getUserName(get("token") ?: ""))
            }
        }
    }

    @Test
    fun updateEmail() {
        val newEmail = "new_email@datahub.com"

        with(postman.put("/api/user/2", mapOf("email" to newEmail))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("user 2 has been update", body?.get("message"))
        }

        with(postman.get("/api/user/2")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val user = get("user") as LinkedHashMap<String, Any>
                Assertions.assertEquals(newEmail, user["email"])
                Assertions.assertNotEquals("2042-03-23 08:54:17", user["updateTime"])
            }
        }
    }

    @Test
    fun updateGroupIds() {
        val newGroupIds = setOf(137, 149)

        with(postman.put("/api/user/2", mapOf("groupIds" to newGroupIds))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("user 2 has been update", body?.get("message"))
        }

        with(postman.get("/api/user/2")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val user = get("user") as LinkedHashMap<String, Any>
                Assertions.assertEquals(true, newGroupIds eq user["groupIds"])
                Assertions.assertNotEquals("2042-03-23 08:54:17", user["updateTime"])
            }
        }
    }

    @Test
    fun updateAll() {
        val newName = "new_name"
        val newPassword = "new_password"
        val newEmail = "new_email@datahub.com"
        val newGroupIds = setOf(137, 149)
        with(postman.put("/api/user/2", mapOf(
            "name" to newName,
            "password" to newPassword,
            "email" to newEmail,
            "groupIds" to newGroupIds
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("user 2 has been update", body?.get("message"))
        }

        with(postman.post("/api/login", mapOf(
            "username" to newName,
            "password" to newPassword
        ))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, String>) {
                Assertions.assertEquals(newName, Jwt.getUserName(get("token") ?: ""))
            }
        }

        with(postman.get("/api/user/2")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val user = get("user") as LinkedHashMap<String, Any>
                Assertions.assertEquals(true, newGroupIds eq user["groupIds"])
                Assertions.assertEquals(newEmail, user["email"])
                Assertions.assertNotEquals("2042-03-23 08:54:17", user["updateTime"])
            }
        }
    }

    @Test
    fun updateInvalidUser() {
        with(postman.put("/api/user/4", mapOf("name" to "user who has been remove"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("user 4 not found", body?.get("error"))
        }

        with(postman.put("/api/user/180", mapOf("name" to "user not exists"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("user 180 not found", body?.get("error"))
        }
    }

    @Test
    fun remove() {
        with(postman.delete("/api/user/2")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("user 2 has been removed", body?.get("message"))
        }

        with(postman.get("/api/user/2")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("user 2 not found", body?.get("error"))
        }

        with(postman.delete("/api/user/4")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("user 4 not found", body?.get("error"))
        }

        with(postman.delete("/api/user/180")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("user 180 not found", body?.get("error"))
        }

    }
}