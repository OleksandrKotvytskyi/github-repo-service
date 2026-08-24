# GitHub Repository Service

A REST API, built with **Kotlin + Spring WebFlux**, that lists the non-fork repositories owned
by a given GitHub user, together with their branches and the latest commit SHA on each branch.

## Endpoint

```
GET /api/v1/users/{username}/repositories
Accept: application/json
```

**200 OK**

```json
[
  {
    "name": "github-repo-service",
    "owner": "octocat",
    "branches": [
      { "name": "main", "lastCommitSha": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2" },
      { "name": "develop", "lastCommitSha": "9f8e7d6c5b4a9f8e7d6c5b4a9f8e7d6c5b4a9f8e" }
    ]
  }
]
```

Forked repositories are excluded — only repositories owned/created by the user are returned.
An unknown-but-syntactically-valid username with zero repositories returns `200 OK` with `[]`.

Interactive API docs (Swagger UI): `http://localhost:8080/swagger-ui.html`
Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`

## Running it

Requires JDK 21.

```bash
./gradlew bootRun
```

Optionally set a GitHub personal access token to raise the rate limit from 60 to 5000
requests/hour (no scopes needed for public data):

```bash
GITHUB_TOKEN=ghp_xxx ./gradlew bootRun
```

Build a jar and run it directly:

```bash
./gradlew bootJar
java -jar build/libs/github-repo-service-0.0.1-SNAPSHOT.jar
```

Run the test suite:

```bash
./gradlew test
```

## Architecture

The code is layered to keep GitHub's data model fully decoupled from this service's own
public contract:

```
web/            Controller, our API's DTOs (RepositoryResponse, BranchResponse, ErrorResponse),
                and the global exception handler.
service/        RepositoryService — the only layer that "translates" between GitHub's
                shape and ours (fork filtering, DTO mapping, concurrency policy).
client/         GithubClient — a thin wrapper over WebClient that talks to the GitHub REST
                API and exposes GitHub's DTOs (GithubRepositoryDto, GithubBranchDto), with
                pagination and error normalisation. Knows nothing about our public contract.
exception/      Custom exceptions (GithubUserNotFoundException, GithubUpstreamException)
                used to carry failure semantics up from client → service → web layer.
config/         WebClient bean, GitHub connection properties, OpenAPI metadata.
```

This mirrors a fairly standard "controller → service → external client" layering. The key
design goal was that **changing our JSON contract never requires touching `GithubClient`**,
and **changing what GitHub returns never requires touching the controller** — `RepositoryService`
is the single seam between the two.

### Fetching branches + latest commit efficiently

GitHub's `GET /repos/{owner}/{repo}/branches` endpoint already returns, for every branch,
the SHA of the commit it currently points at (`commit.sha`). That SHA *is* the latest commit
on that branch, so no additional per-commit call is needed — one request per repository is
enough to get all of its branches and their latest commits.

Fetching branches for different repositories is done concurrently
(`Flux.flatMap(mapper, concurrency)`), bounded by `github.api.branch-fetch-concurrency`
(default 8), to keep latency down without hammering GitHub's rate limits.

### Pagination

GitHub paginates both the repository list and the branch list. `GithubClient` follows the
`Link: rel="next"` header recursively until exhausted, so users/repos with more than 100
items are handled correctly rather than silently truncated.

## Error handling

All errors — regardless of where they originate — are normalised by a single
`@RestControllerAdvice` (`GlobalExceptionHandler`) into one consistent JSON shape:

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "GitHub user 'ghost' was not found",
  "path": "/api/v1/users/ghost/repositories"
}
```

| Scenario                                             | Status | Rationale |
|-------------------------------------------------------|--------|-----------|
| GitHub user does not exist                             | `404 Not Found` | The request was well-formed; the resource being asked about doesn't exist. |
| Client requests a media type other than JSON           | `406 Not Acceptable` | Standard HTTP semantics for content negotiation failure; the controller only `produces` JSON so Spring raises this automatically. |
| GitHub rate limit hit (403/429 from GitHub)             | `429 Too Many Requests` | Distinguishes "try again later" from a hard upstream failure, and matches GitHub's own signal. |
| GitHub API returns any other error (5xx, unexpected 4xx, timeouts) | `502 Bad Gateway` | This service acts as a gateway to a third-party API it doesn't control; a `502` correctly signals the fault is upstream, not in this service or the client's request. |
| Any other unexpected exception                         | `500 Internal Server Error` | Safety net — logged server-side with full detail, but the client only sees a generic message so no internals leak. |

## Assumptions

- "Repository name" identifies within the response, so `name` + `owner` (owner included since
  GitHub organizations/usernames can differ from repo owner in edge cases like renamed users)
  is returned per repository.
- "Not a fork" is taken directly from GitHub's `fork` boolean on the repository resource.
- Only repositories *owned by* the user are considered (`type=owner` query param), i.e.
  repositories the user merely collaborates on are excluded, matching "repositories belonging
  to a GitHub user".
- Branch protection status, commit message/author, and other repo metadata (stars, language,
  etc.) are out of scope — the brief only asks for repo name, branches, and latest commit id
  per branch.
- No caching layer: each request re-fetches fresh data from GitHub. For a take-home-sized
  service this favours correctness/simplicity over throughput; a production version would
  likely add a short-TTL cache (e.g. Caffeine) keyed by username to reduce load on GitHub's
  rate limit.
- Unauthenticated GitHub API access is enough for the assessment (60 req/hour); an optional
  `GITHUB_TOKEN` is supported for higher limits but not required.

## Limitations

- No pagination is exposed on *our* API — for users with very large numbers of repositories,
  the endpoint fetches everything from GitHub before responding. Given `Flux` is used
  throughout, this could be adapted to a streaming/paged public API without changing the
  service or client layers.
- No authentication/authorization on the service's own endpoint — out of scope per the brief.
- No retry/circuit-breaker on transient GitHub failures beyond what `WebClient`/GitHub's own
  reliability provides; a single upstream failure surfaces immediately as a `502`/`429` rather
  than being retried transparently.

## Testing strategy

- **Unit tests** (`RepositoryServiceTest`): the service is tested in isolation with
  `GithubClient` mocked via **Mockito-Kotlin**, verifying fork filtering and correct
  DTO mapping without any network involvement.
- **Client tests** (`GithubClientTest`): `GithubClient` is tested against a real embedded
  **OkHttp `MockWebServer`**, covering pagination (`Link` header following), successful
  responses, 404 → `GithubUserNotFoundException` mapping, and other error codes →
  `GithubUpstreamException` mapping. This gives confidence in the actual HTTP/JSON wiring
  without depending on the real GitHub API.
- **Web slice tests** (`RepositoryControllerTest`): `@WebFluxTest` + `WebTestClient` with a
  mocked `RepositoryService`, verifying the controller/`GlobalExceptionHandler` produce the
  correct status codes and consistent error bodies for every documented error class,
  including content negotiation (`406`) which is enforced by Spring itself.
- **Integration test** (`GithubRepoServiceApplicationIT`): boots the full Spring context and
  points `github.api.base-url` at an embedded `MockWebServer` via `@DynamicPropertySource`,
  exercising the real request path end-to-end (controller → service → client → HTTP) for both
  the happy path and the user-not-found path.

Run everything with `./gradlew test`.

## Tech stack

- Kotlin 1.9, Java 21
- Spring Boot 3.3 (WebFlux, Validation)
- springdoc-openapi for OpenAPI/Swagger UI generation
- JUnit 5, Reactor `StepVerifier`, Mockito + mockito-kotlin, OkHttp MockWebServer
