package com.takamol.githubrepo

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GithubRepoServiceApplicationIT {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `end to end - returns only non-fork repositories with branches and latest commit sha`() {
        server.enqueue(
            MockResponse()
                .setBody(
                    """
                    [
                        {"name": "real-project", "fork": false, "owner": {"login": "octocat"}},
                        {"name": "someones-fork", "fork": true, "owner": {"login": "octocat"}}
                    ]
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )
        server.enqueue(
            MockResponse()
                .setBody(
                    """
                    [
                        {"name": "main", "commit": {"sha": "sha-main"}},
                        {"name": "feature/x", "commit": {"sha": "sha-feature"}}
                    ]
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        webTestClient.get().uri("/api/v1/users/octocat/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].name").isEqualTo("real-project")
            .jsonPath("$[0].branches.length()").isEqualTo(2)
    }

    @Test
    fun `end to end - unknown user yields 404 with consistent error contract`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message": "Not Found"}"""))

        webTestClient.get().uri("/api/v1/users/does-not-exist/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.error").isEqualTo("Not Found")
    }

    companion object {
        private lateinit var server: MockWebServer

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server = MockWebServer()
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.shutdown()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerGithubBaseUrl(registry: DynamicPropertyRegistry) {
            registry.add("github.api.base-url") { server.url("/").toString() }
        }
    }
}
