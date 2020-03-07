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

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import me.liuwj.ktorm.jackson.KtormModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.format.DateTimeFormatter
import javax.servlet.*
import javax.servlet.http.HttpServletRequest


/**
 * @author Jensen Qi
 * @since 1.0.0
 */
@SpringBootApplication
@Configuration
open class RestfulServer : WebMvcConfigurer, Filter {

    @Bean
    open fun ormModule(): Module = KtormModule()

    @Bean
    open fun datetimeFormat() = Jackson2ObjectMapperBuilderCustomizer {
        it.simpleDateFormat("yyyy-MM-dd HH:mm:ss")
        it.serializers(LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        it.serializers(LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
    }

    @Bean
    open fun registerFilter() = FilterRegistrationBean(RestfulServer()).also {
        it.setName("RewriteFilter")
        it.addUrlPatterns("/*")
        it.order = 1
    }

    private fun String.startsWithAny(vararg prefix: String) = prefix.any { this.startsWith(it) }

    override fun init(cfg: FilterConfig) {
        ObjectMapper().findAndRegisterModules()
    }

    override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
        req as HttpServletRequest
        if (req.servletPath.startsWithAny("/api/")) {
            chain.doFilter(req, resp)
        } else if (req.servletPath.startsWithAny("/index.html", "/favicon.ico", "/static/", "/inject.js.map", "/login")) {
            chain.doFilter(req, resp)
        } else {
            req.getRequestDispatcher("/index.html").forward(req, resp)
        }
    }

    override fun destroy() {}

}
