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
import datahub.tools.Postman
import datahub.dao.SchemaUtils
import datahub.models.dtype.FileType
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
class FileControllerTest {

    @Autowired
    lateinit var template: TestRestTemplate
    private lateinit var postman: Postman

    @BeforeAll
    fun startDb() {
        DB.newEmbeddedDB(3307).start()
    }


    @BeforeEach
    fun rebuildDB() {
        SchemaUtils.rebuildDB()
        SchemaUtils.loadTable("datahub.users", this.javaClass.classLoader.getResource("tables/users.txt")!!.path)
        SchemaUtils.loadTable("datahub.files", this.javaClass.classLoader.getResource("tables/files.txt")!!.path)
        this.postman = Postman(template)
        this.postman.login()
    }

    @Test
    fun listingRootDir() {
        val rootDirCount = 8
        with(postman.get("/api/file")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(rootDirCount, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(rootDirCount, files.size)
                for (file in files) {
                    Assertions.assertEquals(FileType.DIR.toString(), file["type"])
                    Assertions.assertEquals(null, file["version"])
                }
            }
        }
    }

    @Test
    fun listingFirstOrder() {
        val fileNodeCount = 5
        with(postman.get("/api/file", mapOf("parentId" to 1))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(fileNodeCount, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(fileNodeCount, files.size)

                val types = listOf("DIR", "DDL", "SQL", "SQL", "SQL")
                val names = listOf("zwgjydgn", "jldwzlys", "kniovyqn", "ladlehnr", "yoglnkyc")
                for (i in 0 until 5) {
                    val file = files[i]
                    Assertions.assertEquals(1, file["groupId"])
                    Assertions.assertEquals(1, file["parentId"])
                    Assertions.assertEquals(types[i], file["type"])
                    Assertions.assertEquals(names[i], file["name"])
                }
            }
        }
    }

    @Test
    fun listingSecondOrder() {
        val fileNodeCount = 3
        with(postman.get("/api/file", mapOf("parentId" to 4))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(fileNodeCount, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(fileNodeCount, files.size)

                val types = listOf("DIR", "DDL", "SQL")
                val names = listOf("zvdjsdhz", "yzhamcqc", "yijlstlq")
                for (i in 0 until 3) {
                    val file = files[i]
                    Assertions.assertEquals(1, file["groupId"])
                    Assertions.assertEquals(4, file["parentId"])
                    Assertions.assertEquals(types[i], file["type"])
                    Assertions.assertEquals(names[i], file["name"])
                }
            }
        }
    }

    @Test
    fun search() {
        with(postman.get("/api/file", mapOf("like" to " a  b   c    "))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(2, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(2, files.size)
                Assertions.assertEquals("bcmawkte", files.first()["name"])
                Assertions.assertEquals("lwbaccod", files.last()["name"])
            }
        }

        with(postman.get("/api/file", mapOf("like" to "hh"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(3, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(3, files.size)
            }
        }
    }

    @Test
    fun createDir() {
        with(postman.post("/api/file", mapOf(
            "groupId" to 12345,
            "name" to "test create",
            "type" to "DIR",
            "parentId" to 38324))
        ) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val file = get("file") as LinkedHashMap<String, Any>
                Assertions.assertEquals(12345, file["groupId"])
                Assertions.assertEquals(1, file["ownerId"])
                Assertions.assertEquals("test create", file["name"])
                Assertions.assertEquals("DIR", file["type"])
                Assertions.assertEquals(38324, file["parentId"])
                Assertions.assertEquals(null, file["version"])
            }
        }

        with(postman.get("/api/file", mapOf("like" to "test create"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals("test create", file["name"])
            }
        }

        with(postman.get("/api/file", mapOf("parentId" to 38324))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals("test create", file["name"])
            }
        }
    }

    @Test
    fun createFile() {
        with(postman.post("/api/file", mapOf(
            "groupId" to 12345,
            "name" to "test create",
            "type" to "SQL",
            "parentId" to 38324))
        ) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                val file = get("file") as LinkedHashMap<String, Any>
                Assertions.assertEquals(12345, file["groupId"])
                Assertions.assertEquals(1, file["ownerId"])
                Assertions.assertEquals("test create", file["name"])
                Assertions.assertEquals("SQL", file["type"])
                Assertions.assertEquals(38324, file["parentId"])
                Assertions.assertNotEquals(null, file["version"])
            }
        }

        with(postman.get("/api/file", mapOf("like" to "test create"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals("test create", file["name"])
            }
        }

        with(postman.get("/api/file", mapOf("parentId" to 38324))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals("test create", file["name"])
            }
        }
    }

    @Test
    fun update() {
        with(postman.put("/api/file/28", mapOf("name" to "test update"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("file 28 has been update", body?.get("message"))
        }
        with(postman.get("/api/file", mapOf("like" to "test update"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals("test update", file["name"])
            }
        }

        with(postman.put("/api/file/28", mapOf("ownerId" to 1))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("file 28 has been update", body?.get("message"))
        }
        with(postman.get("/api/file", mapOf("like" to "test update"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals(1, file["ownerId"])
            }
        }

        with(postman.put("/api/file/28", mapOf("version" to 38324))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("file 28 has been update", body?.get("message"))
        }
        with(postman.get("/api/file", mapOf("like" to "test update"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals(38324, file["version"])
            }
        }

        with(postman.put("/api/file/28", mapOf("parentId" to 38324))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("file 28 has been update", body?.get("message"))
        }
        with(postman.get("/api/file", mapOf("parentId" to 38324))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val file = (get("files") as List<LinkedHashMap<String, Any>>).first()
                Assertions.assertEquals(38324, file["version"])
            }
        }
    }

    @Test
    fun updateMoreThanOneArgument() {
        with(postman.put("/api/file/3", mapOf("ownerId" to 1, "name" to "test update"))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("illegal argument: ownerId, name, version, parentId must only one not null", body?.get("error"))
        }
    }

    @Test
    fun updateNotFoundFile() {
        with(postman.put("/api/file/11", mapOf("ownerId" to 1))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("file 11 not found", body?.get("error"))
        }

        with(postman.put("/api/file/70", mapOf("ownerId" to 1))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("file 70 not found", body?.get("error"))
        }

    }

    @Test
    fun remove() {
        with(postman.delete("/api/file/42")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("file 42 has been removed", body?.get("message"))
        }

        with(postman.get("/api/file", mapOf("parentId" to 4))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(2, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(2, files.size)
            }
        }

        with(postman.delete("/api/file/55")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("file 55 has been removed", body?.get("message"))
        }

        with(postman.get("/api/file", mapOf("parentId" to 4))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(1, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(1, files.size)
            }
        }

    }

    @Test
    fun removeRootDir() {
        with(postman.delete("/api/file/56")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("illegal argument: can not remove root dir hhkjnqwc", body?.get("error"))
        }
    }

    @Test
    fun removeDir() {
        with(postman.delete("/api/file/4")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            Assertions.assertEquals("file 4 has been removed", body?.get("message"))
        }

        val fileNodeCount = 0
        with(postman.get("/api/file", mapOf("parentId" to 4))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(fileNodeCount, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(fileNodeCount, files.size)
            }
        }

        with(postman.get("/api/file", mapOf("parentId" to 27))) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("success", body?.get("status"))
            with(body?.get("data") as Map<String, Any>) {
                Assertions.assertEquals(fileNodeCount, get("count"))
                val files = get("files") as List<LinkedHashMap<String, Any>>
                Assertions.assertEquals(fileNodeCount, files.size)
            }
        }
    }

    @Test
    fun removeNotFoundFile() {
        with(postman.delete("/api/file/11")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("file 11 not found", body?.get("error"))
        }
        with(postman.delete("/api/file/70")) {
            Assertions.assertEquals(HttpStatus.OK, statusCode)
            Assertions.assertEquals("failed", body?.get("status"))
            Assertions.assertEquals("file 70 not found", body?.get("error"))
        }
    }
}