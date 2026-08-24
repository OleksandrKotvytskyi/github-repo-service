package com.takamol.githubrepo.web

import com.takamol.githubrepo.exception.GithubUpstreamException
import com.takamol.githubrepo.exception.GithubUserNotFoundException
import com.takamol.githubrepo.service.RepositoryService
import com.takamol.githubrepo.web.dto.BranchResponse
import com.takamol.githubrepo.web.dto.RepositoryResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

@WebFluxTest(controllers = [RepositoryController::class])
class RepositoryControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var repositoryService: RepositoryService

    @Test
    fun `returns non-fork repositories as json`() {
        whenever(repositoryService.getNonForkRepositories("octocat")).thenReturn(
            Flux.just(
                RepositoryResponse(
                    name = "repo-a",
                    owner = "octocat",
                    branches = listOf(BranchResponse("main", "sha-1")),
                ),
            ),
        )

        webTestClient.get().uri("/api/v1/users/octocat/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("repo-a")
            .jsonPath("$[0].branches[0].name").isEqualTo("main")
            .jsonPath("$[0].branches[0].lastCommitSha").isEqualTo("sha-1")
    }

    @Test
    fun `returns 404 with consistent error body when user does not exist`() {
        whenever(repositoryService.getNonForkRepositories("ghost")).thenReturn(
            Flux.error(GithubUserNotFoundException("ghost")),
        )

        webTestClient.get().uri("/api/v1/users/ghost/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.error").isEqualTo("Not Found")
            .jsonPath("$.path").isEqualTo("/api/v1/users/ghost/repositories")
    }

    @Test
    fun `returns 502 when the upstream GitHub API fails`() {
        whenever(repositoryService.getNonForkRepositories("octocat")).thenReturn(
            Flux.error(GithubUpstreamException(HttpStatus.INTERNAL_SERVER_ERROR, "boom")),
        )

        webTestClient.get().uri("/api/v1/users/octocat/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
            .expectBody()
            .jsonPath("$.status").isEqualTo(502)
    }

    @Test
    fun `returns 429 when the upstream API is rate limited`() {
        whenever(repositoryService.getNonForkRepositories("octocat")).thenReturn(
            Flux.error(GithubUpstreamException(HttpStatus.FORBIDDEN, "rate limited")),
        )

        webTestClient.get().uri("/api/v1/users/octocat/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test
    fun `returns 406 when client requests an unsupported media type`() {
        webTestClient.get().uri("/api/v1/users/octocat/repositories")
            .accept(MediaType.APPLICATION_XML)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE)
    }

    @Test
    fun `returns 500 with a generic message on unexpected failures`() {
        whenever(repositoryService.getNonForkRepositories("octocat")).thenReturn(
            Flux.error(IllegalStateException("something broke")),
        )

        webTestClient.get().uri("/api/v1/users/octocat/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody()
            .jsonPath("$.message").isEqualTo("An unexpected error occurred. Please try again later.")
    }
}
