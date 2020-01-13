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

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

fun String.startsWithAny(vararg prefix: String) = prefix.any { this.startsWith(it) }

@SpringBootApplication
open class RestfulServer : WebMvcConfigurerAdapter(), Filter {


    override fun init(cfg: FilterConfig) {}

    override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
        req as HttpServletRequest
        val notDispatch = req.servletPath.startsWithAny("/index.html", "/favicon.ico", "/dist/", "/api/")
        if (notDispatch) {
            chain.doFilter(req, resp)
        } else {
            req.getRequestDispatcher("${req.contextPath}/index.html").forward(req, resp)
        }
    }

    override fun destroy() {}


    override fun addResourceHandlers(registry: ResourceHandlerRegistry?) {
        registry!!.addResourceHandler("/static/**").addResourceLocations("classpath:/static/")
        super.addResourceHandlers(registry)
    }


    @Bean
    open fun registerFilter(): FilterRegistrationBean {
        val registration = FilterRegistrationBean()
        registration.setName("RewriteFilter")
        registration.filter = RestfulServer()
        registration.addUrlPatterns("/*")
        registration.order = 1
        return registration
    }
}
