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
package datahub.api.controller

import com.google.common.collect.Lists
import datahub.api.Response
import datahub.api.ResponseData
import datahub.api.utils.Page
import datahub.dao.Machines
import datahub.models.Machine
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.ColumnDeclaring
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/api/machine")
class MachineController {

    /**
     * @api {get} /api/machine 获取机器列表
     * @apiDescription 获取机器列表，支持分页查询和模糊查询
     * @apiGroup Machine
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} [page = 1] 分页ID
     * @apiParam {Number} [pageSize = 9999] 分页大小
     * @apiParam {String} like 机器 hostname 模糊匹配，多个词用空格分隔，null 字符串会被忽
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"count":188,"machines":[{"id":234,"hostname":"hevhjzva","mac":"50-95-F4-68-0E-DA","ip":"145.28.32.35","cpuLoad":34,"memLoad":2,"diskUsage":80,"isRemove":false,"createTime":"2009-07-03 10:34:56","updateTime":"2010-12-22 20:37:10"},{"id":235,"hostname":"djesrwkr","mac":"1F-E1-DA-20-DB-EB","ip":"95.127.187.33","cpuLoad":13,"memLoad":27,"diskUsage":35,"isRemove":false,"createTime":"2019-07-15 22:29:11","updateTime":"2019-12-13 07:56:14"}]}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @GetMapping
    fun listing(@RequestParam(required = false, defaultValue = "1") page: Int,
                @RequestParam(required = false, defaultValue = "9999") pageSize: Int,
                @RequestParam(required = false) like: String?): ResponseData {
        val machines = Machines.select().where {
            val conditions = Lists.newArrayList<ColumnDeclaring<Boolean>>(Machines.isRemove eq false)
            if (like != null && like.isNotBlank() && like.trim().toUpperCase() != "NULL") {
                like.split("\\s+".toRegex()).forEach {
                    conditions.add(Machines.hostname.like("%$it%"))
                }
            }
            conditions.reduce { a, b -> a and b }
        }
        val count = machines.totalRecords
        return Response.Success.WithData(mapOf(
            "count" to count,
            "machines" to machines.orderBy(Machines.id.asc()).limit(Page.offset(page, pageSize), pageSize).map {
                Machines.createEntity(it)
            }
        ))
    }

    /**
     * @api {get} /api/machine/{id} 查询机器
     * @apiDescription 查询指定 ID 的机器详情
     * @apiGroup Machine
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"machine":{"id":1,"hostname":"tejxajfq","mac":"1F-72-5B-F7-10-AB","ip":"107.116.90.29","cpuLoad":34,"memLoad":31,"diskUsage":63,"isRemove":false,"createTime":"2029-06-06 19:57:08","updateTime":"2032-06-08 19:36:03"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"machine 2 not found"}
     */
    @GetMapping("{id}")
    fun find(@PathVariable id: Int): ResponseData {
        val machine = Machines.findById(id)
        return if (machine == null || machine.isRemove) {
            Response.Failed.DataNotFound("machine $id")
        } else {
            Response.Success.WithData(mapOf("machine" to machine))
        }
    }

    /**
     * @api {post} /api/machine 新建机器
     * @apiDescription 新建一台调度服务器，并将新建后的信息返回
     * @apiGroup Machine
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} hostname 服务器 hostname
     * @apiParam {String} ip 服务器 IP
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"machine":{"hostname":"test","mac":"","ip":"192.168.1.1","cpuLoad":0,"memLoad":0,"diskUsage":0,"isRemove":false,"createTime":"2020-03-13 00:42:16","updateTime":"2020-03-13 00:42:16","id":248}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @PostMapping
    fun create(@RequestParam(required = true) hostname: String,
               @RequestParam(required = true) ip: String): ResponseData {
        val machine = Machine {
            this.hostname = hostname
            this.mac = ""
            this.ip = ip
            this.cpuLoad = 0
            this.memLoad = 0
            this.diskUsage = 0
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
        }
        Machines.add(machine)
        return Response.Success.WithData(mapOf("machine" to machine))
    }

    /**
     * @api {put} /api/machine/{id} 更新机器信息
     * @apiDescription 更新指定 ID 的机器信息，如果该指定机器已被删除或不存在则失败
     * @apiGroup Machine
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} [hostname = null] 机器 hostname，如果不提供则不更新
     * @apiParam {String} [ip = null] 机器 IP，如果不提供则不更新
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"machine":{"id":5,"hostname":"myjwey","mac":"7D-75-70-DE-73-0E","ip":"14.66.49.193","cpuLoad":1,"memLoad":60,"diskUsage":59,"isRemove":false,"createTime":"2004-11-28 21:50:06","updateTime":"2020-03-13 01:01:13"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"machine 2 not found"}
     */
    @PutMapping("{id}")
    fun update(@PathVariable id: Int, @RequestParam(required = false) hostname: String?,
               @RequestParam(required = false) ip: String?): ResponseData {
        val machine = Machines.findById(id)
        return if (machine == null || machine.isRemove) {
            Response.Failed.DataNotFound("machine $id")
        } else {
            var update = false
            if (hostname != null) {
                machine.hostname = hostname
                update = true
            }
            if (ip != null) {
                machine.ip = ip
                update = true
            }
            if (update) {
                machine.updateTime = LocalDateTime.now()
            }
            machine.flushChanges()
            Response.Success.WithData(mapOf("machine" to machine))
        }

    }

    /**
     * @api {delete} /api/machine/{id} 删除机器
     * @apiDescription 删除指定 ID 的机器，如果该指定机器已被删除或不存在则失败
     * @apiGroup Machine
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","message":"machine 3 has been removed"}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"machine 2 not found"}
     */
    @DeleteMapping("{id}")
    fun remove(@PathVariable id: Int): ResponseData {
        val machine = Machines.findById(id)
        return if (machine == null || machine.isRemove) {
            Response.Failed.DataNotFound("machine $id")
        } else {
            machine.isRemove = true
            machine.flushChanges()
            Response.Success.Remove("machine ${machine.id}")
        }

    }

}