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

import datahub.models.dtype.enum
import datahub.scheduler.ops.OperatorType
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.jackson.json
import me.liuwj.ktorm.schema.*
import java.time.LocalDateTime

data class Task(
    val id: Int,
    val type: Enum<OperatorType>,
    val name: String,
    val command: String,
    val ownerId: Int,
    val parentIds: Set<Int>,
    val childrenIds: Set<Int>,
    val isRemove: Boolean,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)

@ColumnsDef("""
    id              int             comment 'task ID' auto_increment primary key,
    type            tinyint         comment 'task type',
    name            varchar(512)    comment 'task name',
    command         text            comment 'command to exec',
    owner_id        int             comment 'owner user ID',
    parent_ids      text            comment 'parent task id list',
    children_ids    text            comment 'children task id list',
    is_remove       tinyint         comment 'whether task is removed',
    create_time     datetime        comment 'task create time',
    update_time     datetime        comment 'last update time',
    key idx_type(is_remove, type),
    key idx_owner(is_remove, owner_id)
""")
object Tasks : BaseTable<Task>("tasks") {
    val id by int("id").primaryKey()
    val type by enum("type", typeRef<OperatorType>())
    val name by varchar("name")
    val command by text("command")
    val ownerId by int("owner_id")
    val parentIds by json("parent_ids", typeRef<Set<Int>>())
    val childrenIds by json("children_ids", typeRef<Set<Int>>())
    val isRemove by boolean("is_remove")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")
    val defaultDatetime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Task(
        id = row[id] ?: 0,
        type = row[type] ?: OperatorType.Unsupported,
        name = row[name] ?: "",
        command = row[command] ?: "",
        ownerId = row[ownerId] ?: 0,
        parentIds = row[parentIds] ?: setOf(),
        childrenIds = row[childrenIds] ?: setOf(),
        isRemove = row[isRemove] ?: false,
        createTime = row[createTime] ?: defaultDatetime,
        updateTime = row[updateTime] ?: defaultDatetime
    )
}
