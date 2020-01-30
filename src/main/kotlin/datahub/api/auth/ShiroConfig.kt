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

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor
import org.apache.shiro.spring.web.ShiroFilterFactoryBean
import org.apache.shiro.web.mgt.DefaultWebSecurityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.servlet.Filter

/**
 * @author Jensen Qi 2020/01/18
 * @since 1.0.0
 */
@Configuration
open class ShiroConfig {
    @Bean
    open fun shiroRealm() = ShiroRealm()

    @Bean
    open fun securityManager() = DefaultWebSecurityManager().also {
        it.setRealm(shiroRealm())
    }

    @Bean
    open fun shiroFilterFactoryBean(securityManager: SecurityManager) = ShiroFilterFactoryBean().also {
        securityManager as DefaultWebSecurityManager
        it.securityManager = securityManager
        it.filters = mutableMapOf<String, Filter>("jwt" to JwtFilter())
        it.filterChainDefinitionMap = mutableMapOf("/**" to "jwt")
    }

    @Bean
    open fun authorizationAttributeSourceAdvisor(securityManager: SecurityManager) = AuthorizationAttributeSourceAdvisor().also {
        it.securityManager = securityManager
    }
}