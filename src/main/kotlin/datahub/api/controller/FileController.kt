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
import datahub.dao.FileContents
import datahub.dao.Files
import datahub.models.File
import datahub.models.FileContent
import datahub.models.dtype.FileType
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.limit
import me.liuwj.ktorm.dsl.select
import me.liuwj.ktorm.dsl.where
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.findById
import org.apache.shiro.SecurityUtils
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

/**
 * @author Jensen Qi 2020/01/19
 * @since 1.0.0
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/api/file")
class FileController {
    @GetMapping
    fun listing(@RequestParam(required = false, defaultValue = "1") page: Int,
                @RequestParam(required = false, defaultValue = "9999") pageSize: Int = Int.MAX_VALUE): ResponseData {
        val files = Files.select().where { Files.isRemove eq false }
        val count = files.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "files" to files.limit(Page.offset(page, pageSize), pageSize).map { Files.createEntity(it) }
        ))
    }

    @GetMapping("{id}")
    fun find(@PathVariable id: Int): ResponseData {
        val file = Files.findById(id)
        return if (file == null || file.isRemove) {
            Response.Failed.DataNotFound("file $id")
        } else {
            Response.Success.WithData(mapOf("file" to file))
        }
    }

    @GetMapping("{id}/content")
    fun getContent(@PathVariable id: Int): ResponseData {
        val file = Files.findById(id)
        return if (file == null || file.isRemove || file.type == FileType.Dir) {
            Response.Failed.DataNotFound("content of file $id")
        } else {
            val fileContent = FileContents.findById(file.version!!)
            if (fileContent == null || fileContent.isRemove) {
                Response.Failed.DataNotFound("content of file $id")
            } else {
                Response.Success.WithData(mapOf("content" to fileContent))
            }
        }
    }

    @PostMapping
    fun create(@NotBlank(message = "{groupId can't be blank}") groupId: Int,
               @NotBlank(message = "{name can't be blank}") name: String,
               @NotBlank(message = "{fileType can't be blank}") type: FileType,
               @Min(value = 1, message = "{parentId must greater than 1}") parentId: Int
    ): ResponseData {
        println(SecurityUtils.getSubject())
        println(SecurityUtils.getSubject().principal)
        val file = File {
            this.groupId = groupId
            this.ownerId = 0 // todo:current UserId
            this.name = name
            this.type = type
            this.parentId = parentId
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Files.add(file)
        return if (type != FileType.Dir) {
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
            Response.Success.WithData(mapOf(
                "file" to file,
                "content" to content
            ))
        } else {
            Response.Success.WithData(mapOf("file" to file))
        }
    }

    @PutMapping("{id}")
    fun update(@PathVariable id: Int) {

    }

    @DeleteMapping("{id}")
    fun remove(@PathVariable id: Int) {

    }

}