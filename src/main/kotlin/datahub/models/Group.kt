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
package datahub.scheduler.models

import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*
import java.time.LocalDateTime

/**
 *  Table of privilege groups, a user might belong to a set of privilege groups
 */


data class Group(
    val id: Int,
    val name: String,
    val isRemove: Boolean,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)

@ColumnsDef("""
    id              int             comment 'group ID' auto_increment primary key,
    name            varchar(64)     comment 'group name',
    is_remove       tinyint         comment 'whether group is removed',
    create_time     datetime        comment 'task create time',
    update_time     datetime        comment 'last update time'
""")
object Groups : BaseTable<Group>("group") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val isRemove by boolean("is_remove")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")

    val defaultDatetime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Group(
        id = row[id] ?: 0,
        name = row[name] ?: "",
        isRemove = row[isRemove] ?: false,
        createTime = row[createTime] ?: defaultDatetime,
        updateTime = row[updateTime] ?: defaultDatetime
    )
}


