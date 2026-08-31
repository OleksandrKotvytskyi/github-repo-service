package com.githubrepo.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun githubRepoServiceOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("GitHub Repository Service")
                .description(
                    "Exposes non-fork GitHub repositories for a given user, together with " +
                        "their branches and the latest commit SHA on each branch."
                )
                .version("v1")
                .contact(Contact().name("Takamol"))
        )
}
