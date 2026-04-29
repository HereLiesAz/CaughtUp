# CleanUnderwear

CleanUnderwear is a modern Android application built using Kotlin and Jetpack Compose. It aims to provide an intuitive and seamless user experience with a robust underlying architecture.

## Tech Stack

The application leverages the latest Android development tools and libraries:

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for a declarative and reactive UI.
*   **Architecture**: Built with MVVM (Model-View-ViewModel) and Clean Architecture principles in mind.
*   **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/)
*   **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html)
*   **Networking**: [Retrofit](https://square.github.io/retrofit/) and [OkHttp](https://square.github.io/okhttp/)
*   **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
*   **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)

## Project Structure

*   `app/`: The main application module containing the UI, ViewModels, and App-level configurations.
*   `gradle/`: Contains the Gradle Wrapper and the `libs.versions.toml` file for centralized dependency management (Version Catalogs).
*   `docs/`: Contains detailed documentation regarding the architecture and design patterns used in the project.

## Setup and Build Instructions

### Prerequisites
*   Android Studio (latest stable or Ladybug recommended)
*   JDK 17

### Building the Project

1.  Clone the repository:
    ```bash
    git clone https://github.com/HereLiesAz/CleanUnderwear.git
    cd CleanUnderwear
    ```
2.  Open the project in Android Studio. Let Gradle sync and resolve dependencies.
3.  To build the debug APK from the command line, run:
    ```bash
    ./gradlew assembleDebug
    ```
4.  To run the tests:
    ```bash
    ./gradlew test
    ```

### Automatic Versioning

The project uses an automatic versioning system integrated into `app/build.gradle.kts`. The versioning is driven by the `version.properties` file located at the root of the project.

*   `versionMajor`, `versionMinor`, and `versionPatch` define the base version name (e.g., `1.0.0`).
*   `versionBuild` tracks the build number and is used as the `versionCode`.
*   During a release build (e.g., `./gradlew assembleRelease`), the `versionCode` is automatically incremented in the `version.properties` file.

### CI/CD workflows

The project uses GitHub Actions for continuous integration and delivery.

*   `.github/workflows/merged-build.yml`: This workflow is triggered on pull requests and pushes to the `main` or `master` branch. It automatically builds the application, verifies that it compiles correctly, and on pushes, it creates a new GitHub Release with the compiled APK attached.

## Documentation

For more in-depth information, please refer to the following documents:

*   [Architecture Guide](docs/ARCHITECTURE.md)
*   [Design Guide](docs/DESIGN.md)
