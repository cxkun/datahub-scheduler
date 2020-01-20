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

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter
import org.springframework.util.AntPathMatcher
import java.lang.Exception
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

/**
 * @author Jensen Qi 2020/01/18
 * @since 1.0.0
 */
class JwtFilter : BasicHttpAuthenticationFilter() {
    private val token = "TOKEN"

    private val mather = AntPathMatcher()

    override fun isAccessAllowed(req: ServletRequest?, resp: ServletResponse?, mappedValue: Any?) = when {
        listOf("/static/**", "/login", "/api/login").any {
            mather.match(it, req.asHttp.requestURI)
        } -> true
        isLoginAttempt(req, resp) -> executeLogin(req, resp)
        else -> false
    }

    override fun isLoginAttempt(req: ServletRequest?, resp: ServletResponse?) = req.asHttp.getHeader(token).isNotNull()

    override fun executeLogin(req: ServletRequest?, resp: ServletResponse?) = try {
        getSubject(req, resp).login(JwtToken(req.asHttp.getHeader(token)))
        true
    } catch (e: Exception) {
        false
    }

    private val ServletRequest?.asHttp: HttpServletRequest
        get() {
            this as HttpServletRequest
            return this
        }

    private fun String?.isNotNull() = this != null

}





