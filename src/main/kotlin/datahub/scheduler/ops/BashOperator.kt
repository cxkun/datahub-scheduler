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
import org.apache.commons.exec.*
import org.apache.commons.exec.environment.EnvironmentUtils
import org.apache.commons.io.IOUtils
import org.quartz.JobExecutionContext
import java.io.ByteArrayOutputStream
import java.io.IOException


class BashOperator(task: Task, command: String = "echo ") : Operator(task) {
    private val EXIT_VALUE = 0
    private val commandLine: CommandLine = CommandLine("bash")
    private val std: ByteArrayOutputStream
    private val handle: DefaultExecuteResultHandler
    private val killer: ExecuteWatchdog
    private val executor: DefaultExecutor


    init {
        commandLine.addArgument("-c")
        commandLine.addArgument(command, false)
        this.std = ByteArrayOutputStream()
        this.handle = DefaultExecuteResultHandler()
        this.killer = ExecuteWatchdog(Long.MAX_VALUE)
        this.executor = DefaultExecutor()
        this.executor.setExitValue(EXIT_VALUE)
        this.executor.streamHandler = PumpStreamHandler(std, std)
        this.executor.watchdog = killer
    }

    val output: String
        get() {
            return this.std.toString()
        }

    val isFinish: Boolean
        get() {
            return this.handle.hasResult() || this.killer.killedProcess()
        }

    val isSuccess: Boolean
        get() {
            return try {
                this.isFinish && this.handle.exitValue == EXIT_VALUE
            } catch (e: IllegalStateException) {
                false
            }
        }

    fun kill() {
        try {
            // we need to get the watch dog process first
            var field = ExecuteWatchdog::class.java.getDeclaredField("process")
            field.isAccessible = true
            val process = field.get(this.killer)

            // then get the pid of process
            field = process.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            val pid = field.getInt(process)

            // finally, we get the process tree of pid and kill each of them
            val pstree = Runtime.getRuntime().exec("pstree -p $pid")
            pstree.waitFor()

            """(?<=\()[^)]+""".toRegex().findAll(IOUtils.toString(pstree.inputStream)).forEach {
                val kill = Runtime.getRuntime().exec("kill -9 ${it.value}")
                kill.waitFor()
            }

        } catch (e: IllegalAccessException) {
            logger.error("get watch dog process failed")
        } catch (e: InterruptedException) {
            logger.error("kill pstree failed")
        } catch (e: NoSuchFieldException) {
            logger.error("get pid field from watch dog failed")
        } catch (e: IOException) {
            logger.error("parse pstree result failed")
        }
    }


    override fun process(context: JobExecutionContext?) {
        try {
            executor.execute(commandLine, EnvironmentUtils.getProcEnvironment(), this.handle)
        } catch (e: IOException) {
            this.executor.setExitValue(-1)
            // todo: 日志打印
        }
        this.handle.waitFor()
    }

}