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
package datahub.dao

import datahub.models.File
import datahub.models.dtype.FileType
import me.liuwj.ktorm.schema.*

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@ColumnsDef("""
    id              bigint unsigned comment 'file ID' auto_increment primary key,
    group_id        int unsigned    comment 'group ID',
    owner_id        int unsigned    comment 'owner user ID',
    name            varchar(128)    comment 'file name',
    type            varchar(32)     comment 'file type',
    version         bigint unsigned comment 'current version, null if type is dir',
    parent_id       bigint unsigned comment 'parent ID, null if it is root dir',
    is_remove       bool            comment 'whether group is removed',
    create_time     datetime        comment 'task create time',
    update_time     datetime        comment 'last update time',
    key idx_group(is_remove, group_id, type),
    key idx_parent(is_remove, parent_id)
""")
object Files : Table<File>("files") {
    val id by int("id").primaryKey().bindTo { it.id }
    val groupId by int("group_id").bindTo { it.groupId }
    val ownerId by int("owner_id").bindTo { it.ownerId }
    val name by varchar("name").bindTo { it.name }
    val type by enum("type", typeRef<FileType>()).bindTo { it.type }
    val version by int("version").bindTo { it.version }
    val parentId by int("parent_id").bindTo { it.parentId }
    val isRemove by boolean("is_remove").bindTo { it.isRemove }
    val createTime by datetime("create_time").bindTo { it.createTime }
    val updateTime by datetime("update_time").bindTo { it.updateTime }
}