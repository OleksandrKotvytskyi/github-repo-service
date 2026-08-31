package com.githubrepo.client.exception

import org.springframework.http.HttpStatusCode

/**
 * Thrown when a call to the GitHub API fails for a reason other than "user not found"
 * (e.g. rate limiting, GitHub 5xx errors, network/timeouts surfaced as upstream failures).
 * Mapped to HTTP 429 (rate limited) or 502 (other upstream failures) by the global handler.
 */
class GithubUpstreamException(
    val upstreamStatus: HttpStatusCode?,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
