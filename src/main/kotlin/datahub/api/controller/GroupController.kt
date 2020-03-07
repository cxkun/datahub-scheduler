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
import datahub.api.utils.Page
import datahub.dao.Files
import datahub.dao.Groups
import datahub.dao.Users
import datahub.models.File
import datahub.models.Group
import datahub.models.dtype.FileType
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.ColumnDeclaring
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
@RequestMapping("/api/group")
class GroupController {

    /**
     * @api {get} /api/group 获取项目组列表
     * @apiDescription 获取项目组列表，支持分页和模糊查询
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} [page = 1] 分页ID
     * @apiParam {Number} [pageSize = 9999] 分页大小
     * @apiParam {String} like 项目组名模糊匹配，多个词用空格分隔，null 字符串会被忽略
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"count":32,"groups":[{"id":37,"name":"rjktnfdu","isRemove":false,"createTime":"2006-10-07 09:46:27","updateTime":"2007-07-29 16:10:54"},{"id":38,"name":"fpjsyxun","isRemove":false,"createTime":"2044-10-25 01:02:29","updateTime":"2046-03-05 16:13:40"}]}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @GetMapping
    fun listing(@RequestParam(required = false, defaultValue = "1") page: Int,
                @RequestParam(required = false, defaultValue = "9999") pageSize: Int,
                @RequestParam(required = false) like: String?): ResponseData {
        val groups = Groups.select().where {
            val conditions = Lists.newArrayList<ColumnDeclaring<Boolean>>(Groups.isRemove eq false)
            if (like != null && like.isNotBlank() && like.trim().toUpperCase() != "NULL") {
                like.split("\\s+".toRegex()).forEach {
                    conditions.add(Groups.name.like("%$it%"))
                }
            }
            conditions.reduce { a, b -> a and b }
        }
        val count = groups.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "groups" to groups.orderBy(Groups.id.asc()).limit(Page.offset(page, pageSize), pageSize).map {
                Groups.createEntity(it)
            }
        ))
    }

    /**
     * @api {get} /api/group/{id} 查找项目组
     * @apiDescription 查找指定 ID 的项目组信息，查找已删除的或不存在的项目组会返回失败
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"group":{"id":1,"name":"root","isRemove":false,"createTime":"2050-01-10 23:42:07","updateTime":"2050-10-27 21:44:49"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"group 7 not found"}
     */
    @GetMapping("{id}")
    fun find(@PathVariable id: Int): ResponseData {
        val group = Groups.findById(id)
        return if (group == null || group.isRemove) {
            Response.Failed.DataNotFound("group $id")
        } else {
            Response.Success.WithData(mapOf("group" to group))
        }
    }

    /**
     * @api {post} /api/group 创建项目组
     * @apiDescription 创建项目组，并创建项目组的根节点，返回创建后的项目组信息和根节点信息
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} name 项目组名称
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"group":{"name":"xxxx","isRemove":false,"createTime":"2020-03-07 23:23:54","updateTime":"2020-03-07 23:23:54","id":40},"file":{"groupId":40,"ownerId":1,"name":"xxxx","type":"DIR","version":null,"parentId":null,"isRemove":false,"createTime":"2020-03-07 23:23:54","updateTime":"2020-03-07 23:23:54","id":70}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @PostMapping
    fun create(@NotBlank(message = "{required}") name: String): ResponseData {
        val group = Group {
            this.name = name
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Groups.add(group)
        val file = File {
            this.groupId = group.id
            this.ownerId = Jwt.currentUser.id
            this.name = group.name
            this.type = FileType.DIR
            this.version = null
            this.parentId = null
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Files.add(file)
        return Response.Success.WithData(mapOf("group" to group, "file" to file))
    }

    /**
     * @api {put} /api/group/{id} 更新项目组信息
     * @apiDescription 更新指定 ID 的项目组信息，并返回更新后的数据
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} [name = null] 项目组名称，如果不提供则不更新
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"group":{"id":38,"name":"fpjsy","isRemove":false,"createTime":"2044-10-25 01:02:29","updateTime":"2020-03-07 23:25:36"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"group 7 not found"}
     */
    @PutMapping("{id}")
    fun update(@PathVariable id: Int, @RequestParam(required = false) name: String?): ResponseData {
        val group = Groups.findById(id)
        return if (group == null || group.isRemove) {
            Response.Failed.DataNotFound("group $id")
        } else {
            var update = false
            if (name != null) {
                group.name = name
                update = true
            }
            if (update) {
                group.updateTime = LocalDateTime.now()
            }
            group.flushChanges()
            Response.Success.WithData(mapOf("group" to group))
        }
    }

    /**
     * @api {delete} /api/group/{id} 删除项目组
     * @apiDescription 删除指定 ID 的项目组
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","message":"group 38 has been removed"}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @DeleteMapping("{id}")
    fun remove(@PathVariable id: Int): ResponseData {
        val group = Groups.findById(id)
        return if (group == null || group.isRemove) {
            Response.Failed.DataNotFound("group $id")
        } else {
            group.isRemove = true
            group.flushChanges()
            Response.Success.Remove("group ${group.id}")
        }
    }

}