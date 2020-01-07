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

import me.liuwj.ktorm.schema.BaseTable
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(value = AnnotationRetention.RUNTIME)
annotation class ColumnsDef(val columns: String)


val BaseTable<*>.DDL: String
    get() = "${this.tableName}(${this::class.findAnnotation<ColumnsDef>()!!.columns})"


fun String.withDB(dbName: String) = "create table if not exists $dbName.$this default charset=utf8mb4"

