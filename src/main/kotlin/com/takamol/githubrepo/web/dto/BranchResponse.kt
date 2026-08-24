package com.takamol.githubrepo.web.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A single branch of a repository, with the SHA of its latest commit.")
data class BranchResponse(
    @Schema(description = "Branch name.", example = "main")
    val name: String,
    @Schema(
        description = "SHA of the most recent commit on this branch.",
        example = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
    )
    val lastCommitSha: String,
)
