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

import ch.vorburger.mariadb4j.DB
import datahub.dao.SchemaUtils
import datahub.models.dtype.FileType
import datahub.tools.Postman
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("UNCHECKED_CAST")
class GroupControllerTest {

    @Autowired
    lateinit var template: TestRestTemplate
    private lateinit var postman: Postman

    @BeforeAll
    fun startDb(){
        DB.newEmbeddedDB(3307).start()
    }

    @BeforeEach
    fun initEnvironment() {
        SchemaUtils.rebuildDB()
        SchemaUtils.loadTable("datahub.users", this.javaClass.classLoader.getResource("tables/users.txt")!!.path)
        SchemaUtils.loadTable("datahub.groups", this.javaClass.classLoader.getResource("tables/groups.txt")!!.path)
        this.postman = Postman(template)
        this.postman.login()
    }

    @AfterAll
    fun cleanEnvironment() = SchemaUtils.rebuildDB()

    @Test
    fun listing() {
        val validGroupCount = 32
        with(postman.get("/api/group")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(validGroupCount, get("count"))
                val groups = get("groups") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(validGroupCount, groups.size)
            }
        }

        val pageSize = 7
        val queryTimes = validGroupCount / pageSize + 1
        val lastPageGroupCount = validGroupCount % pageSize
        for (page in 1..queryTimes) {
            with(postman.get("/api/group", mapOf("page" to page, "pageSize" to pageSize))) {
                Assertions.assertEquals(HttpStatus.OK, statusCode)
                Assertions.assertEquals("success", body?.get("status"))
                with(body?.get("data") as Map<String, Any>) {
                    Assertions.assertEquals(validGroupCount, get("count"))
                    val groups = get("groups") as List<LinkedHashMap<String, Any>>
                    Assertions.assertEquals(if (page == queryTimes) lastPageGroupCount else pageSize, groups.size)
                }
            }
        }
    }

    @Test
    fun find() {
        with(postman.get("/api/group/23")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val group = get("group") as LinkedHashMap<String, Any>
                Assertions.assertEquals("kcwhynhd", group["name"])
                Assertions.assertEquals("2044-11-11 15:27:26", group["createTime"])
                Assertions.assertEquals("2047-07-02 20:28:57", group["updateTime"])
            }
        }

        with(postman.get("/api/group/39")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("group 39 not found", body?.get("error"))
        }

        with(postman.get("/api/group/40")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("group 40 not found", body?.get("error"))
        }
    }

    @Test
    fun create() {
        postman.login("guest", "guest")

        val nextGroupId = 40
        val name = "test_create"

        with(postman.post("/api/group", mapOf("name" to name))
        ) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                with(get("group") as Map<String, Any>) {
                    Assertions.assertEquals(nextGroupId, get("id"))
                    Assertions.assertEquals(name, get("name"))
                }
                with(get("file") as Map<String, Any>) {
                    Assertions.assertEquals(nextGroupId, get("groupId"))
                    Assertions.assertEquals(2, get("ownerId"))
                    Assertions.assertEquals(name, get("name"))
                    Assertions.assertEquals(FileType.DIR.toString(), get("type"))
                    Assertions.assertEquals(null, get("version"))
                    Assertions.assertEquals(null, get("parentId"))
                }
            }
        }

        with(postman.get("/api/group/$nextGroupId")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val group = get("group") as LinkedHashMap<String, Any>
                Assertions.assertEquals(name, group["name"])
            }
        }

        // todo test file created

    }

    @Test
    fun update() {
        val newName = "new_name"

        with(postman.put("/api/group/25", mapOf("name" to newName))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("group 25 has been update", body?.get("message"))
        }

        with(postman.get("/api/group/25")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val group = get("group") as LinkedHashMap<String, Any>
                Assertions.assertEquals(newName, group["name"])
                Assertions.assertNotEquals("2051-07-10 20:16:48", group["updateTime"])
            }
        }
    }

    @Test
    fun remove() {
        with(postman.delete("/api/group/15")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("group 15 has been removed", body?.get("message"))
        }

        with(postman.get("/api/group/15")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("group 15 not found", body?.get("error"))
        }

        with(postman.delete("/api/group/18")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("group 18 not found", body?.get("error"))
        }

        with(postman.delete("/api/group/40")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("group 40 not found", body?.get("error"))
        }
    }
}