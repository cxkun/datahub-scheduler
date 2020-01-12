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
package datahub.scheduler.tracker

import datahub.models.Tasks
import datahub.scheduler.ops.OperatorType
import datahub.scheduler.ops.VirtualOperator
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.findList
import org.apache.log4j.Logger
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory


class JobTracker {
    private val logger: Logger = Logger.getLogger(JobTracker::class.java)
    val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()


    fun start() {
        logger.info("DataHub Scheduler start")
        scheduler.start()
        scheduler.triggerVirtualOperator()
    }

    private fun Scheduler.triggerVirtualOperator() {
        Tasks.findList { it.type eq OperatorType.Virtual }.forEach {
            VirtualOperator(it).triggerChildren(this)
        }
    }

}