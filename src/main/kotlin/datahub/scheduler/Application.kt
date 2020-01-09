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
package datahub.scheduler

import com.xenomachina.argparser.ArgParser
import datahub.scheduler.config.CliArgument
import datahub.scheduler.config.ExecCommand
import datahub.scheduler.models.*
import org.apache.log4j.Logger

fun main(args: Array<String>) {
    val logger = Logger.getLogger("DataHub")
    logger.info(logo)
    logger.info(color)
    ArgParser(args).parseInto(::CliArgument).run {
        when (exec) {
            ExecCommand.BuildDatabase -> SchemaUtils.buildDB()
            ExecCommand.CleanDatabase -> SchemaUtils.cleanDB()
            ExecCommand.RebuildDatabase -> SchemaUtils.rebuildDB()
            ExecCommand.MockDatabase -> SchemaUtils.mockDB()
            else -> {

            }
        }
    }
}
