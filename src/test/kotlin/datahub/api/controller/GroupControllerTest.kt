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

import datahub.dao.SchemaUtils
import datahub.models.dtype.FileType
import datahub.tools.Postman
import datahub.tools.RestfulTestToolbox
import org.junit.jupiter.api.*
import org.springframework.http.HttpStatus


/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@Suppress("UNCHECKED_CAST")
class GroupControllerTest : RestfulTestToolbox() {

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
        postman.get("/api/group").shouldSuccess.thenGetData.andCheckCount(validGroupCount)
            .thenGetListOf("groups").andCheckSize(validGroupCount)

        val pageSize = 7
        val queryTimes = validGroupCount / pageSize + 1
        val lastPageGroupCount = validGroupCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/group", mapOf("page" to page, "pageSize" to pageSize)).shouldSuccess
                .thenGetData.andCheckCount(validGroupCount)
                .thenGetListOf("groups").andCheckSize(if (page == queryTimes) lastPageGroupCount else pageSize)
        }
    }

    @Test
    fun find() {
        postman.get("/api/group/23").shouldSuccess.thenGetData.thenGetItem("group").withExpect {
            it["name"] shouldBe "kcwhynhd"
            it["createTime"] shouldBe "2044-11-11 15:27:26"
            it["updateTime"] shouldBe "2047-07-02 20:28:57"
        }
        postman.get("/api/group/39").shouldFailed.withError("group 39 not found")
        postman.get("/api/group/40").shouldFailed.withError("group 40 not found")
    }

    @Test
    fun create() {
        postman.login("guest", "guest")

        val nextGroupId = 40
        val name = "test_create"

        postman.post("/api/group", mapOf("name" to name)).shouldSuccess.thenGetData.apply {
            this.thenGetItem("group").withExpect {
                it["id"] shouldBe nextGroupId
                it["name"] shouldBe name
            }
            this.thenGetItem("file").withExpect {
                it["groupId"] shouldBe nextGroupId
                it["ownerId"] shouldBe 2
                it["name"] shouldBe name
                it["type"] shouldBe FileType.DIR.toString()
                it["version"] shouldBe null
                it["parentId"] shouldBe null
            }
        }

        postman.get("/api/group/$nextGroupId").shouldSuccess.thenGetData.thenGetItem("group").withExpect {
            it["name"] shouldBe name
        }

        // todo test file created
    }

    @Test
    fun update() {
        val newName = "new_name"
        postman.put("/api/group/25", mapOf("name" to newName)).shouldSuccess.thenGetData.thenGetItem("group")
            .withExpect {
                it["name"] shouldBe newName
                it["updateTime"] shouldNotBe "2051-07-10 20:16:48"
            }
        postman.get("/api/group/25").shouldSuccess.thenGetData.thenGetItem("group").withExpect {
            it["name"] shouldBe newName
            it["updateTime"] shouldNotBe "2051-07-10 20:16:48"
        }
    }

    @Test
    fun remove() {
        postman.get("/api/group/15").shouldSuccess
        postman.delete("/api/group/15").shouldSuccess.withMessage("group 15 has been removed")
        postman.get("/api/group/15").shouldFailed.withError("group 15 not found")

        postman.delete("/api/group/18").shouldFailed.withError("group 18 not found")

        postman.delete("/api/group/40").shouldFailed.withError("group 40 not found")

    }
}