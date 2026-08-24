package com.takamol.githubrepo.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Partial mapping of GitHub's "Branch (short)" schema returned by the list-branches endpoint.
 * `commit.sha` is the SHA of the latest commit on that branch, so no additional
 * per-commit lookup is required.
 * See: https://docs.github.com/en/rest/branches/branches#list-branches
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubBranchDto(
    val name: String,
    val commit: GithubCommitRefDto,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommitRefDto(
    val sha: String,
)
