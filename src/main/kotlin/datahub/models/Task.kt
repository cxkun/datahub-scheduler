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
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.jackson.json
import me.liuwj.ktorm.schema.*
import java.time.LocalDateTime

interface Task : Entity<Task> {
    companion object : Entity.Factory<Task>()

    val id: Int
    var type: OperatorType
    var name: String
    var command: String
    var ownerId: Int
    var parentIds: Set<Int>
    var childrenIds: Set<Int>
    var isRemove: Boolean
    var createTime: LocalDateTime
    var updateTime: LocalDateTime
}


