package br.com.odin.general.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun odinOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Odin General API")
                    .version("v1")
                    .description("API principal do Odin para clientes web/mobile e orquestracao da Service API."),
            )
}
