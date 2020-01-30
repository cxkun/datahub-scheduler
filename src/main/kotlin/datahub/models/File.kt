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
package datahub.models

import datahub.models.dtype.FileType
import me.liuwj.ktorm.entity.Entity
import java.time.LocalDateTime

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
interface File : Entity<File> {
    companion object : Entity.Factory<File>()

    val id: Int
    var groupId: Int
    var ownerId: Int
    var name: String
    var type: FileType
    var version: Int? // content version, (i.e FileContent.id, null if type is dir)
    var parentId: Int? // parent file node ID, null if type is root dir
    var isRemove: Boolean
    var createTime: LocalDateTime
    var updateTime: LocalDateTime

}