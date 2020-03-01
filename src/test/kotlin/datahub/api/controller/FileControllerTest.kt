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
import datahub.dao.SchemaUtils
import datahub.models.dtype.FileType
import datahub.tools.RestfulTestToolbox
import org.junit.jupiter.api.*

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@Suppress("UNCHECKED_CAST")
class FileControllerTest : RestfulTestToolbox() {

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
        postman.get("/api/file").shouldSuccess.thenGetData.andCheckCount(rootDirCount)
            .thenGetListOf("files").andCheckSize(rootDirCount).forEach {
                it["type"] shouldBe FileType.DIR.toString()
                it["version"] shouldBe null
            }
    }

    @Test
    fun listingFirstOrder() {
        val fileNodeCount = 5
        val types = listOf("DIR", "DDL", "SQL", "SQL", "SQL")
        val names = listOf("zwgjydgn", "jldwzlys", "kniovyqn", "ladlehnr", "yoglnkyc")
        postman.get("/api/file", mapOf("parentId" to 1)).shouldSuccess.thenGetData.andCheckCount(fileNodeCount)
            .thenGetListOf("files").andCheckSize(fileNodeCount).forEachIndexed { i, it ->
                it["groupId"] shouldBe 1
                it["parentId"] shouldBe 1
                it["type"] shouldBe types[i]
                it["name"] shouldBe names[i]
            }
    }

    @Test
    fun listingSecondOrder() {
        val fileNodeCount = 3
        val types = listOf("DIR", "DDL", "SQL")
        val names = listOf("zvdjsdhz", "yzhamcqc", "yijlstlq")
        postman.get("/api/file", mapOf("parentId" to 4)).shouldSuccess.thenGetData.andCheckCount(fileNodeCount)
            .thenGetListOf("files").andCheckSize(fileNodeCount).forEachIndexed { i, it ->
                it["groupId"] shouldBe 1
                it["parentId"] shouldBe 4
                it["type"] shouldBe types[i]
                it["name"] shouldBe names[i]
            }
    }

    @Test
    fun findRootDir() {

    }

    @Test
    fun search() {
        val files = postman.get("/api/file", mapOf("like" to " a  b   c    "))
            .shouldSuccess.thenGetData.andCheckCount(2).thenGetListOf("files").andCheckSize(2)
        files.first()["name"] shouldBe "bcmawkte"
        files.last()["name"] shouldBe "lwbaccod"

        postman.get("/api/file", mapOf("like" to "hh")).shouldSuccess.thenGetData.andCheckCount(3)
            .thenGetListOf("files").andCheckSize(3)
    }

    @Test
    fun createDir() {
        postman.post("/api/file", mapOf(
            "groupId" to 12345,
            "name" to "test create",
            "type" to "DIR",
            "parentId" to 38324)
        ).shouldSuccess.thenGetData.thenGetItem("file").withExpect {
            it["groupId"] shouldBe 12345
            it["ownerId"] shouldBe 1
            it["name"] shouldBe "test create"
            it["type"] shouldBe "DIR"
            it["parentId"] shouldBe 38324
            it["version"] shouldBe null
        }

        postman.get("/api/file", mapOf("like" to "test create")).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["name"] shouldBe "test create" }

        postman.get("/api/file", mapOf("parentId" to 38324)).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["name"] shouldBe "test create" }
    }

    @Test
    fun createFile() {
        postman.post("/api/file", mapOf(
            "groupId" to 12345,
            "name" to "test create",
            "type" to "SQL",
            "parentId" to 38324)
        ).shouldSuccess.thenGetData.thenGetItem("file").withExpect {
            it["groupId"] shouldBe 12345
            it["ownerId"] shouldBe 1
            it["name"] shouldBe "test create"
            it["type"] shouldBe "SQL"
            it["parentId"] shouldBe 38324
            it["version"] shouldNotBe null
        }

        postman.get("/api/file", mapOf("like" to "test create")).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["name"] shouldBe "test create" }

        postman.get("/api/file", mapOf("parentId" to 38324)).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["name"] shouldBe "test create" }
    }

    @Test
    fun update() {
        postman.put("/api/file/28", mapOf("name" to "test update")).shouldSuccess.withMessage("file 28 has been update")
        postman.get("/api/file", mapOf("like" to "test update")).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["name"] shouldBe "test update" }

        postman.put("/api/file/28", mapOf("ownerId" to 1)).shouldSuccess.withMessage("file 28 has been update")
        postman.get("/api/file", mapOf("like" to "test update")).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["ownerId"] shouldBe 1 }

        postman.put("/api/file/28", mapOf("version" to 38324)).shouldSuccess.withMessage("file 28 has been update")
        postman.get("/api/file", mapOf("like" to "test update")).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["version"] shouldBe 38324 }

        postman.put("/api/file/28", mapOf("parentId" to 38324)).shouldSuccess.withMessage("file 28 has been update")
        postman.get("/api/file", mapOf("parentId" to 38324)).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").first().withExpect { it["version"] shouldBe 38324 }
    }

    @Test
    fun updateMoreThanOneArgument() {
        postman.put("/api/file/3", mapOf("ownerId" to 1, "name" to "test update")).shouldFailed
            .withIllegalArgumentError("ownerId, name, version, parentId must only one not null")
    }

    @Test
    fun updateNotFoundFile() {
        postman.put("/api/file/11", mapOf("ownerId" to 1)).shouldFailed.withNotFoundError("file 11")
        postman.put("/api/file/70", mapOf("ownerId" to 1)).shouldFailed.withNotFoundError("file 70")
    }

    @Test
    fun remove() {
        postman.delete("/api/file/42").shouldSuccess.withMessage("file 42 has been removed")
        postman.get("/api/file", mapOf("parentId" to 4)).shouldSuccess.thenGetData.andCheckCount(2)
            .thenGetListOf("files").andCheckSize(2)

        postman.delete("/api/file/55").shouldSuccess.withMessage("file 55 has been removed")
        postman.get("/api/file", mapOf("parentId" to 4)).shouldSuccess.thenGetData.andCheckCount(1)
            .thenGetListOf("files").andCheckSize(1)
    }

    @Test
    fun removeRootDir() {
        postman.delete("/api/file/56").shouldFailed.withIllegalArgumentError("can not remove root dir hhkjnqwc")
    }

    @Test
    fun removeDir() {
        postman.delete("/api/file/4").shouldSuccess.withMessage("file 4 has been removed")

        // 子节点应被删除
        postman.get("/api/file", mapOf("parentId" to 4)).shouldSuccess.thenGetData.andCheckCount(0)
            .thenGetListOf("files").andCheckSize(0)

        // 孙节点应被删除
        postman.get("/api/file", mapOf("parentId" to 27)).shouldSuccess.thenGetData.andCheckCount(0)
            .thenGetListOf("files").andCheckSize(0)

        // 兄弟节点不应被删除
        postman.get("/api/file", mapOf("parentId" to 1)).shouldSuccess.thenGetData.andCheckCount(4)
            .thenGetListOf("files").andCheckSize(4)
    }


    @Test
    fun removeNotFoundFile() {
        postman.delete("/api/file/11").shouldFailed.withNotFoundError("file 11")
        postman.delete("/api/file/70").shouldFailed.withNotFoundError("file 70")
    }
}