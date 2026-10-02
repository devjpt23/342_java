# CS 342 Weather Information Service

A robust, modular, and testable command-line weather information system built in Java 25 and Maven, refactored according to object-oriented software design principles, constructor-based dependency injection, and separation of concerns.

---

## Team Members
- Individual Submission (or insert partner name here if working in pairs)

---

## Running the Application

### Prerequisites
- **Java 25** (or compatible modern JDK)
- **Maven 3.9+**

### Compile
```bash
mvn compile
```

### Run Interactive CLI
Using the Maven Exec Plugin:
```bash
mvn exec:java
```

Or run directly via `java`:
```bash
java -cp target/classes weather.Main
```

### Interactive Commands
When the prompt `weather>` appears, the following commands are available:
- `help` — Display available commands and their syntax.
- `locations` — List all configured locations known to the system.
- `current <location>` — Display current temperature, feels like, humidity, wind speed, conditions, and ASCII bar for a city (e.g., `current Chicago` or `current "New York"`).
- `compare <loc1> <loc2>` — Side-by-side comparison of current weather between two cities (e.g., `compare Chicago "New York"`).
- `summary <location>` — Produce a readable, natural-language sentence summarizing conditions (e.g., `summary "Los Angeles"`).
- `warmest` — Identify the warmest location among all configured cities.
- `coldest` — Identify the coldest location among all configured cities.
- `all` — Display a quick weather overview for all configured cities.
- `quit` — Exit the application cleanly.

---

## Running the Tests

Execute the complete automated test suite:
```bash
mvn test
```

All 28 unit tests run in isolation with 0 network calls and execute in ~1 second.

---

## Design

The application adheres strictly to the single responsibility principle and separation of concerns across distinct packages:

```
src/
├── main/
│   └── java/
│       └── weather/
│           ├── Main.java                          [Composition Root]
│           ├── model/
│           │   ├── Location.java                  [City & coordinates record]
│           │   ├── WeatherData.java               [Weather measurements record]
│           │   └── WeatherComparison.java         [Comparison domain model]
│           ├── provider/
│           │   ├── WeatherDataProvider.java       [Provider abstraction]
│           │   ├── OpenMeteoWeatherProvider.java  [Open-Meteo HTTP implementation]
│           │   └── WeatherProviderException.java  [Domain exception]
│           ├── service/
│           │   └── WeatherService.java            [Application & business logic]
│           └── cli/
│               └── WeatherCLI.java                [Input reading & presentation]
└── test/
    └── java/
        └── weather/
            ├── TestWeatherDataProvider.java       [Controlled test stub]
            ├── WeatherServiceTest.java            [12 Service unit tests]
            ├── WeatherCLITest.java                [11 CLI unit tests]
            └── OpenMeteoWeatherProviderTest.java  [5 Parser unit tests]
```

### Component Responsibilities:
1. **`weather.Main`**: The composition root. It wires together the dependencies (`OpenMeteoWeatherProvider` -> `WeatherService` -> `WeatherCLI`) and launches the CLI. It contains no application or presentation logic.
2. **`weather.model`**: Immutable records (`Location`, `WeatherData`, `WeatherComparison`) holding typed data. Raw JSON strings never escape the provider boundary.
3. **`weather.provider`**: Handles communication with the remote weather service. `WeatherDataProvider` defines the contract, while `OpenMeteoWeatherProvider` builds HTTP requests, checks HTTP status codes, and parses responses into `WeatherData`. Any network failure, non-200 status, or malformed data triggers an explicit `WeatherProviderException`.
4. **`weather.service`**: Encapsulates core business rules (finding locations, computing summaries, comparing conditions, finding extremes). It never communicates directly with `HttpClient` and never prints directly to `System.out`.
5. **`weather.cli`**: Reads interactive user commands, tokenizes inputs (supporting quoted strings), validates arguments, calls `WeatherService`, handles exceptions gracefully, and formats output with ASCII visuals.

---

## Interfaces

The **`WeatherDataProvider`** interface sits between the high-level business logic (`WeatherService`) and the low-level data source (`OpenMeteoWeatherProvider`).

### Why an interface?
- **Decoupling**: `WeatherService` depends only on the contract `WeatherData getCurrentWeather(Location location)`, not on any concrete networking or parsing classes.
- **Interchangeability**: Replacing Open-Meteo with NOAA, WeatherAPI, or a local file cache requires only implementing `WeatherDataProvider` without modifying a single line of `WeatherService` or `WeatherCLI`.
- **Testability**: In unit testing, tests provide a controlled stub (`TestWeatherDataProvider`) that simulates successes, network outages, and edge cases with zero external dependencies.

---

## Dependency Injection

### Injected Dependencies:
1. **`WeatherDataProvider` into `WeatherService`**:
   `WeatherService` receives an implementation of `WeatherDataProvider` through its constructor:
   ```java
   public WeatherService(WeatherDataProvider provider)
   public WeatherService(WeatherDataProvider provider, List<Location> knownLocations)
   ```
2. **`WeatherService` into `WeatherCLI`**:
   `WeatherCLI` receives `WeatherService`, `Scanner`, and `PrintStream` through its constructor:
   ```java
   public WeatherCLI(WeatherService service, InputStream in, PrintStream out)
   ```

### Benefits:
- Eliminates hardcoded `new OpenMeteoWeatherProvider()` instantiations inside business logic.
- Enables deterministic, fast unit tests using stubs/mocks.
- Allows configuring custom location sets or streams for testing different operational scenarios.

---

## Testing

Automated testing is built using **JUnit Jupiter (JUnit 5)**:
- **No Live API Calls**: Unit tests never make HTTP requests or require an active Internet connection.
- **Controlled Test Implementation (`TestWeatherDataProvider`)**: A test stub implementing `WeatherDataProvider` delivers deterministic `WeatherData` for tested cities and allows simulating provider failures (`setShouldFail(true)`).
- **Comprehensive Coverage**:
  - `WeatherServiceTest.java` (12 tests): Covers retrieving weather, known location resolution, case-insensitive matching, unknown location error handling, comparison math, summary generation, warmest/coldest stream reduction, and provider failure propagation.
  - `WeatherCLITest.java` (11 tests): Covers all commands (`help`, `locations`, `current`, `compare`, `summary`, `warmest`, `coldest`, `all`, `quit`), quoted multi-word arguments, invalid commands, missing arguments, and graceful provider failure reporting.
  - `OpenMeteoWeatherProviderTest.java` (5 tests): Validates JSON parsing, missing field detection (rejecting silent defaults), and WMO weather code translation.

---

## Java 25 Features

This project leverages modern Java features to improve design clarity and conciseness:
1. **Records**: `Location`, `WeatherData`, and `WeatherComparison` use Java records for compact, immutable data carriers with built-in validation in compact constructors.
2. **Enhanced Switch Expressions**:
   - In `WeatherCLI`, command dispatching uses pattern-like switch branches (`case "help" -> ...`).
   - In `OpenMeteoWeatherProvider`, WMO weather code decoding uses multi-label switch arms (`case 61, 63, 65 -> "Rain"`).
3. **Text Blocks (`"""..."""`)**: Used in `WeatherCLI` for the multi-line `help` menu and in `OpenMeteoWeatherProviderTest` for sample JSON payloads.
4. **Local Variable Type Inference (`var`)**: Enhances readability in service and CLI methods where variable types are evident from assignments.
5. **Modern Collection APIs and Streams**: Used in `WeatherService` to filter locations, map cities to weather records, find extremes (`max`/`min` with `Comparator.comparingDouble`), and collect to unmodifiable lists (`toList()`).

---

## Additional Feature

The application implements three additional weather capabilities utilizing the service layer and stream operations:
1. **`warmest` Command**: Uses `service.getWarmestLocation()`, processing all known locations via streams to find the city with the maximum current temperature.
2. **`coldest` Command**: Uses `service.getColdestLocation()`, processing all known locations via streams to find the city with the minimum current temperature.
3. **`all` Command**: Displays an overview table comparing conditions and ASCII temperature bars across all configured cities simultaneously.

All additional features go through `WeatherService` and `WeatherDataProvider`, upholding architectural constraints.

---

## Design Reflection Questions

### 1. What responsibilities were combined in the original starter code that you separated during refactoring?
In the starter `Main.java`, a single class was responsible for:
- Storing static location coordinates.
- Initializing `HttpClient` and managing HTTP transport details.
- Formulating URL query parameters.
- Sending network requests and inspecting HTTP response status.
- Parsing JSON via regular expressions.
- Application logic (error fallbacks, data manipulation).
- Presentation and formatting (printing text and rendering ASCII bars to `System.out`).

During refactoring, these were separated into distinct single-responsibility components:
- **Data models** (`Location`, `WeatherData`, `WeatherComparison`) hold state.
- **Provider** (`WeatherDataProvider`, `OpenMeteoWeatherProvider`) handles HTTP communication and JSON parsing.
- **Service** (`WeatherService`) encapsulates business rules and queries.
- **Presentation** (`WeatherCLI`) handles input parsing, user feedback, and rendering.
- **Composition root** (`Main`) coordinates object construction and startup.

### 2. How does programming to `WeatherDataProvider` make the program easier to change?
Programming to `WeatherDataProvider` decouples consumers (`WeatherService` and `WeatherCLI`) from provider details. If the Open-Meteo API changes its endpoint format, or if the organization migrates to the National Weather Service (NWS) API or an internal cached database, only a new implementation of `WeatherDataProvider` needs to be created. No changes are required in `WeatherService`, `WeatherCLI`, or any unit tests testing business logic.

### 3. How does dependency injection make `WeatherService` easier to test?
Because `WeatherService` receives `WeatherDataProvider` through its constructor rather than instantiating `OpenMeteoWeatherProvider` with `new`, unit tests can pass a test double (`TestWeatherDataProvider`). This allows tests to:
- Control returned temperatures and conditions deterministically.
- Simulate error conditions (HTTP 500, socket timeouts, corrupted JSON) on demand.
- Run instantaneously without requiring an Internet connection or live API credentials.

### 4. Why should unit tests avoid depending on the live weather API?
- **Flakiness**: Real network connections can drop, experience latency, or encounter intermittent DNS/connectivity problems.
- **Non-deterministic data**: Live weather changes continuously; asserting that Chicago is 65°F will fail when the weather changes tomorrow.
- **Rate limiting**: Continuous automated builds and CI/CD pipelines making frequent calls could get rate-limited or banned.
- **Execution speed**: HTTP round-trips take hundreds of milliseconds per call, whereas in-memory stubs execute in sub-milliseconds.

### 5. If the current weather API were replaced with a completely different provider, which parts of your application would need to change? Which parts should not need to change?
- **Parts that would need to change**:
  - A new provider class (e.g., `NwsWeatherProvider` or `AccuWeatherProvider`) implementing `WeatherDataProvider` would be written to handle the new provider's URL structure, authentication, and JSON schema.
  - `Main.java` would be updated to instantiate the new provider during dependency injection.
- **Parts that should NOT need to change**:
  - `WeatherDataProvider` interface.
  - `WeatherService` application logic (comparison, summaries, warmest/coldest calculations).
  - `WeatherCLI` presentation, command parsing, and user interactions.
  - `Location`, `WeatherData`, and `WeatherComparison` models.
  - `WeatherServiceTest` and `WeatherCLITest` test suites.
