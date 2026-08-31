package com.githubrepo.util

import org.springframework.http.HttpHeaders

/** Parses the RFC 5988 `Link` header GitHub uses for pagination and extracts the `rel="next"` URL, if any. */
fun extractNextLink(headers: HttpHeaders): String? {
    val linkHeader = headers.getFirst(HttpHeaders.LINK) ?: return null
    return linkHeader.split(",")
        .map { it.trim() }
        .firstOrNull { it.contains("rel=\"next\"") }
        ?.substringAfter('<')
        ?.substringBefore('>')
}