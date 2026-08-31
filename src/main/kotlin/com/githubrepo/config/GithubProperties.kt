package com.githubrepo.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for connecting to the GitHub REST API.
 *
 * [token] is optional. Without it, requests are subject to GitHub's unauthenticated
 * rate limit (60 requests/hour per IP). Supplying a personal access token via the
 * `GITHUB_TOKEN` environment variable raises this to 5000 requests/hour.
 */
@ConfigurationProperties(prefix = "github.api")
data class GithubProperties(
    val baseUrl: String = "https://api.github.com",
    val token: String = "",
    val perPage: Int = 100,
    /** Max number of repositories whose branches are fetched concurrently. */
    val branchFetchConcurrency: Int = 8,
)
