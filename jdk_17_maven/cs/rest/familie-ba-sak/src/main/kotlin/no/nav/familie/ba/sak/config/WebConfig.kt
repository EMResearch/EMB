package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.http.interceptor.RolletilgangInterceptor
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Import(OIDCUtil::class, RolleConfig::class)
class WebConfig(
    private val rolleConfig: RolleConfig,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(RolletilgangInterceptor(rolleConfig))
            .excludePathPatterns("/api/task/**")
            .excludePathPatterns("/api/v2/task/**")
            .excludePathPatterns("/internal")
            .excludePathPatterns("/testverktoy")
            .excludePathPatterns("/api/feature")
        super.addInterceptors(registry)
    }
}
