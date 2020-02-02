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
import datahub.models.File
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
class FileControllerTest {
    @Autowired
    lateinit var template: TestRestTemplate
    lateinit var postman: Postman

    @BeforeAll
    fun rebuildDB() {
        SchemaUtils.rebuildDB()
        this.postman = Postman(template)
        this.postman.login()
    }

    @Test
    fun listing() {
        val response = postman.get("/api/file")
        Assertions.assertEquals(response.statusCode, HttpStatus.OK)

        val status = response.body?.get("status") ?: "failed"
        Assertions.assertEquals(status, "success")

        val data = response.body?.get("data")
        data as Map<String, Any>
        Assertions.assertEquals(data["count"], 0)

        val files = data["files"]
        files as List<File>
        Assertions.assertIterableEquals(files, listOf<File>())
    }

    @Test
    fun find() {
    }

    @Test
    fun getContent() {
    }

    @Test
    fun create() {
    }

    @Test
    fun update() {
    }

    @Test
    fun remove() {
    }
}