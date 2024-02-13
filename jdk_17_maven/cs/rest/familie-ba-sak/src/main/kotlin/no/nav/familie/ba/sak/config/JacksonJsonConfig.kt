package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonJsonConfig {

    companion object {
        private val OM = ObjectMapper()

        init {
            OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
            OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            OM.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
            OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            OM.registerModule(JavaTimeModule())
            OM.registerModule(KotlinModule.Builder().build())
        }
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return OM
    }
}
