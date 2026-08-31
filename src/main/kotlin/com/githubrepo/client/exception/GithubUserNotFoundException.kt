package com.githubrepo.client.exception

/**
 * Thrown when the requested GitHub user/organisation does not exist (GitHub responded 404).
 * Mapped to HTTP 404 by the global exception handler.
 */
class GithubUserNotFoundException(username: String) :
    RuntimeException("GitHub user '$username' was not found")
