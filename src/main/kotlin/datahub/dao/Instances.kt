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

import datahub.models.Instance
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.boolean
import me.liuwj.ktorm.schema.datetime
import me.liuwj.ktorm.schema.int


/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@ColumnsDef("""
    id              bigint unsigned comment 'instance ID' auto_increment primary key,
    job_id          bigint unsigned comment 'job ID',
    is_remove       bool            comment 'whether instance is removed',
    create_time     datetime        comment 'instance create time',
    update_time     datetime        comment 'last update time'
""")
object Instances : Table<Instance>("instances") {
    val id by int("id").primaryKey().bindTo { it.id }
    val jobId by int("job_id").bindTo { it.jobId }
    val isRemove by boolean("is_remove").bindTo { it.isRemove }
    val createTime by datetime("create_time").bindTo { it.createTime }
    val updateTime by datetime("update_time").bindTo { it.updateTime }
}