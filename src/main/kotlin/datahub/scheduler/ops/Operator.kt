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
package datahub.scheduler.ops

import datahub.models.Task
import datahub.models.Tasks
import me.liuwj.ktorm.entity.findListByIds
import org.apache.log4j.Logger
import org.quartz.*

abstract class Operator(private val task: Task) : org.quartz.Job {

    val logger = Logger.getLogger(this.javaClass)!!
    private val childrenOperators = Tasks.findListByIds(task.childrenIds).map {
        when (it.type) {
            OperatorType.Bash -> BashOperator(task)
            OperatorType.DataX -> DataXOperator(task)
            OperatorType.Hive -> HiveOperator(task)
            OperatorType.Mail -> MailOperator(task)
            OperatorType.Python -> PythonOperator(task)
            else -> throw Exception("unsupported operator type")
        }
    }


    abstract fun process(context: JobExecutionContext?)

    override fun execute(context: JobExecutionContext?) {
        process(context)
        triggerChildren(context)
    }


    fun triggerChildren(scheduler: Scheduler) {
        childrenOperators.forEach {
            val jobDetail = JobBuilder.newJob(it.javaClass)
                .withIdentity("${it.task.id}", "group todo").build()
            val trigger = TriggerBuilder.newTrigger().startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()).build()
            scheduler.scheduleJob(jobDetail, trigger)
        }

    }

    private fun triggerChildren(context: JobExecutionContext?) {
        triggerChildren(context!!.scheduler)
    }
}