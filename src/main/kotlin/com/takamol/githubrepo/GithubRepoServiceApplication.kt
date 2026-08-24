package com.takamol.githubrepo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GithubRepoServiceApplication

fun main(args: Array<String>) {
	runApplication<GithubRepoServiceApplication>(*args)
}
