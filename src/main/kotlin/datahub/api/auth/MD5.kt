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
package datahub.api.auth

import org.apache.shiro.crypto.hash.SimpleHash
import org.apache.shiro.util.ByteSource

/**
 * @author Jensen Qi 2020/01/18
 * @since 1.0.0
 */
object MD5 {
    private const val ALGORITHM_NAME = "MD5"
    private const val HASH_ITERATIONS = 2

    fun encrypt(password: String): String = SimpleHash(ALGORITHM_NAME, password, ByteSource.Util.bytes(password), HASH_ITERATIONS).toHex()

}