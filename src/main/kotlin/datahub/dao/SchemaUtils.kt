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

import com.alibaba.druid.pool.DruidDataSourceFactory
import com.google.common.base.Charsets
import com.google.common.io.Resources
import datahub.api.auth.MD5
import datahub.models.File
import datahub.models.Group
import datahub.models.User
import datahub.models.dtype.FileType
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.schema.Table
import org.apache.log4j.Logger
import org.reflections.Reflections
import java.io.FileReader
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.full.findAnnotation


/**
 * @author Jensen Qi
 * @since 1.0.0
 */
object SchemaUtils {
    private val logger = Logger.getLogger(this.javaClass)
    private const val lightBlue = "\u001B[1;94m"
    private const val end = "\u001B[m"

    private val dbName = "datahub" //todo: read by config
    val props = Properties().also { it.load(FileReader(Resources.getResource("druid.properties").file)) }
    private val db = Database.connect(DruidDataSourceFactory.createDataSource(props))


    /**
     * listing all ORM model define in datahub.dao package
     */
    private val models = Reflections("datahub.dao").getSubTypesOf(Table::class.java).map {
        it.getField("INSTANCE").get(it) as Table<*>
    }


    private val Table<*>.DDL: String
        get() = "${this.tableName}(${this::class.findAnnotation<ColumnsDef>()!!.columns})"

    fun String.withDB(dbName: String) = "create table if not exists $dbName.$this default charset=utf8mb4"


    fun buildDB() = db.useConnection { conn ->
        logger.info("create database $dbName")
        conn.prepareStatement("create database if not exists $dbName default character set = 'utf8'").use { it.execute() }
        logger.info("database $dbName have been created")
        models.forEach { table ->
            val createStatement = table.DDL.withDB(dbName)
            logger.info("create table for class ${table.javaClass.name}:\n" + lightBlue + createStatement + end)
            conn.prepareStatement(createStatement).use { it.execute() }
            logger.info("table $dbName.${table.tableName} have been created")
        }
        conn.catalog = dbName
    }

    fun initDB() {
        val group = Group {
            name = "root"
            isRemove = false
            createTime = LocalDateTime.now()
            updateTime = LocalDateTime.now()
        }
        Groups.add(group)
        logger.info("group root created")

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
        logger.info("user root created")

        val file = File {
            this.groupId = group.id
            this.ownerId = user.id
            this.name = "${user.name}_project"
            this.type = FileType.DIR
            this.version = null
            this.parentId = null
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Files.add(file)
        logger.info("dir root_project created")
    }


    fun cleanDB() = db.useConnection { conn ->
        models.forEach { table ->
            logger.info("drop table for class ${table.javaClass.name}")
            conn.prepareStatement("drop table if exists $dbName.${table.tableName}").use { it.execute() }
            logger.info("table $dbName.${table.tableName} have been drop")
        }
        logger.info("drop database $dbName")
        conn.prepareStatement("drop database if exists $dbName").use { it.execute() }
        logger.info("database $dbName have been drop")
    }

    fun rebuildDB() = cleanDB().also { buildDB() }

    fun mockDB() = models.forEach {
        TODO()
    }

    fun loadTable(tableName: String, filePath: String) = db.useConnection { conn ->
        logger.info("load table $tableName from $filePath")
        val table = conn.prepareStatement("select * from $tableName").metaData
        val types = (1..table.columnCount).map { table.getColumnTypeName(it)!! }

        val file = Resources.readLines(java.io.File(filePath).toURI().toURL(), Charsets.UTF_8)
        file.removeAt(0)

        val values = file.joinToString(",") { line ->
            line.split("\t").mapIndexed { i, value ->
                val type = types[i]
                if (type.contains("CHAR") || type.contains("DATE") || type.contains("TEXT")) {
                    "'$value'"
                } else {
                    value
                }
            }.joinToString(",", "(", ")")
        }
        conn.prepareStatement("insert into $tableName values $values").use { it.execute() }

        logger.info("file $filePath has been load into table $tableName")
    }

}

