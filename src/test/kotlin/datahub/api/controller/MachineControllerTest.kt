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
import datahub.tools.Postman
import datahub.tools.RestfulTestToolbox
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class MachineControllerTest : RestfulTestToolbox() {

    @BeforeEach
    fun rebuildDB() {
        SchemaUtils.rebuildDB()
        SchemaUtils.loadTable("datahub.machines", this.javaClass.classLoader.getResource("tables/machines.txt")!!.path)
        SchemaUtils.loadTable("datahub.users", this.javaClass.classLoader.getResource("tables/users.txt")!!.path)
        this.postman = Postman(template)
        this.postman.login()
    }

    @Test
    fun listing() {
        val validMachineCount = 188
        postman.get("/api/machine").shouldSuccess.thenGetData.andCheckCount(validMachineCount)
            .thenGetListOf("machines").andCheckSize(validMachineCount)

        val pageSize = 13
        val queryTimes = validMachineCount / pageSize + 1
        val lastPageMachineCount = validMachineCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/machine", mapOf("page" to page, "pageSize" to pageSize)).shouldSuccess
                .thenGetData.andCheckCount(validMachineCount)
                .thenGetListOf("machines").andCheckSize(if (page == queryTimes) lastPageMachineCount else pageSize)
        }
    }

    @Test
    fun search() {
        // 提供空或 null 的相似词
        var validMachineCount = 188
        var pageSize = 13
        var queryTimes = validMachineCount / pageSize + 1
        var lastPageMachineCount = validMachineCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/machine", mapOf("page" to page, "pageSize" to pageSize, "like" to null)).shouldSuccess
                .thenGetData.andCheckCount(validMachineCount)
                .thenGetListOf("machines").andCheckSize(if (page == queryTimes) lastPageMachineCount else pageSize)

            postman.get("/api/machine", mapOf("page" to page, "pageSize" to pageSize, "like" to "  ")).shouldSuccess
                .thenGetData.andCheckCount(validMachineCount)
                .thenGetListOf("machines").andCheckSize(if (page == queryTimes) lastPageMachineCount else pageSize)
        }

        // 提供 1 个相似词
        validMachineCount = 58
        pageSize = 13
        queryTimes = validMachineCount / pageSize + 1
        lastPageMachineCount = validMachineCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/machine", mapOf("page" to page, "pageSize" to pageSize, "like" to " a ")).shouldSuccess
                .thenGetData.andCheckCount(validMachineCount)
                .thenGetListOf("machines").andCheckSize(if (page == queryTimes) lastPageMachineCount else pageSize)
        }

        // 提供 2 个相似词
        validMachineCount = 15
        pageSize = 4
        queryTimes = validMachineCount / pageSize + 1
        lastPageMachineCount = validMachineCount % pageSize
        for (page in 1..queryTimes) {
            postman.get("/api/machine", mapOf("page" to page, "pageSize" to pageSize, "like" to " a b")).shouldSuccess
                .thenGetData.andCheckCount(validMachineCount)
                .thenGetListOf("machines").andCheckSize(if (page == queryTimes) lastPageMachineCount else pageSize)
        }
    }

    @Test
    fun create() {
        val newHostname = "new host name"
        val newIP = "192.168.1.1"
        val nextId = 247

        postman.post("/api/machine", mapOf("hostname" to newHostname, "ip" to newIP)).shouldSuccess.thenGetData
            .thenGetItem("machine").withExpect {
                it["id"] shouldBe nextId
                it["hostname"] shouldBe newHostname
                it["ip"] shouldBe newIP
                it["mac"] shouldBe ""
                it["cpuLoad"] shouldBe 0
                it["memLoad"] shouldBe 0
                it["diskUsage"] shouldBe 0
            }
    }

    @Test
    fun update() {
        val newHostname = "new host name"
        val newIP = "192.168.1.1"

        // 只更新 hostname
        postman.put("/api/machine/1", mapOf("hostname" to newHostname)).shouldSuccess.thenGetData
            .thenGetItem("machine").withExpect {
                it["hostname"] shouldBe newHostname
                it["ip"] shouldBe "107.116.90.29"
                it["updateTime"] shouldNotBe "2032-06-08 19:36:03"
            }

        // 只更新 IP
        postman.put("/api/machine/3", mapOf("ip" to newIP)).shouldSuccess.thenGetData
            .thenGetItem("machine").withExpect {
                it["hostname"] shouldBe "nknvleif"
                it["ip"] shouldBe newIP
                it["updateTime"] shouldNotBe "2036-03-31 18:40:59"
            }

        // 更新 hostname, IP
        postman.put("/api/machine/5", mapOf("hostname" to newHostname, "ip" to newIP)).shouldSuccess.thenGetData
            .thenGetItem("machine").withExpect {
                it["hostname"] shouldBe newHostname
                it["ip"] shouldBe newIP
                it["updateTime"] shouldNotBe "2006-05-14 03:39:26"
            }

        // 更新已删除的机器信息
        postman.put("/api/machine/2", mapOf("hostname" to newHostname, "ip" to newIP))
            .shouldFailed.withNotFoundError("machine 2")

        // 更新不存在的机器信息
        postman.put("/api/machine/247", mapOf("hostname" to newHostname, "ip" to newIP))
            .shouldFailed.withNotFoundError("machine 247")

    }

    @Test
    fun remove() {
        postman.delete("/api/machine/11").shouldSuccess.withMessage("machine 11 has been removed")
        postman.delete("/api/machine/12").shouldFailed.withNotFoundError("machine 12") // 已删除的
        postman.delete("/api/machine/247").shouldFailed.withNotFoundError("machine 247") // 不存在的
    }
}