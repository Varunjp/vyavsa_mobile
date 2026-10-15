
## Project

Vyavsa native Android application for small businesses.

## Source of Truth

- Inspect existing code before making changes.
- Backend routes, DTOs, middleware, and tests define actual API behavior.
- Read `docs/backend-api-map.md` before implementing API integrations.
- Never invent endpoints, payload fields, roles, or response formats.
- Preserve existing implementation and repository conventions.

## Scope

- Keep Android application code inside the designated mobile folder.
- Modify backend files only when necessary and justified.
- Avoid unrelated refactoring and unnecessary new files.
- Never commit secrets, credentials, tokens, or production data.

## Stack

- Kotlin, Jetpack Compose, Material 3.
- MVVM, feature-oriented architecture.
- Retrofit, OkHttp, Kotlin Serialization.
- Coroutines and Flow.
- Hilt for dependency injection.
- DataStore for preferences.
- Room only when justified.
- JUnit and Android testing libraries.

Use the existing compatible toolchain and dependency versions. Do not upgrade dependencies without a reason.

## Architecture

- Compose screens render state and emit user actions.
- ViewModels own screen state and presentation logic.
- Repositories coordinate data access.
- API services define typed HTTP contracts.
- Keep business logic outside composables.
- Use domain/use-case abstractions only when they add value.
- Share reusable UI components through the design system.
- Keep dependency direction consistent and avoid circular dependencies.

## UI Rules

- Minimal, modern, accessible Material 3 design.
- Consistent colors, typography, spacing, shapes, and icons.
- Support loading, success, empty, and error states.
- Handle keyboard visibility, system insets, and screen sizes.
- Use descriptive accessibility labels and appropriate touch targets.
- Do not use fake financial data in production screens.
- Hide unsupported features instead of displaying nonfunctional navigation.

## API and Security

- Use the verified backend contract.
- Centralize environment-specific base URLs.
- Use HTTPS in production.
- Never hardcode credentials or tokens.
- Redact authorization headers and sensitive request/response data from logs.
- Store sensitive tokens using an appropriate secure mechanism.
- Handle refresh-token concurrency and failures safely.
- Do not blindly retry non-idempotent operations.
- Respect backend authentication, RBAC, and tenant isolation.
- Clear user-specific cached state on logout and account switching.
- Never treat client-side checks as authorization.
- Avoid logging OTPs, passwords, tokens, or financial details.

## Kotlin Rules

- Follow Kotlin and Android conventions.
- Prefer immutable data and small, focused functions.
- Use structured concurrency.
- Avoid GlobalScope, blocking calls on the main thread, and unnecessary mutable shared state.
- Collect UI state lifecycle-safely.
- Handle exceptions explicitly.
- Avoid force unwraps and unexplained magic values.
- Keep functions and files focused.
- Add comments only when intent is not obvious.

## Workflow

1. Inspect relevant files before editing.
2. Identify the smallest safe implementation.
3. Follow existing naming and architectural conventions.
4. Implement one feature or vertical slice at a time.
5. Integrate real API behavior.
6. Add or update focused tests.
7. Run the relevant build, tests, and static checks.
8. Review the diff for regressions and secrets.
9. Report verified results and remaining limitations.

Do not proceed past a failing quality gate without fixing the failure or clearly identifying a genuine external blocker.

## Testing

- Test business logic and ViewModels.
- Test API serialization and error handling.
- Test authentication and session expiration.
- Test tenant/account switching and logout.
- Test important UI states and user flows.
- Use MockWebServer for isolated HTTP tests when appropriate.
- Do not claim unexecuted tests passed.

## Token Efficiency

- Read this file once and follow it throughout the task.
- Inspect targeted files before exploring unrelated directories.
- Prefer targeted searches over dumping entire files or repositories.
- Reuse existing models, components, and dependencies.
- Avoid rereading unchanged files and repeating completed analysis.
- Keep plans and progress reports concise.
- Do not regenerate working code without a concrete reason.
- Update documentation only when contracts or architecture change.
- Record verified API findings once in `docs/backend-api-map.md`.
- Delegate only independent tasks that benefit from parallel work.
- Do not spawn agents for simple edits or duplicate investigations.

## Completion

A task is complete only when the implementation is integrated, relevant checks have been executed, and actual results are reported.

List outstanding backend dependencies, failed checks, and manual verification steps explicitly.