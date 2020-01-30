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
package datahub.api

import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

/**
 * @author Jensen Qi 2020/01/20
 * @since 1.0.0
 */
@Suppress("UNCHECKED_CAST")
class Postman(private val template: TestRestTemplate) {
    private val header = HttpHeaders().also {
        it.contentType = MediaType.APPLICATION_FORM_URLENCODED
    }

    private fun uncheckedCast(response: ResponseEntity<Map<*, *>>): ResponseEntity<Map<String, Any>> {
        response as ResponseEntity<Map<String, Any>>
        return response
    }

    fun post(url: String, args: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val map = LinkedMultiValueMap<String, Any>().also {
            args.forEach { (key, value) ->
                it.add(key, value)
            }
        }
        val req = HttpEntity<MultiValueMap<String, Any>>(map, header)
        val response = template.postForEntity(url, req, Map::class.java)!!
        return uncheckedCast(response)
    }

    fun get(url: String) = get(url, mapOf())

    fun get(url: String, args: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val argString = args.map { (key, value) ->
            "$key=$value"
        }.joinToString("&")
        val response = template.exchange("${url}?$argString", HttpMethod.GET, HttpEntity<Object>(header), Map::class.java)
        return uncheckedCast(response)
    }

    fun delete() {

    }

    fun put() {

    }

    fun login() {
        val response = post("/api/login", mapOf(
            "username" to "root",
            "password" to "root"
        ))
        val data = response.body!!["data"]
        data as Map<String, String>
        header.set("token", data["token"])
    }

}

