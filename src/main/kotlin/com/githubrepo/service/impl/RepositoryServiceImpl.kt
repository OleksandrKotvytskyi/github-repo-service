package com.githubrepo.service.impl

import com.githubrepo.client.GithubClient
import com.githubrepo.config.GithubProperties
import com.githubrepo.service.RepositoryService
import com.githubrepo.service.mapper.toRepositoryResponse
import com.githubrepo.web.dto.RepositoryResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class RepositoryServiceImpl(
    private val githubClient: GithubClient,
    private val properties: GithubProperties,
) : RepositoryService {

    override fun getNonForkRepositories(username: String): Flux<RepositoryResponse> =
        githubClient.getUserRepositories(username)
            .filter { repository -> !repository.fork }
            .flatMap(
                { repository ->
                    toRepositoryResponse(
                        githubClient.getBranches(repository.owner.login, repository.name), repository
                    )
                },
                properties.branchFetchConcurrency)
}