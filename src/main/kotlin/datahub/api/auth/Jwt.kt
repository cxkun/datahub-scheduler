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
package datahub.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import datahub.dao.Users
import datahub.models.User
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.select
import me.liuwj.ktorm.dsl.where
import me.liuwj.ktorm.entity.findOne
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.*


/**
 * @author Jensen Qi 2020/01/18
 * @since 1.0.0
 */
object Jwt {

    private const val EXPIRE_TIME = 86400000L // seconds of one day

    fun verify(token: String, userName: String, password: String) = try {
        JWT.require(Algorithm.HMAC256(password)).withClaim("username", userName).build().verify(token)
        true
    } catch (e: Exception) {
        false
    }

    fun getUserName(token: String) = try {
        JWT.decode(token).getClaim("username").asString()
    } catch (e: JWTDecodeException) {
        null
    }

    fun sign(userName: String, password: String) = try {
        JWT.create().withClaim("username", userName)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRE_TIME))
            .sign(Algorithm.HMAC256(password))
    } catch (e: Exception) {
        null
    }

    val currentUser: User
        get() {
            val servlet = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
            val request = servlet.request
            val userName = getUserName(request.getHeader("TOKEN"))!!
            return Users.findOne { it.name eq userName }!!
        }

}