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
import datahub.api.utils.Page
import datahub.dao.Files
import datahub.dao.Groups
import datahub.models.File
import datahub.models.Group
import datahub.models.dtype.FileType
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.limit
import me.liuwj.ktorm.dsl.select
import me.liuwj.ktorm.dsl.where
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
@RequestMapping("/api/group")
class GroupController {

    @GetMapping
    fun listing(page: Int = 1, pageSize: Int = Int.MAX_VALUE): ResponseData {
        val groups = Groups.select().where { Groups.isRemove eq false }
        val count = groups.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "groups" to groups.limit(Page.offset(page, pageSize), pageSize).map { Groups.createEntity(it) }
        ))
    }

    @GetMapping("{id}")
    fun find(@PathVariable id: Int): ResponseData {
        val group = Groups.findById(id)
        return if (group == null || group.isRemove) {
            Response.Failed.DataNotFound("group $id")
        } else {
            Response.Success.WithData(mapOf("group" to group))
        }
    }

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
            this.ownerId = 1 // root user
            this.name = group.name
            this.type = FileType.Dir
            this.version = null
            this.parentId = null
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Files.add(file)
        return Response.Success.WithData(mapOf("group" to group, "file" to file))
    }

    @PutMapping("{id}")
    fun update(@PathVariable id: Int, name: String): ResponseData {
        val group = Groups.findById(id)
        return if (group == null || group.isRemove) {
            Response.Failed.DataNotFound("group $id")
        } else {
            group.name = name
            group.flushChanges()
            Response.Success.Update("group ${group.id}")
        }
    }

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