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
import datahub.dao.FileContents
import datahub.dao.Files
import datahub.models.File
import datahub.models.FileContent
import datahub.models.dtype.FileType
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.ColumnDeclaring
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * @author Jensen Qi 2020/01/19
 * @since 1.0.0
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/api/file")
class FileController {

    /**
     * @api {get} /api/file 获取文件列表
     * @apiDescription 获取指定父节点的文件列表，或全局模糊查找
     * @apiGroup File
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} [parentId = null] 父节点 ID
     * @apiParam {String} [like = null] 文件名模糊匹配，多个词用空格分隔，null 字符串会被忽略
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"count":3,"files":[{"id":27,"groupId":1,"ownerId":19,"name":"zvdjsdhz","type":"DIR","parentId":4,"isRemove":false,"createTime":"2028-06-08 05:25:18","updateTime":"2029-07-07 15:07:21"},{"id":55,"groupId":1,"ownerId":44,"name":"yijlstlq","type":"SQL","version":41,"parentId":4,"isRemove":false,"createTime":"2008-04-13 07:17:42","updateTime":"2011-01-27 21:49:43"},{"id":42,"groupId":1,"ownerId":40,"name":"yzhamcqc","type":"SPARK","version":23,"parentId":4,"isRemove":false,"createTime":"2017-09-18 02:08:21","updateTime":"2020-08-30 13:28:13"}]}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @GetMapping
    fun listing(@RequestParam(required = false) parentId: Int?,
                @RequestParam(required = false) like: String?): ResponseData {
        val files = Files.select().where {
            val conditions = Lists.newArrayList<ColumnDeclaring<Boolean>>(Files.isRemove eq false)
            if (like != null && like.isNotBlank() && like.trim().toUpperCase() != "NULL") { // do global search, will ignore parent ID filter
                like.split("\\s+".toRegex()).forEach {
                    conditions.add(Files.name.like("%$it%"))
                }
            } else {
                if (parentId == null) {
                    conditions.add(Files.parentId.isNull())
                } else {
                    conditions.add(Files.parentId eq parentId)
                }
            }
            conditions.reduce { a, b -> a and b }
        }
        val count = files.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "files" to files.map { Files.createEntity(it) }.sortedWith(compareBy({
                when (it.type) {
                    FileType.DIR -> 0
                    FileType.SQL -> 1
                    FileType.MR -> 2
                    FileType.SPARK -> 3
                }
            }, { it.name })) // DIR first
        ))
    }


    /**
     * @api {get} /api/file/root 获取根目录
     * @apiDescription 获取指定项目组的根目录
     * @apiGroup File
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} groupId 项目组 ID
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"file":{"id":1,"groupId":1,"ownerId":1,"name":"root_project","type":"DIR","isRemove":false,"createTime":"2037-05-20 14:58:39","updateTime":"2040-02-04 21:46:36"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @GetMapping("/root")
    fun findRoot(@RequestParam(required = true) groupId: Int): ResponseData {
        val file = Files.select().where {
            Files.isRemove eq false and (Files.groupId eq groupId) and (Files.parentId.isNull())
        }.map { Files.createEntity(it) }
        if (file.size != 1) {
            return Response.Failed.DataNotFound("root in $groupId")
        }
        return Response.Success.WithData(mapOf("file" to file.first()))
    }

    /**
     * @api {post} /api/file 创建文件节点
     * @apiDescription 创建文件节点，并返回创建后的数据
     * @apiGroup File
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} groupId 归属的项目组 ID
     * @apiParam {String} name 节点名称
     * @apiParam {Enum} type 文件类型，可选 DIR、 SQL、 SPARK、 MR
     * @apiParam {Number} parentId 父节点 ID
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"file":{"groupId":1,"ownerId":1,"name":"aaa","type":"SPARK","version":1,"parentId":1,"isRemove":false,"createTime":"2020-03-07 23:36:34","updateTime":"2020-03-07 23:36:34","id":71}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @PostMapping
    fun create(@RequestParam(required = true) groupId: Int,
               @RequestParam(required = true) name: String,
               @RequestParam(required = true) type: FileType,
               @RequestParam(required = true) parentId: Int): ResponseData {
        // todo: parameter check and permission check
        if (Files.select().where { Files.isRemove eq false and (Files.parentId eq parentId) and (Files.name eq name) }.totalRecords > 0) {
            return Response.Failed.IllegalArgument("该文件夹下已存在 $name 节点")
        }
        val currentUser = Jwt.currentUser
        val file = File {
            this.groupId = groupId
            this.ownerId = currentUser.id
            this.name = name
            this.type = type
            this.version = null
            this.parentId = parentId
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Files.add(file)
        if (type != FileType.DIR) {
            val content = FileContent {
                this.fileId = file.id
                this.content = ""
                this.isRemove = false
                this.updateTime = LocalDateTime.now()
                this.updateTime = LocalDateTime.now()
            }
            FileContents.add(content)
            file.version = content.id
            file.flushChanges()
        }
        return Response.Success.WithData(mapOf("file" to file))
    }

    private fun File.isRootDir() = this.parentId == null

    /**
     * @api {put} /api/file/{id} 更新文件信息
     * @apiDescription 更新指定 ID 的文件信息
     * @apiGroup File
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} [ownerId = null] 文件归属者 ID
     * @apiParam {String} [name = null] 文件名
     * @apiParam {Number} [version = null] 文件版本号
     * @apiParam {Number} [parentId = null] 父节点 ID
     * @apiSuccessExample 请求成功
     * {"status":"success","message":"file 3 has been update"}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @PutMapping("{id}")
    fun update(@PathVariable id: Int,
               @RequestParam(required = false) ownerId: Int?,
               @RequestParam(required = false) name: String?,
               @RequestParam(required = false) version: Int?,
               @RequestParam(required = false) parentId: Int?): ResponseData {
        if (listOfNotNull(ownerId, name, version, parentId).size != 1) {
            return Response.Failed.IllegalArgument("ownerId, name, version, parentId must only one not null")
        }
        val file = Files.findById(id)
        return if (file == null || file.isRemove) {
            Response.Failed.DataNotFound("file $id")
        } else if (file.isRootDir()) {
            return Response.Failed.IllegalArgument("根节点不允许更改")
        } else {
            when {
                ownerId != null -> file.ownerId = ownerId
                name != null -> file.name = name
                version != null -> file.version = version
                parentId != null -> file.parentId = parentId
            }
            file.updateTime = LocalDateTime.now()
            file.flushChanges()
            Response.Success.Update("file $id")
        }
    }

    private fun removeChildRecursive(parentId: Int) {
        Database.global.update(Files) {
            it.isRemove to true
            it.updateTime to LocalDateTime.now()
            where { it.parentId eq parentId and (it.isRemove eq false) }
        }
        Files.select().where { Files.type eq FileType.DIR and (Files.parentId eq parentId) }.forEach {
            removeChildRecursive(Files.createEntity(it).id)
        }
    }

    /**
     * @api {delete} /api/file/{id} 删除文件
     * @apiDescription 删除指定 ID 的文件节点，如果这是一个文件夹节点，则递归地删除其子节点
     * @apiGroup File
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","message":"file 2 has been removed"}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @DeleteMapping("{id}")
    fun remove(@PathVariable id: Int): ResponseData {
        val file = Files.findById(id)
        return if (file == null || file.isRemove) {
            Response.Failed.DataNotFound("file $id")
        } else if (file.parentId == null) {
            Response.Failed.IllegalArgument("根节点不允许删除")
        } else {
            file.isRemove = true
            file.updateTime = LocalDateTime.now()
            file.flushChanges()
            if (file.type == FileType.DIR) {
                removeChildRecursive(file.id)
            }
            Response.Success.Remove("file ${file.id}")
        }
    }

}