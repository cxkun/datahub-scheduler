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
package datahub.api

/**
 * @author Jensen Qi
 * @since 1.0.0
 */
object Response {

    fun withData(status: String, data: Any) = mapOf("status" to status, "data" to data)
    fun withMessage(status: String, message: Any) = mapOf("status" to status, "message" to message)
    fun withError(status: String, message: Any) = mapOf("status" to status, "error" to message)

    object Success {
        const val status = "success"
        fun WithData(data: Any) = withData(status, data)
        fun WithMessage(message: Any) = withMessage(status, message)
        fun Update(data: Any) = withMessage(status, "$data has been update")
        fun Remove(data: Any) = withMessage(status, "$data has been removed")


    }

    object Failed {
        const val status = "failed"
        fun WithError(error: Any) = withError(status, error)
        fun DataNotFound(data: Any) = withError(status, "$data not found")
        fun PermissionDenied(message: Any) = withError(status, "permission denied: $message")
        fun IllegalArgument(message: Any) = withData(status, "illegal argument: $message")
    }

}