package com.githubrepo.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Partial mapping of GitHub's "Repository" schema — only the fields this service needs.
 * See: https://docs.github.com/en/rest/repos/repos#list-repositories-for-a-user
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubRepositoryDto(
    val name: String,
    val fork: Boolean,
    val owner: GithubOwnerDto,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubOwnerDto(
    val login: String,
)
