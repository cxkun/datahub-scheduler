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
import datahub.api.ResponseData
import datahub.api.auth.MD5
import datahub.api.utils.Page
import datahub.dao.Users
import datahub.models.User
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.findById
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/api/user")
class UserController {

    @GetMapping
    @ResponseBody
    fun listing(page: Int = 1, pageSize: Int = Int.MAX_VALUE): ResponseData {
        val users = Users.select().where { Users.isRemove eq false }
        val count = users.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "users" to users.limit(Page.offset(page, pageSize), pageSize).map { Users.createEntity(it) }
        ))
    }

    @GetMapping("{id}")
    fun find(@PathVariable id: Int): ResponseData {
        val user = Users.findById(id)
        return if (user == null || user.isRemove) {
            Response.Failed.DataNotFound("user $id")
        } else {
            Response.Success.WithData("user" to user)
        }
    }

    @PostMapping
    @ResponseBody
    fun create(@NotBlank(message = "{required}") username: String,
               @NotBlank(message = "{required}") password: String,
               @NotBlank(message = "{required}") groupIds: Set<Int>,
               @NotBlank(message = "{required}") email: String): ResponseData {
        val user = User {
            this.name = username
            this.password = MD5.encrypt(password)
            this.groupIds = groupIds
            this.email = email
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Users.add(user)
        return Response.Success.WithData("user" to user)
    }


    @PutMapping("{id}")
    @ResponseBody
    fun update(@PathVariable id: String,
               username: String,
               password: String,
               groupIds: Set<Int>,
               email: String): ResponseData {
        val user = Users.findById(id)
        return if (user == null || user.isRemove) {
            Response.Failed.DataNotFound("user $id")
        } else {
            user.name = username
            user.password = MD5.encrypt(password)
            user.groupIds = groupIds
            user.email = email
            user.updateTime = LocalDateTime.now()
            user.flushChanges()
            Response.Success.Update("user $username")
        }
    }

    @DeleteMapping("{id}")
    fun remove(@PathVariable id: Int): ResponseData {
        val user = Users.findById(id)
        return if (user == null || user.isRemove) {
            Response.Failed.DataNotFound("user $id")
        } else {
            user.isRemove = true
            user.flushChanges()
            Response.Success.Remove("user ${user.name}")
        }
    }

}
