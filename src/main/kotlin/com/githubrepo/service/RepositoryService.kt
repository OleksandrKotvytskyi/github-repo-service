package com.githubrepo.service

import com.githubrepo.config.GithubProperties
import com.githubrepo.web.dto.RepositoryResponse
import reactor.core.publisher.Flux

/**
 * Orchestrates calls to [com.githubrepo.client.GithubClient] and translates GitHub's data model into
 * this service's own API contract. This is the only layer that knows about both
 * the upstream shape and our public shape, keeping the controller and the GitHub
 * client fully decoupled from one another.
 */
interface RepositoryService {
    /**
     * Returns every non-fork repository owned by [username], each with its branches
     * and the latest commit SHA on every branch. Branches for different repositories
     * are fetched concurrently (bounded by [GithubProperties.branchFetchConcurrency])
     * to keep latency low without overwhelming GitHub's rate limits.
     */
    fun getNonForkRepositories(username: String): Flux<RepositoryResponse>
}