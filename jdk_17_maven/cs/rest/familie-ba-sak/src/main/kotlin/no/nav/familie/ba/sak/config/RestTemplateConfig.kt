package no.nav.familie.ba.sak.config

import no.nav.familie.http.interceptor.BearerTokenClientCredentialsClientInterceptor
import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration

@Configuration
@Import(
    ConsumerIdClientInterceptor::class,
    BearerTokenClientInterceptor::class,
    MdcValuesPropagatingClientInterceptor::class,
    BearerTokenClientCredentialsClientInterceptor::class,
)
@Profile("!mock-rest-template-config")
class RestTemplateConfig {

    @Bean("jwtBearerClientCredentials")
    fun restTemplateJwtBearerClientCredentials(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientCredentialsClientInterceptor: BearerTokenClientCredentialsClientInterceptor,
    ): RestOperations {
        return RestTemplateBuilder()
            .interceptors(
                consumerIdClientInterceptor,
                bearerTokenClientCredentialsClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            )
            .additionalMessageConverters(
                ByteArrayHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(objectMapper),
            )
            .build()
    }

    @Bean("jwtBearer")
    fun restTemplateJwtBearer(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestOperations {
        return RestTemplateBuilder()
            .interceptors(
                consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            )
            .additionalMessageConverters(
                ByteArrayHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(objectMapper),
            )
            .build()
    }

    @Bean("jwtBearerMedLangTimeout")
    fun restTemplateJwtBearerMedLangTimeout(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestOperations {
        return RestTemplateBuilder()
            .setReadTimeout(Duration.ofMinutes(12L))
            .setConnectTimeout(Duration.ofMinutes(12L))
            .interceptors(
                consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            )
            .additionalMessageConverters(
                ByteArrayHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(objectMapper),
            )
            .build()
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return restTemplate
    }

    @Bean
    fun restOperations(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    ): RestOperations {
        return RestTemplateBuilder()
            .interceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
            .additionalMessageConverters(
                ByteArrayHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(objectMapper),
            )
            .build()
    }

    @Bean
    fun restTemplateBuilderMedProxy(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    ): RestTemplateBuilder {
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5))
            .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
    }

    companion object {
        const val RETRY_BACKOFF_500MS = "\${retry.backoff.delay:500}"
    }
}

val restTemplate = RestTemplate(
    listOf(
        StringHttpMessageConverter(StandardCharsets.UTF_8),
        ByteArrayHttpMessageConverter(),
        MappingJackson2HttpMessageConverter(objectMapper),
    ),
)
