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

import datahub.models.Task
import datahub.models.dtype.enum
import datahub.scheduler.ops.OperatorType
import me.liuwj.ktorm.jackson.json
import me.liuwj.ktorm.schema.*


@ColumnsDef("""
    id              bigint unsigned comment 'task ID' auto_increment primary key,
    type            varchar(32)     comment 'task type',
    name            varchar(512)    comment 'task name',
    command         text            comment 'command to exec',
    owner_id        int unsigned    comment 'owner user ID',
    parent_ids      text            comment 'parent task id list',
    children_ids    text            comment 'children task id list',
    is_remove       bool            comment 'whether task is removed',
    create_time     datetime        comment 'task create time',
    update_time     datetime        comment 'last update time',
    key idx_type(is_remove, type),
    key idx_owner(is_remove, owner_id)
""")
object Tasks : Table<Task>("tasks") {
    val id by int("id").primaryKey().bindTo { it.id }
    val type by enum("type", typeRef<OperatorType>()).bindTo { it.type }
    val name by varchar("name").bindTo { it.name }
    val command by text("command").bindTo { it.command }
    val ownerId by int("owner_id").bindTo { it.ownerId }
    val parentIds by json("parent_ids", typeRef<Set<Int>>()).bindTo { it.parentIds }
    val childrenIds by json("children_ids", typeRef<Set<Int>>()).bindTo { it.childrenIds }
    val isRemove by boolean("is_remove").bindTo { it.isRemove }
    val createTime by datetime("create_time").bindTo { it.createTime }
    val updateTime by datetime("update_time").bindTo { it.updateTime }
}