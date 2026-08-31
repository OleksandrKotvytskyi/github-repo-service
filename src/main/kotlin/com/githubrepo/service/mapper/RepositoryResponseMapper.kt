package com.githubrepo.service.mapper

import com.githubrepo.client.dto.GithubBranchDto
import com.githubrepo.client.dto.GithubRepositoryDto
import com.githubrepo.web.dto.BranchResponse
import com.githubrepo.web.dto.RepositoryResponse
import reactor.core.publisher.Flux

fun toRepositoryResponse(branches: Flux<GithubBranchDto>, repository: GithubRepositoryDto) =
    branches.map { branch -> BranchResponse(name = branch.name, lastCommitSha = branch.commit.sha) }
        .collectList()
        .map { branches -> RepositoryResponse(repository.name, repository.owner.login, branches) }