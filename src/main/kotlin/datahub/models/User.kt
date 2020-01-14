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

import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.jackson.json
import me.liuwj.ktorm.schema.*
import java.time.LocalDateTime

data class User(
    val id: Int,
    val groupIds: Set<Int>,
    val name: String,
    val email: String,
    val password: String,
    val isRemove: Boolean,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)

@ColumnsDef("""
    id              int             comment 'user ID' auto_increment primary key,
    group_ids       text            comment 'list of group ID which user affiliate',
    name            varchar(256)    comment 'user name',
    email           varchar(256)    comment 'email, also use for login',
    password        varchar(256)    comment 'encrypted password for login',
    is_remove       tinyint         comment 'whether user is removed',
    create_time     datetime        comment 'user create time',
    update_time     datetime        comment 'last update time',
    key idx_name(is_remove, name)
""")
object Users : BaseTable<User>("users") {
    val id by int("id").primaryKey()
    val groupIds by json("group_ids", typeRef<Set<Int>>())
    val name by varchar("name")
    val email by varchar("email")
    val password by varchar("password")
    val isRemove by boolean("is_remove")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")

    val defaultDatetime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = User(
        id = row[id] ?: 0,
        groupIds = row[groupIds] ?: setOf(),
        name = row[name] ?: "",
        email = row[email] ?: "",
        password = row[password] ?: "",
        isRemove = row[isRemove] ?: false,
        createTime = row[createTime] ?: defaultDatetime,
        updateTime = row[updateTime] ?: defaultDatetime
    )

    val DDL get() = ""
}

