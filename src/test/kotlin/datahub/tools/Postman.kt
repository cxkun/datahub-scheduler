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
package datahub.tools

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

    private fun Map<String, Any>.toRequestParam(): HttpEntity<MultiValueMap<String, Any>> {
        val params = LinkedMultiValueMap<String, Any>().also {
            this.forEach { (key, value) ->
                it.add(key, value)
            }
        }
        return HttpEntity<MultiValueMap<String, Any>>(params, header)
    }

    fun post(url: String, args: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val response = template.postForEntity(url, args.toRequestParam(), Map::class.java)!!
        return uncheckedCast(response)
    }

    fun get(url: String) = get(url, mapOf())

    fun get(url: String, args: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val argString = args.map { (key, value) ->
            "$key=$value"
        }.joinToString("&")
        val response = template.exchange("${url}?$argString", HttpMethod.GET, HttpEntity<Any>(header), Map::class.java)
        return uncheckedCast(response)
    }

    fun delete(url: String): ResponseEntity<Map<String, Any>> {
        val response = template.exchange(url, HttpMethod.DELETE, mapOf<String, Any>().toRequestParam(), Map::class.java)
        return uncheckedCast(response)
    }


    fun put(url: String, args: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val response = template.exchange(url, HttpMethod.PUT, args.toRequestParam(), Map::class.java)
        return uncheckedCast(response)
    }

    fun login() = login("root", "root")

    fun login(username: String, password: String) {
        val response = post("/api/login", mapOf(
            "username" to username,
            "password" to password
        ))
        val data = response.body!!["data"]
        data as Map<String, String>
        header.set("token", data["token"])
    }

}

