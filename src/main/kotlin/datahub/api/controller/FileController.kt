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

    @GetMapping
    fun listing(@RequestParam(required = false) parentId: Int?,
                @RequestParam(required = false) like: String?): ResponseData {
        val files = Files.select().where {
            val conditions = Lists.newArrayList<ColumnDeclaring<Boolean>>(Files.isRemove eq false)
            if (!like.isNullOrBlank()) { // do global search, will ignore parent ID filter
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
     * 通过 groupId 查找该项目组的根目录
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

    @PostMapping
    fun create(@RequestParam(required = true) groupId: Int,
               @RequestParam(required = true) name: String,
               @RequestParam(required = true) type: FileType,
               @RequestParam(required = true) parentId: Int): ResponseData {
        // todo: parameter check and permission check
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

    @DeleteMapping("{id}")
    fun remove(@PathVariable id: Int): ResponseData {
        val file = Files.findById(id)
        return if (file == null || file.isRemove) {
            Response.Failed.DataNotFound("file $id")
        } else if (file.parentId == null) {
            Response.Failed.IllegalArgument("can not remove root dir ${file.name}")
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