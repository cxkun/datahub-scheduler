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

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*
import java.time.LocalDateTime

/**
 *  Table of privilege groups, a user might belong to a set of privilege groups
 */


interface Group : Entity<Group> {
    companion object : Entity.Factory<Group>()

    val id: Int
    var name: String
    var isRemove: Boolean
    var createTime: LocalDateTime
    var updateTime: LocalDateTime
}



