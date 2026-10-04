package weather.cli;

import weather.model.Location;
import weather.model.WeatherComparison;
import weather.model.WeatherData;
import weather.service.WeatherService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class WeatherCLI {

    private final WeatherService service;
    private final Scanner scanner;
    private final PrintStream out;
    private boolean running;

    public WeatherCLI(WeatherService service) {
        this(service, System.in, System.out);
    }

    public WeatherCLI(WeatherService service, InputStream input, PrintStream output) {
        this.service = Objects.requireNonNull(service, "WeatherService cannot be null");
        this.scanner = new Scanner(Objects.requireNonNull(input, "Input cannot be null"));
        this.out = Objects.requireNonNull(output, "Output cannot be null");
    }

    public void start() {
        running = true;
        out.println("Weather Information Service");
        out.println("Type 'help' to see available commands.");

        while (running) {
            out.print("weather> ");
            out.flush();

            if (!scanner.hasNextLine()) {
                break;
            }

            executeCommand(scanner.nextLine());
        }
    }

    public void executeCommand(String commandLine) {
        try {
            List<String> tokens = tokenize(commandLine);

            if (tokens.isEmpty()) {
                return;
            }

            String command = tokens.get(0).toLowerCase();
            List<String> arguments = tokens.subList(1, tokens.size());

            switch (command) {
                case "help" -> handleHelp(arguments);
                case "locations" -> handleLocations(arguments);
                case "current" -> handleCurrent(arguments);
                case "compare" -> handleCompare(arguments);
                case "summary" -> handleSummary(arguments);
                case "windiest" -> handleWindiest(arguments);
                case "quit" -> handleQuit(arguments);
                default -> out.println(
                        "Unknown command: " + command + ". Type 'help' for available commands."
                );
            }
        } catch (RuntimeException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void handleHelp(List<String> arguments) {
        requireArgumentCount("help", arguments, 0);

        out.println("""
                Available commands:
                  help                              Display this help message
                  locations                         Display configured locations
                  current <location>                Display current weather
                  compare <location1> <location2>   Compare two locations
                  summary <location>                Display a weather summary
                  windiest                          Display the windiest location
                  quit                              Exit the application

                Use quotation marks around multi-word locations, such as "New York".
                """);
    }

    private void handleLocations(List<String> arguments) {
        requireArgumentCount("locations", arguments, 0);

        out.println("Configured locations:");
        for (Location location : service.getLocations()) {
            out.println("- " + location.city());
        }
    }

    private void handleCurrent(List<String> arguments) {
        requireArgumentCount("current <location>", arguments, 1);

        WeatherData data = service.getCurrentWeather(arguments.get(0));

        out.println("Current weather for " + data.city() + ":");
        out.printf("  Temperature: %.1f°F [%s]%n", data.temperature(), renderBar(data.temperature()));
        out.printf("  Feels like:  %.1f°F%n", data.feelsLikeTemperature());
        out.printf("  Wind speed:  %.1f mph%n", data.windSpeed());
    }

    private void handleCompare(List<String> arguments) {
        requireArgumentCount("compare <location1> <location2>", arguments, 2);

        WeatherComparison comparison = service.compare(arguments.get(0), arguments.get(1));
        WeatherData first = comparison.first();
        WeatherData second = comparison.second();

        out.printf("%-18s %-16s %-16s%n", "Measurement", first.city(), second.city());
        out.printf("%-18s %10.1f°F %10.1f°F%n",
                "Temperature", first.temperature(), second.temperature());
        out.printf("%-18s %10.1f°F %10.1f°F%n",
                "Feels like", first.feelsLikeTemperature(), second.feelsLikeTemperature());
        out.printf("%-18s %9.1f mph %9.1f mph%n",
                "Wind speed", first.windSpeed(), second.windSpeed());

        out.printf("Differences (%s minus %s): %.1f°F temperature, "
                        + "%.1f°F feels-like temperature, %.1f mph wind speed.%n",
                first.city(),
                second.city(),
                comparison.temperatureDifference(),
                comparison.feelsLikeTemperatureDifference(),
                comparison.windSpeedDifference());
    }

    private void handleSummary(List<String> arguments) {
        requireArgumentCount("summary <location>", arguments, 1);
        out.println(service.getSummary(arguments.get(0)));
    }

    private void handleWindiest(List<String> arguments) {
        requireArgumentCount("windiest", arguments, 0);

        WeatherData data = service.getWindiestLocation();
        out.printf(
                "The windiest configured location is %s with winds of %.1f mph.%n",
                data.city(),
                data.windSpeed()
        );
    }

    private void handleQuit(List<String> arguments) {
        requireArgumentCount("quit", arguments, 0);
        running = false;
        out.println("Goodbye!");
    }

    private static void requireArgumentCount(
            String commandSyntax,
            List<String> arguments,
            int expectedCount
    ) {
        if (arguments.size() != expectedCount) {
            throw new IllegalArgumentException("Usage: " + commandSyntax);
        }
    }

    static String renderBar(double temperature) {
        int barLength = (int) Math.max(0, Math.min(40, temperature / 2));
        return "■".repeat(barLength) + " ".repeat(40 - barLength);
    }

    static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();

        if (input == null || input.isBlank()) {
            return tokens;
        }

        StringBuilder currentToken = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < input.length(); index++) {
            char currentCharacter = input.charAt(index);

            if (currentCharacter == '"') {
                insideQuotes = !insideQuotes;
            } else if (Character.isWhitespace(currentCharacter) && !insideQuotes) {
                addToken(tokens, currentToken);
            } else {
                currentToken.append(currentCharacter);
            }
        }

        if (insideQuotes) {
            throw new IllegalArgumentException("Missing closing quotation mark");
        }

        addToken(tokens, currentToken);
        return tokens;
    }

    private static void addToken(List<String> tokens, StringBuilder currentToken) {
        if (!currentToken.isEmpty()) {
            tokens.add(currentToken.toString());
            currentToken.setLength(0);
        }
    }
}
