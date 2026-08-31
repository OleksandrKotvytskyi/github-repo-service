package com.githubrepo.web

import com.githubrepo.service.RepositoryService
import com.githubrepo.web.dto.ErrorResponse
import com.githubrepo.web.dto.RepositoryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class RepositoryController(private val repositoryService: RepositoryService) {

    @Operation(
        summary = "List a user's non-fork repositories",
        description = "Returns every repository owned by the given GitHub user that is not a fork, " +
            "together with its branches and the SHA of the latest commit on each branch.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Repositories found (possibly empty)"),
        ApiResponse(
            responseCode = "404",
            description = "The GitHub user does not exist",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "406",
            description = "Client requested a media type other than application/json",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "429",
            description = "GitHub API rate limit exceeded",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "502",
            description = "GitHub API returned an unexpected error",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @GetMapping("/api/v1/users/{username}/repositories", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getRepositories(@PathVariable username: String): Flux<RepositoryResponse> =
        repositoryService.getNonForkRepositories(username)
}
