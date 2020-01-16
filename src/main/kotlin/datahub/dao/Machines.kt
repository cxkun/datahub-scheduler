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

import datahub.models.Machine
import me.liuwj.ktorm.schema.*


@ColumnsDef("""
    id              int unsigned    comment 'machine ID' auto_increment primary key,
    hostname        varchar(128)    comment 'hostname',
    mac             varchar(17)     comment 'MAC address',
    ip              varchar(15)     comment 'IP address',
    cpu_load        tinyint         comment 'cpu load, range [0, 100]%',
    mem_load        tinyint         comment 'memory load, range [0, 100]%',
    disk_load       tinyint         comment 'disk usage, range [0, 100]%',
    is_remove       bool            comment 'whether machine is removed',
    create_time     datetime        comment 'task create time',
    update_time     datetime        comment 'last update time',
    key idx_type(is_remove, hostname)
""")
object Machines : Table<Machine>("machines") {
    val id by int("id").primaryKey().bindTo { it.id }
    val hostname by varchar("hostname").bindTo { it.hostname }
    val mac by varchar("mac").bindTo { it.mac }
    val ip by varchar("ip").bindTo { it.ip }
    val cpuLoad by int("cpu_load").bindTo { it.cpuLoad }
    val memLoad by int("mem_load").bindTo { it.memLoad }
    val diskUsage by int("disk_usage").bindTo { it.diskUsage }
    val isRemove by boolean("is_remove").bindTo { it.isRemove }
    val createTime by datetime("create_time").bindTo { it.createTime }
    val updateTime by datetime("update_time").bindTo { it.updateTime }
}
