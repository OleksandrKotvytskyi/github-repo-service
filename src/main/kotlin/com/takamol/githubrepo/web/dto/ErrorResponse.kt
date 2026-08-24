package com.takamol.githubrepo.web.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Consistent error payload returned for every failure case.")
data class ErrorResponse(
    @Schema(description = "Time the error occurred.")
    val timestamp: Instant,
    @Schema(description = "HTTP status code.", example = "404")
    val status: Int,
    @Schema(description = "Short, human-readable status phrase.", example = "Not Found")
    val error: String,
    @Schema(description = "Human-readable explanation of what went wrong.")
    val message: String,
    @Schema(description = "Path of the request that triggered the error.", example = "/api/v1/users/octocat/repositories")
    val path: String,
)
