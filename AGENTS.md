# Vyavsa Mobile — Agent Operating Instructions

Vyavsa is a native Android business management application for small businesses and retail/distribution enterprises.

---

## 1. Context System & Execution Workflow

Every agent run must follow this deterministic 7-step sequence:

1. **Context Discovery**: Consult [.agents/context/INDEX.md](file:///home/varun/Desktop/vyavsa_mobile/.agents/context/INDEX.md) to identify the minimal context files to load for the active task.
2. **Task Continuity**: Check `git status -s`. If uncommitted changes exist or continuing an existing task, inspect [.agents/context/CURRENT_TASK.md](file:///home/varun/Desktop/vyavsa_mobile/.agents/context/CURRENT_TASK.md) before starting.
3. **Selective Loading**: Load **only** the 1–2 context files relevant to the active task domain (e.g. `ARCHITECTURE.md`, `MODULES.md`, `CONSTRAINTS.md`, `DECISIONS.md`). Never load all documentation files.
4. **Targeted Code Inspection**: Inspect specific source files and symbols before editing. Never dump entire folders.
5. **Smallest Safe Implementation**: Implement one vertical slice at a time following existing architecture and conventions.
6. **Verification Quality Gate**: Run relevant tests (e.g. `./gradlew test --tests <ClassName>`). Never claim unexecuted tests passed. Never proceed past a failing gate.
7. **Task Handoff**: Update [.agents/context/CURRENT_TASK.md](file:///home/varun/Desktop/vyavsa_mobile/.agents/context/CURRENT_TASK.md) with completed changes, verified test outputs, and the next concrete action.

---

## 2. Source of Truth & Scope

- **Authoritative Contracts**: Backend routes, DTOs, middleware, and tests define actual API behavior. Inspect `/home/varun/Desktop/vyavsa` when necessary.
- **Selective API Map Usage**: [docs/backend-api-map.md](file:///home/varun/Desktop/vyavsa_mobile/docs/backend-api-map.md) is 26 KB (>6,000 tokens). **Never load it in full.** Read only the specific section needed (e.g. Section 5 for sales) via line-range reads or grep.
- **Scope Discipline**: Keep Android application code inside the mobile repository. Modify backend files only when necessary and justified. Avoid unrelated refactoring and preserve uncommitted working tree changes.
- **Zero Secrets**: Never commit credentials, tokens, passwords, or production keys.

---

## 3. Technology Stack & Architecture

- **Stack**: Kotlin 1.9.24, Jetpack Compose (BOM 2024.04.01), Material 3, MVVM, Retrofit 2.11.0, OkHttp 4.12.0, Kotlinx Serialization, Coroutines & Flow, Hilt 2.51.1, DataStore Preferences, JUnit 4.
- **Architecture**: Compose UI -> ViewModel (owns screen state via StateFlow) -> Repository -> Retrofit API Service / DataStore.
- **Separation of Concerns**: Keep business logic outside composables. Share UI components via `core.designsystem`. Maintain unidirectional data flow.
- **Data Synchronization**: Propagate multi-screen updates reactively using [DataRefreshCoordinator.kt](file:///home/varun/Desktop/vyavsa_mobile/app/src/main/java/com/vyavsa/app/core/sync/DataRefreshCoordinator.kt).

---

## 4. Security & API Rules

- **Base URLs**: Centralize environment URLs via `BuildConfig.BASE_URL` (Dev: `http://10.0.2.2:8080/`, Prod: `https://vyavsa.varunjp.in/`).
- **Token Security**: Store tokens strictly in [TokenManager.kt](file:///home/varun/Desktop/vyavsa_mobile/app/src/main/java/com/vyavsa/app/core/security/TokenManager.kt) via `EncryptedSharedPreferences`.
- **Log Redaction**: Route all OkHttp logging through [RedactedLoggingInterceptor.kt](file:///home/varun/Desktop/vyavsa_mobile/app/src/main/java/com/vyavsa/app/core/network/RedactedLoggingInterceptor.kt). Mask `Authorization` and `Cookie` headers.
- **Refresh Concurrency**: [TokenAuthenticator.kt](file:///home/varun/Desktop/vyavsa_mobile/app/src/main/java/com/vyavsa/app/core/network/TokenAuthenticator.kt) synchronizes on a refresh lock and enforces `responseCount >= 2` loop limit.
- **RBAC Authority**: Server-side RBAC and tenant isolation are authoritative. Client checks govern UI visibility only. Clear session on logout or refresh failure.
- **Idempotency**: Never blindly retry non-idempotent operations (e.g. POST sales/purchases).

---

## 5. UI & Kotlin Standards

- **Material 3**: Follow modern Material 3 design, supporting loading, success, empty, and error states. Handle system insets and keyboard visibility.
- **No Fake Data**: Do not hardcode fake financial metrics in production composables.
- **Kotlin Conventions**: Structured concurrency, immutable data, lifecycle-safe Flow collection, explicit exception handling, no force unwraps (`!!`).

---

## 6. Testing & Quality Gate

- Test business logic, ViewModels, API serialization contracts, and thermal invoice formatting.
- Execute targeted unit tests using `./gradlew test --tests "<package.or.Class>"`.
- Use MockWebServer for isolated network integration tests.
- A task is complete only when the implementation is integrated, tests pass, and actual results are reported in [CURRENT_TASK.md](file:///home/varun/Desktop/vyavsa_mobile/.agents/context/CURRENT_TASK.md).
