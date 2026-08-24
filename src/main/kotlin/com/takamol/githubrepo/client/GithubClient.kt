package com.takamol.githubrepo.client

import com.takamol.githubrepo.client.dto.GithubBranchDto
import com.takamol.githubrepo.client.dto.GithubRepositoryDto
import com.takamol.githubrepo.config.GithubProperties
import com.takamol.githubrepo.exception.GithubUpstreamException
import com.takamol.githubrepo.exception.GithubUserNotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux

/**
 * Thin, low-level client around the public GitHub REST API. It is intentionally
 * unaware of our own API contract — it only maps GitHub's JSON responses onto
 * internal DTOs and normalises failures into our own exception types.
 */
@Component
class GithubClient(
    private val githubWebClient: WebClient,
    private val properties: GithubProperties,
) {

    /**
     * Lists the repositories owned by [username]. Follows pagination (`Link` header)
     * until all pages have been consumed.
     *
     * @throws GithubUserNotFoundException if GitHub responds 404 for this user
     * @throws GithubUpstreamException for any other non-2xx response
     */
    fun getUserRepositories(username: String): Flux<GithubRepositoryDto> =
        fetchAllPages(
            "/users/$username/repos?per_page=${properties.perPage}&type=owner",
            GithubRepositoryDto::class.java,
        ).onErrorMap(WebClientResponseException.NotFound::class.java) {
            GithubUserNotFoundException(username)
        }.onErrorMap(WebClientResponseException::class.java, ::toUpstreamException)

    /**
     * Lists branches of [owner]/[repo], each including the SHA of its latest commit.
     *
     * @throws GithubUpstreamException for any non-2xx response
     */
    fun getBranches(owner: String, repo: String): Flux<GithubBranchDto> =
        fetchAllPages(
            "/repos/$owner/$repo/branches?per_page=${properties.perPage}",
            GithubBranchDto::class.java,
        ).onErrorMap(WebClientResponseException::class.java, ::toUpstreamException)

    private fun toUpstreamException(ex: WebClientResponseException): GithubUpstreamException =
        GithubUpstreamException(
            ex.statusCode,
            "GitHub API responded with ${ex.statusCode.value()}: ${ex.responseBodyAsString.ifBlank { ex.statusText }}",
            ex,
        )

    private fun <T : Any> fetchAllPages(uri: String, type: Class<T>): Flux<T> =
        githubWebClient.get().uri(uri).exchangeToFlux { response ->
            if (response.statusCode().is2xxSuccessful) {
                val nextPageUri = extractNextLink(response.headers().asHttpHeaders())
                val page = response.bodyToFlux(type)
                if (nextPageUri != null) page.concatWith(fetchAllPages(nextPageUri, type)) else page
            } else {
                response.createException().flatMapMany { Flux.error(it) }
            }
        }

    /** Parses the RFC 5988 `Link` header GitHub uses for pagination and extracts the `rel="next"` URL, if any. */
    private fun extractNextLink(headers: HttpHeaders): String? {
        val linkHeader = headers.getFirst(HttpHeaders.LINK) ?: return null
        return linkHeader.split(",")
            .map { it.trim() }
            .firstOrNull { it.contains("rel=\"next\"") }
            ?.substringAfter('<')
            ?.substringBefore('>')
    }

    companion object {
        val RATE_LIMIT_STATUSES = setOf(HttpStatus.FORBIDDEN, HttpStatus.TOO_MANY_REQUESTS)
    }
}
