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

import datahub.dao.Users
import datahub.models.User
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.findOne
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.authz.SimpleAuthorizationInfo
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection

/**
 * @author Jensen Qi 2020/01/18
 * @since 1.0.0
 */
class ShiroRealm : AuthorizingRealm() {
    override fun doGetAuthenticationInfo(authToken: AuthenticationToken?): AuthenticationInfo {
        val token = authToken?.credentials.toString()
        val user = Users.findOne { it.name eq Jwt.getUserName(token).orEmpty() }
        if (Jwt.verify(token, user?.name.orEmpty(), user?.password.orEmpty())) {
            return SimpleAuthenticationInfo(token, token, "shiro_realm")
        } else {
            throw AuthenticationException("wrong token")
        }
    }

    override fun doGetAuthorizationInfo(token: PrincipalCollection?): AuthorizationInfo {
        val user = Users.findOne { it.name eq Jwt.getUserName(token.toString()).orEmpty() }
        val isRootGroup = user?.groupIds?.contains(1) ?: false
        return if (isRootGroup) {
            SimpleAuthorizationInfo().also {
                it.roles = setOf("root")
                it.stringPermissions = setOf("root")
            }
        } else {
            SimpleAuthorizationInfo().also {
                it.roles = setOf()
                it.stringPermissions = setOf()
            }
        }
    }

    override fun supports(token: AuthenticationToken?) = token is JwtToken

}