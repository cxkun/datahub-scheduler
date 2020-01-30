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

import datahub.models.Group
import me.liuwj.ktorm.schema.*

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@ColumnsDef("""
    id              int unsigned    comment 'group ID' auto_increment primary key,
    name            varchar(64)     comment 'group name',
    is_remove       bool            comment 'whether group is removed',
    create_time     datetime        comment 'task create time',
    update_time     datetime        comment 'last update time'
""")
object Groups : Table<Group>("groups") {
    val id by int("id").primaryKey().bindTo { it.id }
    val name by varchar("name").bindTo { it.name }
    val isRemove by boolean("is_remove").bindTo { it.isRemove }
    val createTime by datetime("create_time").bindTo { it.createTime }
    val updateTime by datetime("update_time").bindTo { it.updateTime }
}

