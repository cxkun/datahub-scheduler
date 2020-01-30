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

import datahub.api.Response
import datahub.api.auth.Jwt
import datahub.api.auth.MD5
import datahub.dao.Users
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.findOne
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.*
import javax.validation.constraints.NotBlank

/**
 * @author Jensen Qi 2020/01/19
 * @since 1.0.0
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/api/login")
class LoginController {

    @PostMapping
    @ResponseBody
    fun login(@NotBlank(message = "{required}") username: String,
              @NotBlank(message = "{required}") password: String): Map<String, Any> {
        val passwordMD5 = MD5.encrypt(password)
        return if (Users.findOne { it.name eq username }?.password == passwordMD5) {
            Response.Success.WithData(mapOf("token" to Jwt.sign(username, passwordMD5).orEmpty()))
        } else {
            Response.Failed.WithError("login failed")
        }
    }
}