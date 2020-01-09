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

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.schema.BaseTable
import org.apache.log4j.Logger
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(value = AnnotationRetention.RUNTIME)
annotation class ColumnsDef(val columns: String)


val BaseTable<*>.DDL: String
    get() = "${this.tableName}(${this::class.findAnnotation<ColumnsDef>()!!.columns})"

fun String.withDB(dbName: String) = "create table if not exists $dbName.$this default charset=utf8mb4"

fun BaseTable<*>.mockRecord(rowCount: Int) {

}


object SchemaUtils {
    private val logger = Logger.getLogger(this.javaClass)

    // todo: read config from properties file
    private val db = Database.connect(
        url = "jdbc:mysql://localhost:3306",
        driver = "com.mysql.jdbc.Driver",
        user = "root",
        password = "root"
    )

    private val models = listOf(Tasks, Groups, Instances, Jobs, Machines, Users)

    fun buildDB() = db.useConnection { conn ->
        logger.info("create database datahub")
        conn.prepareStatement("create database if not exists datahub").use { it.execute() }
        logger.info("database datahub have been created")
        models.forEach { table ->
            val createStatement = table.DDL.withDB("datahub")
            logger.info("create table for class ${table.javaClass.name}:\n$createStatement")
            conn.prepareStatement(createStatement).use { it.execute() }
            logger.info("table datahub.${table.tableName} have been created")
        }
    }


    fun cleanDB() = db.useConnection { conn ->
        models.forEach { table ->
            logger.info("drop table for class ${table.javaClass.name}")
            conn.prepareStatement("drop table datahub.${table.tableName}").use { it.execute() }
            logger.info("table datahub.${table.tableName} have been drop")
        }
        logger.info("drop database datahub")
        conn.prepareStatement("drop database datahub").use { it.execute() }
        logger.info("database datahub have been drop")
    }

    fun rebuildDB() = cleanDB().also { buildDB() }

    fun mockDB() = models.forEach { it.mockRecord(100) }

}

