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
import datahub.dao.Groups
import datahub.models.Group
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
            Response.Success.WithData("group" to group)
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
        return Response.Success.WithData("group" to group)
    }

    @PutMapping("{id}")
    fun update(@PathVariable id: Int, name: String): ResponseData {
        val group = Groups.findById(id)
        return if (group == null || group.isRemove) {
            Response.Failed.DataNotFound("group $id")
        } else {
            group.name = name
            group.flushChanges()
            Response.Success.WithData("group has been update")
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
            Response.Success.WithData("group has been remove")
        }
    }

}