package com.takamol.githubrepo.service

import com.takamol.githubrepo.client.GithubClient
import com.takamol.githubrepo.client.dto.GithubBranchDto
import com.takamol.githubrepo.client.dto.GithubCommitRefDto
import com.takamol.githubrepo.client.dto.GithubOwnerDto
import com.takamol.githubrepo.client.dto.GithubRepositoryDto
import com.takamol.githubrepo.config.GithubProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class RepositoryServiceTest {

    private val githubClient: GithubClient = mock()
    private val service = RepositoryService(githubClient, GithubProperties())

    @Test
    fun `excludes forked repositories`() {
        whenever(githubClient.getUserRepositories("octocat")).thenReturn(
            Flux.just(
                GithubRepositoryDto(name = "own-repo", fork = false, owner = GithubOwnerDto("octocat")),
                GithubRepositoryDto(name = "forked-repo", fork = true, owner = GithubOwnerDto("octocat")),
            ),
        )
        whenever(githubClient.getBranches("octocat", "own-repo")).thenReturn(
            Flux.just(GithubBranchDto("main", GithubCommitRefDto("sha-1"))),
        )

        StepVerifier.create(service.getNonForkRepositories("octocat"))
            .expectNextMatches { it.name == "own-repo" && it.branches.size == 1 }
            .verifyComplete()
    }

    @Test
    fun `maps every branch to its latest commit sha`() {
        whenever(githubClient.getUserRepositories("octocat")).thenReturn(
            Flux.just(GithubRepositoryDto(name = "repo", fork = false, owner = GithubOwnerDto("octocat"))),
        )
        whenever(githubClient.getBranches("octocat", "repo")).thenReturn(
            Flux.just(
                GithubBranchDto("main", GithubCommitRefDto("sha-main")),
                GithubBranchDto("develop", GithubCommitRefDto("sha-develop")),
            ),
        )

        StepVerifier.create(service.getNonForkRepositories("octocat"))
            .expectNextMatches { repo ->
                repo.name == "repo" &&
                    repo.branches.any { it.name == "main" && it.lastCommitSha == "sha-main" } &&
                    repo.branches.any { it.name == "develop" && it.lastCommitSha == "sha-develop" }
            }
            .verifyComplete()
    }

    @Test
    fun `returns empty result when user has no repositories`() {
        whenever(githubClient.getUserRepositories("octocat")).thenReturn(Flux.empty())

        StepVerifier.create(service.getNonForkRepositories("octocat"))
            .verifyComplete()
    }
}
