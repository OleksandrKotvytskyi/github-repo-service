package com.takamol.githubrepo.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(GithubProperties::class)
class GithubWebClientConfig {

    @Bean
    fun githubWebClient(builder: WebClient.Builder, properties: GithubProperties): WebClient {
        val webClientBuilder = builder
            .baseUrl(properties.baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")

        if (properties.token.isNotBlank()) {
            webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.token}")
        }

        return webClientBuilder.build()
    }
}
