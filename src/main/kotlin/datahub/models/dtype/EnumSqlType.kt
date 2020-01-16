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
package datahub.models.dtype

import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.TypeReference
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types


// Will remove when ktorm version 2.7 release
fun <E : Any, C : Enum<C>> BaseTable<E>.enum(
    name: String,
    typeRef: TypeReference<C>
): BaseTable<E>.ColumnRegistration<C> {
    return registerColumn(name, EnumSqlType(typeRef.referencedType as Class<C>))
}

class EnumSqlType<C : Enum<C>>(val enumClass: Class<C>) : SqlType<C>(Types.VARCHAR, "varchar") {
    private val valueOf = enumClass.getDeclaredMethod("valueOf", String::class.java)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: C) {
        ps.setString(index, parameter.name)
    }

    override fun doGetResult(rs: ResultSet, index: Int): C? {
        return rs.getString(index)?.takeIf { it.isNotBlank() }?.let { enumClass.cast(valueOf(null, it)) }
    }
}
