package com.takamol.githubrepo.service

import com.takamol.githubrepo.client.GithubClient
import com.takamol.githubrepo.client.dto.GithubRepositoryDto
import com.takamol.githubrepo.config.GithubProperties
import com.takamol.githubrepo.web.dto.BranchResponse
import com.takamol.githubrepo.web.dto.RepositoryResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Orchestrates calls to [GithubClient] and translates GitHub's data model into
 * this service's own API contract. This is the only layer that knows about both
 * the upstream shape and our public shape, keeping the controller and the GitHub
 * client fully decoupled from one another.
 */
@Service
class RepositoryService(
    private val githubClient: GithubClient,
    private val properties: GithubProperties,
) {

    /**
     * Returns every non-fork repository owned by [username], each with its branches
     * and the latest commit SHA on every branch. Branches for different repositories
     * are fetched concurrently (bounded by [GithubProperties.branchFetchConcurrency])
     * to keep latency low without overwhelming GitHub's rate limits.
     */
    fun getNonForkRepositories(username: String): Flux<RepositoryResponse> =
        githubClient.getUserRepositories(username)
            .filter { repository -> !repository.fork }
            .flatMap({ repository -> toRepositoryResponse(repository) }, properties.branchFetchConcurrency)

    private fun toRepositoryResponse(repository: GithubRepositoryDto) =
        githubClient.getBranches(repository.owner.login, repository.name)
            .map { branch -> BranchResponse(name = branch.name, lastCommitSha = branch.commit.sha) }
            .collectList()
            .map { branches -> RepositoryResponse(repository.name, repository.owner.login, branches) }
}
