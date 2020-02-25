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
import datahub.tools.RestfulTestToolbox
import org.junit.jupiter.api.*

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
class LoginControllerTest : RestfulTestToolbox() {

    @BeforeEach
    fun rebuildDB() {
        SchemaUtils.rebuildDB()
        SchemaUtils.loadTable("datahub.users", this.javaClass.classLoader.getResource("tables/users.txt")!!.path)
        this.postman = Postman(template)
    }

    @Test
    fun login() {
        val token = postman.post("/api/login", mapOf("username" to "root", "password" to "root")).shouldSuccess
            .thenGetData["token"].toString()
        Assertions.assertEquals("root", Jwt.getUserName(token))

        postman.post("/api/login", mapOf("username" to "root", "password" to "wrong password"))
            .shouldFailed.withError("login failed")

        postman.post("/api/login", mapOf("username" to "wrong username", "password" to "root"))
            .shouldFailed.withError("login failed")
    }
}