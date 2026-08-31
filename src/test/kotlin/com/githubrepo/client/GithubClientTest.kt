package com.githubrepo.client

import com.githubrepo.config.GithubProperties
import com.githubrepo.client.exception.GithubUpstreamException
import com.githubrepo.client.exception.GithubUserNotFoundException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import java.time.Duration

class GithubClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: GithubClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val webClient = WebClient.builder().baseUrl(server.url("/").toString()).build()
        client = GithubClient(webClient, GithubProperties(perPage = 100))
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `returns repositories from a single page`() {
        server.enqueue(
            MockResponse()
                .setBody(
                    """
                    [
                        {"name": "repo-a", "fork": false, "owner": {"login": "octocat"}},
                        {"name": "repo-b", "fork": true, "owner": {"login": "octocat"}}
                    ]
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        StepVerifier.create(client.getUserRepositories("octocat"))
            .expectNextMatches { it.name == "repo-a" && !it.fork }
            .expectNextMatches { it.name == "repo-b" && it.fork }
            .verifyComplete()
    }

    @Test
    fun `follows pagination via the Link header until exhausted`() {
        server.enqueue(
            MockResponse()
                .setBody("""[{"name": "repo-1", "fork": false, "owner": {"login": "octocat"}}]""")
                .addHeader("Content-Type", "application/json")
                .addHeader("Link", "<${server.url("/users/octocat/repos?page=2")}>; rel=\"next\""),
        )
        server.enqueue(
            MockResponse()
                .setBody("""[{"name": "repo-2", "fork": false, "owner": {"login": "octocat"}}]""")
                .addHeader("Content-Type", "application/json"),
        )

        StepVerifier.create(client.getUserRepositories("octocat"))
            .expectNextMatches { it.name == "repo-1" }
            .expectNextMatches { it.name == "repo-2" }
            .verifyComplete()

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `maps 404 to GithubUserNotFoundException`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message": "Not Found"}"""))

        StepVerifier.create(client.getUserRepositories("does-not-exist"))
            .expectError(GithubUserNotFoundException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `maps other error statuses to GithubUpstreamException`() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("service unavailable"))

        StepVerifier.create(client.getUserRepositories("octocat"))
            .expectErrorMatches { it is GithubUpstreamException }
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `returns branches with latest commit sha`() {
        server.enqueue(
            MockResponse()
                .setBody(
                    """
                    [
                        {"name": "main", "commit": {"sha": "abc123"}},
                        {"name": "dev", "commit": {"sha": "def456"}}
                    ]
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        StepVerifier.create(client.getBranches("octocat", "repo-a"))
            .expectNextMatches { it.name == "main" && it.commit.sha == "abc123" }
            .expectNextMatches { it.name == "dev" && it.commit.sha == "def456" }
            .verifyComplete()
    }
}
