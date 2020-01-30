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

import datahub.api.auth.MD5
import datahub.models.Group
import datahub.models.User
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.schema.Table
import org.apache.log4j.Logger
import org.reflections.Reflections
import java.time.LocalDateTime
import kotlin.reflect.full.findAnnotation


/**
 * @author Jensen Qi
 * @since 1.0.0
 */
object SchemaUtils {
    private val logger = Logger.getLogger(this.javaClass)
    private const val lightBlue = "\u001B[1;94m"
    private const val end = "\u001B[m"
    private val models = Reflections("datahub.dao").getSubTypesOf(Table::class.java).map {
        it.getField("INSTANCE").get(it) as Table<*>
    }

    private val Table<*>.DDL: String
        get() = "${this.tableName}(${this::class.findAnnotation<ColumnsDef>()!!.columns})"

    fun String.withDB(dbName: String) = "create table if not exists $dbName.$this default charset=utf8mb4"


    // todo: read config from properties file
    private val db = Database.connect(
        url = "jdbc:mysql://localhost:3306",
        driver = "com.mysql.jdbc.Driver",
        user = "root",
        password = "root"
    )


    fun buildDB() = db.useConnection { conn ->
        logger.info("create database datahub")
        conn.prepareStatement("create database if not exists datahub default character set = 'utf8'").use { it.execute() }
        logger.info("database datahub have been created")
        models.forEach { table ->
            val createStatement = table.DDL.withDB("datahub")
            logger.info("create table for class ${table.javaClass.name}:\n" + lightBlue + createStatement + end)
            conn.prepareStatement(createStatement).use { it.execute() }
            logger.info("table datahub.${table.tableName} have been created")
        }
        conn.prepareStatement("use datahub").use { it.execute() }
        Database.connect(
            url = "jdbc:mysql://localhost:3306/datahub",
            driver = "com.mysql.jdbc.Driver",
            user = "root",
            password = "root"
        )

        val group = Group {
            name = "root"
            isRemove = false
            createTime = LocalDateTime.now()
            updateTime = LocalDateTime.now()
        }
        val user = User {
            this.groupIds = setOf(group.id)
            this.name = "root"
            this.email = "root@datahub.com"
            this.password = MD5.encrypt("root")
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Users.add(user)
    }


    fun cleanDB() = db.useConnection { conn ->
        models.forEach { table ->
            logger.info("drop table for class ${table.javaClass.name}")
            conn.prepareStatement("drop table if exists datahub.${table.tableName}").use { it.execute() }
            logger.info("table datahub.${table.tableName} have been drop")
        }
        logger.info("drop database datahub")
        conn.prepareStatement("drop database if exists datahub").use { it.execute() }
        logger.info("database datahub have been drop")
    }

    fun rebuildDB() = cleanDB().also { buildDB() }

    fun mockDB() = models.forEach {
        TODO()
    }

}

