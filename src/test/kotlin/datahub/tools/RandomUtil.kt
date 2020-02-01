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
package datahub.tools

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.util.Sets
import java.util.HashSet
import kotlin.random.Random

/**
 * @author Jensen Qi 2020/01/31
 * @since 1.0.0
 */
object RandomUtil {
    fun randomSet(size: Int = 10, from: Int = 1, until: Int = 10) = mutableSetOf<Int>().also {
        for (i in 0..size) {
            it.add(Random.nextInt(from, until))
        }
    }

    fun randomAlphabetic(length: Int = 10) = RandomStringUtils.randomAlphabetic(length)!!

    fun randomEmail() = "${randomAlphabetic(10)}@${randomAlphabetic(5)}.com"


}