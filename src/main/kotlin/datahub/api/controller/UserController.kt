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
import datahub.api.auth.Jwt
import datahub.api.auth.MD5
import datahub.api.utils.Page
import datahub.dao.Users
import datahub.models.User
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import kotlin.collections.LinkedHashMap

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/api/user")
class UserController {

    private fun User.wipeOutPassword(): LinkedHashMap<String, Any> {
        val responseData = LinkedHashMap<String, Any>()
        responseData["id"] = this.id
        responseData["groupIds"] = this.groupIds
        responseData["name"] = this.name
        responseData["email"] = this.email
        responseData["createTime"] = this.createTime
        responseData["updateTime"] = this.updateTime
        return responseData
    }

    @GetMapping
    @ResponseBody
    fun listing(@RequestParam(required = false, defaultValue = "1") page: Int,
                @RequestParam(required = false, defaultValue = "9999") pageSize: Int): ResponseData {
        val users = Users.select().where { Users.isRemove eq false }
        val count = users.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "users" to users.orderBy(Users.id.asc()).limit(Page.offset(page, pageSize), pageSize).map {
                Users.createEntity(it).wipeOutPassword()
            }
        ))
    }

    @GetMapping("/current")
    fun currentUser(): ResponseData = Response.Success.WithData(mapOf("user" to Jwt.currentUser.wipeOutPassword()))


    @GetMapping("{id}")
    fun find(@PathVariable id: Int): ResponseData {
        val user = Users.select().where { Users.id eq id and (Users.isRemove eq false) }.map {
            Users.createEntity(it).wipeOutPassword()
        }.firstOrNull()
        return if (user == null) {
            Response.Failed.DataNotFound("user $id")
        } else {
            Response.Success.WithData(mapOf("user" to user))
        }
    }

    @PostMapping
    @ResponseBody
    fun create(@NotBlank(message = "{required}") name: String,
               @NotBlank(message = "{required}") password: String,
               @RequestParam(required = true) groupIds: ArrayList<Int>,
               @NotBlank(message = "{required}") email: String): ResponseData {
        if (Users.select().where { Users.name eq name and (Users.isRemove eq false) }.totalRecords > 0) {
            return Response.Failed.IllegalArgument("user $name exists")
        }
        val user = User {
            this.name = name
            this.groupIds = groupIds.toSet()
            this.password = MD5.encrypt(password)
            this.email = email
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Users.add(user)
        // todo: 创建用户后将账号密码发送到用户邮箱
        return Response.Success.WithData(mapOf("user" to user.wipeOutPassword()))
    }


    @PutMapping("{id}")
    @ResponseBody
    fun update(@PathVariable id: Int,
               @RequestParam(required = false) name: String?,
               @RequestParam(required = false) password: String?,
               @RequestParam(required = false) groupIds: ArrayList<Int>?,
               @RequestParam(required = false) email: String?): ResponseData {
        val user = Users.findById(id)
        return if (user == null || user.isRemove) {
            Response.Failed.DataNotFound("user $id")
        } else {
            var update = false
            if (name != null) {
                user.name = name
                update = true
            }
            if (password != null) {
                user.password = MD5.encrypt(password)
                update = true
            }
            if (groupIds != null) {
                user.groupIds = groupIds.toSet()
                update = true
            }
            if (email != null) {
                user.email = email
                update = true
            }
            if (update) {
                user.updateTime = LocalDateTime.now()
            }
            user.flushChanges()
            Response.Success.Update("user $id")
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
            Response.Success.Remove("user ${user.id}")
        }
    }

}
