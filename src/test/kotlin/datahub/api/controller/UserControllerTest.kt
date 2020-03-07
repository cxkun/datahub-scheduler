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
import datahub.tools.RestfulTestToolbox
import org.junit.jupiter.api.*

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
class UserControllerTest : RestfulTestToolbox() {

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
        postman.get("/api/user").shouldSuccess.thenGetData.andCheckCount(validUserCount)
            .thenGetListOf("users").andCheckSize(validUserCount)

        val pageSize = 13
        val queryTimes = validUserCount / pageSize + 1
        val lastPageUserCount = validUserCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/user", mapOf("page" to page, "pageSize" to pageSize)).shouldSuccess
                .thenGetData.andCheckCount(validUserCount)
                .thenGetListOf("users").andCheckSize(if (page == queryTimes) lastPageUserCount else pageSize)
                .forEach { it shouldNotContain "password" }
        }
    }

    @Test
    fun search() {
        // 提供空或 null 的相似词
        var validUserCount = 143
        var pageSize = 13
        var queryTimes = validUserCount / pageSize + 1
        var lastPageUserCount = validUserCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/user", mapOf("page" to page, "pageSize" to pageSize, "like" to null)).shouldSuccess
                .thenGetData.andCheckCount(validUserCount)
                .thenGetListOf("users").andCheckSize(if (page == queryTimes) lastPageUserCount else pageSize)
                .forEach { it shouldNotContain "password" }

            postman.get("/api/user", mapOf("page" to page, "pageSize" to pageSize, "like" to "  ")).shouldSuccess
                .thenGetData.andCheckCount(validUserCount)
                .thenGetListOf("users").andCheckSize(if (page == queryTimes) lastPageUserCount else pageSize)
                .forEach { it shouldNotContain "password" }
        }

        // 提供 1 个相似词
        validUserCount = 43
        pageSize = 7
        queryTimes = validUserCount / pageSize + 1
        lastPageUserCount = validUserCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/user", mapOf("page" to page, "pageSize" to pageSize, "like" to " a")).shouldSuccess
                .thenGetData.andCheckCount(validUserCount)
                .thenGetListOf("users").andCheckSize(if (page == queryTimes) lastPageUserCount else pageSize)
                .forEach { it shouldNotContain "password" }
        }

        // 提供 2 个相似词
        validUserCount = 8
        pageSize = 3
        queryTimes = validUserCount / pageSize + 1
        lastPageUserCount = validUserCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/user", mapOf("page" to page, "pageSize" to pageSize, "like" to " a b")).shouldSuccess
                .thenGetData.andCheckCount(validUserCount)
                .thenGetListOf("users").andCheckSize(if (page == queryTimes) lastPageUserCount else pageSize)
                .forEach { it shouldNotContain "password" }
        }
    }

    @Test
    fun currentUser() {
        postman.login("root", "root")
        postman.get("/api/user/current").shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["groupIds"] shouldSameElemWith setOf(1)
            it["name"] shouldBe "root"
            it["email"] shouldBe "root@datahub.com"
            it["createTime"] shouldBe "2048-08-14 06:10:35"
            it["updateTime"] shouldBe "2051-03-13 21:06:23"
            it shouldNotContain "password"
        }

        postman.login("guest", "guest")
        postman.get("/api/user/current").shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["groupIds"] shouldSameElemWith setOf(6, 8, 1, 9, 7, 5, 2)
            it["name"] shouldBe "guest"
            it["email"] shouldBe "guest@datahub.com"
            it["createTime"] shouldBe "2041-02-10 19:37:55"
            it["updateTime"] shouldBe "2042-03-23 08:54:17"
            it shouldNotContain "password"
        }
    }

    @Test
    fun find() {
        postman.get("/api/user/66").shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["groupIds"] shouldSameElemWith setOf(8, 7, 3, 6, 2, 1)
            it["name"] shouldBe "WjWUMovObM"
            it["email"] shouldBe "WjWUMovObM@139.com"
            it["createTime"] shouldBe "2042-06-02 09:25:38"
            it["updateTime"] shouldBe "2043-01-26 13:59:27"
            it shouldNotContain "password"
        }

        postman.get("/api/user/67").shouldFailed.withError("user 67 not found")
    }

    @Test
    fun create() {
        // 禁止创建重名用户
        postman.post("/api/user", mapOf("name" to "root", "password" to "", "groupIds" to listOf(1), "email" to ""))
            .shouldFailed.withError("illegal argument: user root exists")

        val nextUserId = 180
        val name = "test_create"
        val password = "test_password"
        val groupIds = setOf(131, 127)
        val email = "test_create@datahub.com"

        postman.post("/api/user", mapOf("name" to name, "password" to password, "groupIds" to groupIds, "email" to email))
            .shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["id"] shouldBe nextUserId
            it["name"] shouldBe name
            it["email"] shouldBe email
            it["groupIds"] shouldSameElemWith groupIds
            it shouldNotContain "password"
        }

        postman.get("/api/user/$nextUserId").shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["groupIds"] shouldSameElemWith groupIds
            it["name"] shouldBe name
            it["email"] shouldBe email
            it shouldNotContain "password"
        }

        val token = postman.post("/api/login", mapOf("username" to name, "password" to password))
            .shouldSuccess.thenGetData["token"].toString()
        Assertions.assertEquals(name, Jwt.getUserName(token))
    }

    @Test
    fun updateName() {
        val oldName = "root"
        val newName = "root_new_name"

        postman.post("/api/login", mapOf("username" to oldName, "password" to "root")).shouldSuccess

        postman.put("/api/user/1", mapOf("name" to newName)).shouldSuccess.thenGetData.thenGetItem("user")
            .withExpect {
                it["name"] shouldBe newName
                it shouldNotContain "password"
            }

        postman.post("/api/login", mapOf("username" to oldName, "password" to "root")).shouldFailed.withError("login failed")

        val token = postman.post("/api/login", mapOf("username" to newName, "password" to "root"))
            .shouldSuccess.thenGetData["token"].toString()
        Assertions.assertEquals(newName, Jwt.getUserName(token))
    }

    @Test
    fun updatePassword() {
        val oldPassword = "root"
        val newPassword = "root_new_password"

        postman.post("/api/login", mapOf("username" to "root", "password" to oldPassword)).shouldSuccess

        postman.put("/api/user/1", mapOf("password" to newPassword)).shouldSuccess.thenGetData.thenGetItem("user")
            .withExpect { it shouldNotContain "password" }

        postman.post("/api/login", mapOf("username" to "root", "password" to oldPassword)).shouldFailed.withError("login failed")

        val token = postman.post("/api/login", mapOf("username" to "root", "password" to newPassword))
            .shouldSuccess.thenGetData["token"].toString()
        Assertions.assertEquals("root", Jwt.getUserName(token))
    }

    @Test
    fun updateEmail() {
        val newEmail = "new_email@datahub.com"
        postman.put("/api/user/2", mapOf("email" to newEmail)).shouldSuccess.thenGetData.thenGetItem("user")
            .withExpect {
                it["email"] shouldBe newEmail
                it shouldNotContain "password"
            }
        postman.get("/api/user/2").shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["email"] shouldBe newEmail
            it["updateTime"] shouldNotBe "2042-03-23 08:54:17"
        }
    }

    @Test
    fun updateGroupIds() {
        val newGroupIds = setOf(137, 149)
        postman.put("/api/user/2", mapOf("groupIds" to newGroupIds)).shouldSuccess.thenGetData.thenGetItem("user")
            .withExpect {
                it["groupIds"] shouldSameElemWith newGroupIds
                it shouldNotContain "password"
            }
        postman.get("/api/user/2").shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["groupIds"] shouldSameElemWith newGroupIds
            it["updateTime"] shouldNotBe "2042-03-23 08:54:17"
        }
    }

    @Test
    fun updateAll() {
        val newName = "new_name"
        val newPassword = "new_password"
        val newEmail = "new_email@datahub.com"
        val newGroupIds = setOf(137, 149)
        postman.put("/api/user/2", mapOf("name" to newName, "password" to newPassword, "email" to newEmail, "groupIds" to newGroupIds))
            .shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["name"] shouldBe newName
            it["email"] shouldBe newEmail
            it["groupIds"] shouldSameElemWith newGroupIds
            it shouldNotContain "password"
        }

        val token = postman.post("/api/login", mapOf("username" to newName, "password" to newPassword))
            .shouldSuccess.thenGetData["token"].toString()
        Assertions.assertEquals(newName, Jwt.getUserName(token))

        postman.get("/api/user/2").shouldSuccess.thenGetData.thenGetItem("user").withExpect {
            it["groupIds"] shouldSameElemWith newGroupIds
            it["email"] shouldBe newEmail
            it["updateTime"] shouldNotBe "2042-03-23 08:54:17"
        }
    }

    @Test
    fun updateInvalidUser() {
        postman.put("/api/user/4", mapOf("name" to "user who has been remove")).shouldFailed.withError("user 4 not found")
        postman.put("/api/user/180", mapOf("name" to "user not exists")).shouldFailed.withError("user 180 not found")
    }

    @Test
    fun remove() {
        postman.get("/api/user/2").shouldSuccess
        postman.delete("/api/user/2").shouldSuccess.withMessage("user 2 has been removed")
        postman.get("/api/user/2").shouldFailed.withError("user 2 not found")

        postman.delete("/api/user/4").shouldFailed.withError("user 4 not found")

        postman.delete("/api/user/180").shouldFailed.withError("user 180 not found")
    }
}