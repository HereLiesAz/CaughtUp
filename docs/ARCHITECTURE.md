# Architecture Guide

CleanUnderwear follows modern Android development principles, utilizing a Clean Architecture approach combined with the Model-View-ViewModel (MVVM) presentation pattern. This ensures that the codebase remains scalable, testable, and maintainable.

## Core Principles

1.  **Separation of Concerns**: The application is divided into distinct layers, each with a specific responsibility. This prevents tight coupling and makes it easier to modify or replace individual components.
2.  **Unidirectional Data Flow (UDF)**: Data flows downwards from the data source to the UI, and events flow upwards from the UI to the ViewModels. This predictability makes state management more straightforward.
3.  **Dependency Injection**: We use Dagger Hilt to provide dependencies to our classes. This reduces boilerplate, improves modularity, and is crucial for writing unit tests.
4.  **Reactive Programming**: Kotlin Coroutines and Flows are heavily utilized to handle asynchronous operations and stream data reactively across the application layers.

## Layered Architecture

The project is structured into three main layers:

### 1. Presentation Layer (UI & ViewModels)
*   **Jetpack Compose**: The UI is entirely built using Jetpack Compose, Android's modern toolkit for building native UI in a declarative manner.
*   **ViewModels**: Act as the bridge between the UI and the domain/data layers. They hold the UI state and handle user intents. ViewModels do not know about Compose directly; they expose state via `StateFlow` which Compose observes.
*   **UDF in Action**: The UI emits events (e.g., `OnLoginClicked`) to the ViewModel. The ViewModel processes the event, interacts with the lower layers, updates its internal state, and exposes the new state via a Flow, which the UI recomposes based on.

### 2. Domain Layer (Use Cases)
*   This layer contains the core business logic of the application.
*   **Use Cases (Interactors)**: Small, focused classes that execute a specific piece of business logic. They orchestrate data from various repositories and format it for the presentation layer. Use Cases help keep ViewModels lean and improve reusability.

### 3. Data Layer (Repositories & Data Sources)
*   This layer is responsible for fetching, caching, and managing data.
*   **Repositories**: Act as the single source of truth for data. They abstract the origin of the data (network vs. local database) from the rest of the application.
*   **Local Data Source**: We use **Room** for local data persistence.
*   **Remote Data Source**: We use **Retrofit** and **OkHttp** for handling API requests.
*   **DataStore**: Used for storing simple key-value pairs or typed objects asynchronously, replacing SharedPreferences.

## Dependency Injection with Hilt

Dagger Hilt simplifies DI in Android.
*   **Modules**: We define Hilt modules (`@Module`) to tell Hilt how to construct instances of interfaces or classes from external libraries (like Retrofit or Room).
*   **Components**: We use standard Hilt components (like `SingletonComponent` for app-wide dependencies and `ViewModelComponent` for ViewModel-scoped dependencies).
*   **Injection**: We use `@Inject` to request dependencies in our classes (e.g., passing a Repository into a Use Case, or a Use Case into a ViewModel). ViewModels are annotated with `@HiltViewModel`.

## Asynchronous Operations & State Management

*   **Coroutines**: Used for all background work, such as network calls or database queries, avoiding callback hell and managing threads efficiently.
*   **Flows**: Used to represent streams of data. For example, a Room database query can return a `Flow<List<Item>>`, allowing the UI to automatically update whenever the underlying database table changes.
*   **StateFlow**: Used within ViewModels to hold the current UI state. It is a state-holder observable flow that emits the current and new state updates to its collectors.

## Code Quality Tools

*   **Detekt**: We use Detekt for static code analysis to enforce Kotlin coding conventions and catch potential bugs early. The configuration is located at `config/detekt/detekt.yml`.
