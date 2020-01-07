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


data class Machine(
    val id: Int,
    val hostname: String,
    val mac: String,
    val ip: String,
    val cpuLoad: Int,
    val memLoad: Int,
    val diskLoad: Int,
    val isRemove: Boolean,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)

object Machines : BaseTable<Machine>("machine") {
    val id by int("id").primaryKey()
    val hostname by varchar("hostname")
    val mac by varchar("mac")
    val ip by varchar("ip")
    val cpuLoad by int("cpu_load")
    val memLoad by int("mem_load")
    val diskLoad by int("disk_load")
    val isRemove by boolean("is_remove")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")

    val defaultDatetime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Machine(
        id = row[id] ?: 0,
        hostname = row[hostname] ?: "",
        mac = row[mac] ?: "",
        ip = row[ip] ?: "",
        cpuLoad = row[cpuLoad] ?: 0,
        memLoad = row[memLoad] ?: 0,
        diskLoad = row[diskLoad] ?: 0,
        isRemove = row[isRemove] ?: false,
        createTime = row[createTime] ?: defaultDatetime,
        updateTime = row[updateTime] ?: defaultDatetime
    )
}
