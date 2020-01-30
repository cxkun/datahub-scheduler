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
package datahub.config

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.lang.Exception

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
class CliArgument(parser: ArgParser) {
    val exec by parser.storing("--exec", help = "exec command [build_db, clean_db, master, worker]") {
        when (this) {
            "build_db" -> ExecCommand.BuildDatabase
            "clean_db" -> ExecCommand.CleanDatabase
            "rebuild_db" -> ExecCommand.RebuildDatabase
            "mock_db" -> ExecCommand.MockDatabase
            "webserver" -> ExecCommand.WebServer
            "master" -> ExecCommand.Master
            "worker" -> ExecCommand.Worker
            else -> throw Exception("unsupported command $this")
        }
    }.default(ExecCommand.Worker)
}