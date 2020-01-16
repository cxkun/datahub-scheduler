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

import datahub.models.User
import me.liuwj.ktorm.jackson.json
import me.liuwj.ktorm.schema.*


@ColumnsDef("""
    id              int unsigned    comment 'user ID' auto_increment primary key,
    group_ids       text            comment 'list of group ID which user affiliate',
    name            varchar(256)    comment 'user name',
    email           varchar(256)    comment 'email, also use for login',
    password        varchar(256)    comment 'encrypted password for login',
    is_remove       bool            comment 'whether user is removed',
    create_time     datetime        comment 'user create time',
    update_time     datetime        comment 'last update time',
    key idx_name(is_remove, name)
""")
object Users : Table<User>("users") {
    val id by int("id").primaryKey().bindTo { it.id }
    val groupIds by json("group_ids", typeRef<Set<Int>>()).bindTo { it.groupIds }
    val name by varchar("name").bindTo { it.name }
    val email by varchar("email").bindTo { it.email }
    val password by varchar("password").bindTo { it.password }
    val isRemove by boolean("is_remove").bindTo { it.isRemove }
    val createTime by datetime("create_time").bindTo { it.createTime }
    val updateTime by datetime("update_time").bindTo { it.updateTime }
}
