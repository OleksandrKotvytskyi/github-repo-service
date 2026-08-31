package com.githubrepo.service

import com.githubrepo.client.GithubClient
import com.githubrepo.client.dto.GithubBranchDto
import com.githubrepo.client.dto.GithubCommitRefDto
import com.githubrepo.client.dto.GithubOwnerDto
import com.githubrepo.client.dto.GithubRepositoryDto
import com.githubrepo.config.GithubProperties
import com.githubrepo.service.impl.RepositoryServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class RepositoryServiceTest {

    private val githubClient: GithubClient = mock()
    private val service = RepositoryServiceImpl(githubClient, GithubProperties())

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
