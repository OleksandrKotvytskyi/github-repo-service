package com.githubrepo.web.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A non-fork GitHub repository, with all of its branches.")
data class RepositoryResponse(
    @Schema(description = "Repository name.", example = "github-repo-service")
    val name: String,
    @Schema(description = "Login of the repository owner.", example = "octocat")
    val owner: String,
    @Schema(description = "Branches belonging to this repository.")
    val branches: List<BranchResponse>,
)
