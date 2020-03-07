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

import com.google.common.collect.Lists
import datahub.api.Response
import datahub.api.ResponseData
import datahub.api.auth.Jwt
import datahub.api.auth.MD5
import datahub.api.utils.Page
import datahub.dao.Users
import datahub.models.User
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.schema.ColumnDeclaring
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

    /**
     * @api {get} /api/user 获取用户列表
     * @apiDescription 获取用户列表，支持分页和模糊查询
     * @apiGroup User
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} [page = 1] 分页ID
     * @apiParam {Number} [pageSize = 9999] 分页大小
     * @apiParam {String} like 用户名模糊匹配，多个词用空格分隔，null 字符串会被忽略
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"count":143,"users":[{"id":177,"groupIds":[1,2],"name":"yUEtMsgswR","email":"yUEtMsgswR@aliyun.com","createTime":"2010-06-0518:23:58","updateTime":"2011-09-2300:47:13"},{"id":178,"groupIds":[1,2,3],"name":"snaspGzKcI","email":"snaspGzKcI@outlook.com","createTime":"2032-11-1808:54:43","updateTime":"2035-04-0704:26:21"}]}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @GetMapping
    fun listing(@RequestParam(required = false, defaultValue = "1") page: Int,
                @RequestParam(required = false, defaultValue = "9999") pageSize: Int,
                @RequestParam(required = false) like: String?): ResponseData {
        val users = Users.select().where {
            val conditions = Lists.newArrayList<ColumnDeclaring<Boolean>>(Users.isRemove eq false)
            if (like != null && like.isNotBlank() && like.trim().toUpperCase() != "NULL") {
                like.split("\\s+".toRegex()).forEach {
                    conditions.add(Users.name.like("%$it%"))
                }
            }
            conditions.reduce { a, b -> a and b }
        }
        val count = users.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "users" to users.orderBy(Users.id.asc()).limit(Page.offset(page, pageSize), pageSize).map {
                Users.createEntity(it).wipeOutPassword()
            }
        ))
    }

    /**
     * @api {get} /api/user/current 获取当前登录用户
     * @apiGroup User
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"user":{"id":1,"groupIds":[1],"name":"root","email":"root@datahub.com","createTime":"2048-08-14 06:10:35","updateTime":"2051-03-13 21:06:23"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @GetMapping("/current")
    fun currentUser(): ResponseData = Response.Success.WithData(mapOf("user" to Jwt.currentUser.wipeOutPassword()))


    /**
     * @api {get} /api/user/{id} 查找用户
     * @apiDescription 查找指定 ID 的用户，查找已删除的或不不存在的用户将返回错误
     * @apiGroup User
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"user":{"id":3,"groupIds":[2,3,4,6,8],"name":"OHzXwnDAAd","email":"OHzXwnDAAd@189.com","createTime":"2041-11-20 12:44:46","updateTime":"2044-05-12 14:09:07"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"user 2 not found"}
     */
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

    /**
     * @api {post} /api/user 创建用户
     * @apiDescription 创建用户，并返回创建后的数据
     * @apiGroup User
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} name 用户登录名
     * @apiParam {String} password 用户登录密码
     * @apiParam {Array} groupIds 用户归属项目组 ID
     * @apiParam {String} email 用户邮箱
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"user":{"id":180,"groupIds":[2,3],"name":"rootaa","email":"aaaaa","createTime":"2020-03-07 22:58:26","updateTime":"2020-03-07 22:58:26"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @PostMapping
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

    /**
     * @api {put} /api/user/{id} 更新用户信息
     * @apiDescription 更新指定 ID 的用户信息，并返回更新后的数据
     * @apiGroup User
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} [name = null] 用户登录名，不指定则不更新
     * @apiParam {String} [password = null] 用户登录密码，不指定则不更新
     * @apiParam {Array} [groupIds = null] 用户归属项目组，不指定则不更新
     * @apiParam {String} [email = null] 用户邮箱，不指定则不更新
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"user":{"id":10,"groupIds":[3,6],"name":"IinOzxLt","email":"IinOzxLtGL@139.com","createTime":"2018-03-21 03:59:24","updateTime":"2020-03-07 23:00:34"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @PutMapping("{id}")
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
            Response.Success.WithData(mapOf("user" to user.wipeOutPassword()))
        }
    }

    /**
     * @api {delete} /api/user/{id} 删除用户
     * @apiDescription 删除指定 ID 的用户
     * @apiGroup User
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","message":"user 2 has been removed"}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
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
