package com.githubrepo.web

import com.githubrepo.client.exception.GithubUpstreamException
import com.githubrepo.client.exception.GithubUserNotFoundException
import com.githubrepo.web.dto.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.NotAcceptableStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import java.time.Instant

/**
 * Centralises translation of every failure into a single, consistent [ErrorResponse]
 * shape, regardless of where in the stack the failure originated. This keeps the
 * controller free of try/catch noise and guarantees a uniform contract for API
 * consumers across all error classes.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /** GitHub user/organisation does not exist -> 404, the request itself was valid. */
    @ExceptionHandler(GithubUserNotFoundException::class)
    fun handleUserNotFound(ex: GithubUserNotFoundException, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, ex.message ?: "User not found", exchange)

    /**
     * GitHub API itself failed. Rate-limit responses (403/429) are surfaced as 429 so
     * clients know to back off and retry later; any other upstream failure is treated
     * as a 502, since our service is acting as a gateway to a third party we do not control.
     */
    @ExceptionHandler(GithubUpstreamException::class)
    fun handleUpstreamFailure(ex: GithubUpstreamException, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> {
        log.warn("GitHub upstream call failed: {}", ex.message)
        val isRateLimited = ex.upstreamStatus == HttpStatus.FORBIDDEN || ex.upstreamStatus == HttpStatus.TOO_MANY_REQUESTS
        val status = if (isRateLimited) HttpStatus.TOO_MANY_REQUESTS else HttpStatus.BAD_GATEWAY
        return respond(status, "The GitHub API is currently unavailable or returned an error: ${ex.message}", exchange)
    }

    /** Client asked for a representation (Accept header) this API does not produce. */
    @ExceptionHandler(NotAcceptableStatusException::class)
    fun handleNotAcceptable(ex: NotAcceptableStatusException, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_ACCEPTABLE, "Only 'application/json' responses are supported by this API.", exchange)

    /** Malformed request (bad path variable, unreadable body, etc). */
    @ExceptionHandler(ServerWebInputException::class)
    fun handleBadRequest(ex: ServerWebInputException, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, ex.reason ?: "Invalid request", exchange)

    /** Safety net: anything unexpected becomes a 500 without leaking internal details. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error handling request {}", exchange.request.path, ex)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", exchange)
    }

    private fun respond(status: HttpStatus, message: String, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = exchange.request.path.value(),
            ),
        )
}
