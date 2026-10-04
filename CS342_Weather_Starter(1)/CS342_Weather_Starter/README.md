# CS 342 Weather Information Service

## Project Overview

The Weather Information Service is an interactive Java 25 command-line application. It retrieves current weather information from the Open-Meteo Forecast API and displays results for Chicago, Los Angeles, and New York.

The project refactors the original single-file prototype into separate model, provider, service, and command-line layers. This separation keeps HTTP communication, application logic, and presentation code independent from one another.

## Team Members

- Dev Jayesh Patel
- Abrar Ahmed

Replace the placeholders above with the team members' names before submission.
****
## Requirements

- Java 25
- Maven
- Internet access when retrieving live Open-Meteo weather

## Running the Application

From the directory containing `pom.xml`, compile the project:

```bash
mvn compile
```

Start the application:

```bash
java -cp target/classes weather.Main
```

The program displays the `weather>` prompt and waits for a command. Multi-word locations must be placed inside quotation marks.

Examples:

```text
weather> current Chicago
weather> current "Los Angeles"
weather> compare Chicago "New York"
weather> summary "New York"
weather> windiest
weather> quit
```

## Available Commands

| Command | Description |
|---|---|
| `help` | Displays the available commands and their syntax. |
| `locations` | Displays all configured locations. |
| `current <location>` | Displays the current temperature, feels-like temperature, and wind speed. |
| `compare <location1> <location2>` | Compares all supported measurements for two locations. |
| `summary <location>` | Displays a readable summary of current conditions. |
| `windiest` | Displays the configured location with the highest current wind speed. |
| `quit` | Exits the application cleanly. |

## Running the Tests

Run the complete test suite with:

```bash
mvn clean test
```

The project contains:

- 8 `WeatherService` tests
- 5 `WeatherCLI` tests
- 13 total JUnit Jupiter tests

The tests use `FakeWeatherDataProvider`, which supplies controlled data for the three configured cities and can simulate a provider failure. Unit tests do not contact Open-Meteo or require an Internet connection.

## Design

The application is divided into components with separate responsibilities:

| Component | Responsibility |
|---|---|
| `Main` | Creates the production objects, connects their dependencies, and starts the CLI. |
| `WeatherCLI` | Reads commands, validates arguments, calls service methods, and displays results. |
| `WeatherService` | Stores configured locations and performs retrieval, comparison, summary, and windiest-location operations. |
| `WeatherDataProvider` | Defines the provider operation used by the service. |
| `OpenMeteoWeatherProvider` | Sends synchronous HTTP requests to Open-Meteo and converts JSON responses into `WeatherData`. |
| `Location` | Stores a city name, latitude, and longitude. |
| `WeatherData` | Stores a city and its current supported weather measurements. |
| `WeatherComparison` | Stores two weather results and calculates differences between their measurements. |

The main runtime flow is:

```text
User
  -> WeatherCLI
  -> WeatherService
  -> WeatherDataProvider
  -> OpenMeteoWeatherProvider
  -> Open-Meteo API
```

The response returns through the same layers as a `WeatherData` object. Raw JSON does not pass into the service or CLI.

## Interfaces

`WeatherDataProvider` represents any source of current weather information:

```java
WeatherData getCurrentWeather(Location location);
```

`WeatherService` depends on this interface instead of directly depending on `OpenMeteoWeatherProvider`. A different production provider can therefore implement the same interface without requiring changes to the service or CLI.

The test suite uses the same interface to replace the live provider with `FakeWeatherDataProvider`.

## Dependency Injection

`Main` acts as the composition root:

```java
WeatherDataProvider provider = new OpenMeteoWeatherProvider();
WeatherService service = new WeatherService(provider);
WeatherCLI cli = new WeatherCLI(service);

cli.start();
```

`WeatherService` receives its provider through its constructor instead of creating an `OpenMeteoWeatherProvider` internally. This constructor-based dependency injection allows production code to use the real provider while unit tests supply a controlled fake provider.

## Weather Measurements and Units

The application requests and displays:

| Measurement | Open-Meteo field | Unit |
|---|---|---|
| Temperature | `temperature_2m` | Degrees Fahrenheit |
| Feels-like temperature | `apparent_temperature` | Degrees Fahrenheit |
| Wind speed | `wind_speed_10m` | Miles per hour |

The provider requests Fahrenheit with `temperature_unit=fahrenheit` and miles per hour with `wind_speed_unit=mph`.

## Error Handling

The application makes failures visible instead of substituting believable fake weather values. It reports:

- Unknown commands
- Missing or excessive command arguments
- Unknown locations
- Network and interrupted-request failures
- Non-successful HTTP status codes
- Empty or malformed API responses
- Missing measurements
- Invalid numeric measurements
- Negative wind speeds

The provider throws standard Java exceptions for provider failures. The CLI catches runtime failures and displays a useful message without silently replacing the data.

## Testing Without the Live API

`FakeWeatherDataProvider` is located under `src/test`. It implements `WeatherDataProvider` but never sends an HTTP request. Instead, it returns deterministic weather values for Chicago, Los Angeles, and New York.

The fake also accepts a failure setting:

```java
new FakeWeatherDataProvider(true)
```

This lets the tests verify provider-failure behavior without depending on Open-Meteo, network availability, changing weather values, or API rate limits.

The project uses JUnit Jupiter. It does not use Mockito.

## Java 25 Features

The implementation uses modern Java features where they improve readability:

- Records for the immutable `Location`, `WeatherData`, and `WeatherComparison` models
- An enhanced switch expression for dispatching CLI commands
- A text block for the multi-line help screen
- Streams for case-insensitive location lookup and selecting the windiest city
- A method reference, `provider::getCurrentWeather`, when retrieving weather for all locations
- `List.of` and `Map.of` for fixed collections
- `String.repeat` for the CLI temperature bar

The application uses synchronous HTTP and does not use threads, `ExecutorService`, asynchronous requests, or `CompletableFuture`.

## Additional Feature

The additional feature is the `windiest` command:

```text
weather> windiest
```

`WeatherService` retrieves current weather for every configured location through `WeatherDataProvider`. It compares each `WeatherData.windSpeed()` value and returns the record with the highest wind speed. The CLI then displays the city and its wind speed.

## Design Reflection

### 1. What responsibilities were combined in the original starter code that were separated during refactoring?

The original `Main` file had the whole project structure in one place, from the default location to minor error handling, to making api call to `OpenMeteo`. with that design it was impossible to use another provider without changing the entire code to suit the new code. the refactor sperates the `WeatherDataProvider` so that we can simply connect any provider, instead of just relying one `OpenMeteoWeatherProvider`.Furthermore, the older `main` did not have `cli` interaction, therefore in the refactor we have a seperate `cli` module.

### 2. How does programming to `WeatherDataProvider` make the program easier to change?

For example, if open-meteo starts charging money, our code implementation wouldn't have to change the entire codebase. it would just add new provider class and connect that new provider to this existing codebase.

### 3. How does dependency injection make `WeatherService` easier to test?

`WeatherService` receives a provider through its constructor. Production code injects `OpenMeteoWeatherProvider`, while tests inject `FakeWeatherDataProvider`. this means we can add dummy values in the fake provider and mimic the original api calls, this prevents making calls to the actual service.

For example, lets say that `open-meteo` charges `1 dollar` for every request we send. it makes sense to spend the dollar if a user makes the request, during testing it doesnt make sense to spend money and making actual calls. 

### 4. Why should unit tests avoid depending on the live weather API?

Testing is used to check if the codebase has the correct logic. So if there is a buggy logic, there is a high chance that the testcase will alert the user about it. this means that the testcases are used to test the determinstic behavior of the code. when testing with live data, the data will change very often so it becomes hard to reproduce the results for every test run.

### 5. If the current weather API were replaced, which parts would need to change and which parts should remain unchanged?

if weather api changes we need to change:
1. add a new provider class like `OpenMeteoWeatherProvider` which implements `WeatherDataProvider`.
2. the `main` file should now refer to the new provider instead open-meteo.

what needs to remain:
1. `WeatherDataProvider` interface, `WeatherService`, `WeatherCLI` model records, tests sohuld all remain the same. becuase these dont depend on open-meteo. (it doesnt even know what open-meto is) all they know is `WeatherDataProvider`
