package weather.cli;

import weather.model.Location;
import weather.model.WeatherComparison;
import weather.model.WeatherData;
import weather.provider.WeatherProviderException;
import weather.service.WeatherService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command-line interface for the Weather Information Service.
 * Responsible for input reading, command parsing, invoking WeatherService,
 * and presenting formatted output to the user.
 */
public class WeatherCLI {

    private final WeatherService service;
    private final Scanner scanner;
    private final PrintStream out;
    private boolean running = true;

    public WeatherCLI(WeatherService service) {
        this(service, System.in, System.out);
    }

    public WeatherCLI(WeatherService service, InputStream in, PrintStream out) {
        this(service, new Scanner(in), out);
    }

    public WeatherCLI(WeatherService service, Scanner scanner, PrintStream out) {
        this.service = Objects.requireNonNull(service, "WeatherService cannot be null");
        this.scanner = Objects.requireNonNull(scanner, "Scanner cannot be null");
        this.out = Objects.requireNonNull(out, "PrintStream cannot be null");
    }

    /**
     * Starts the interactive command loop.
     */
    public void start() {
        out.println("=================================================");
        out.println("     🌤️  Weather Information Service CLI        ");
        out.println("=================================================");
        out.println("Type 'help' to see available commands or 'quit' to exit.\n");

        while (running && scanner.hasNextLine()) {
            out.print("weather> ");
            var line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            executeCommand(line);
        }
    }

    /**
     * Executes a single command string and displays output.
     */
    public void executeCommand(String commandLine) {
        var tokens = tokenize(commandLine);
        if (tokens.isEmpty()) {
            return;
        }

        var command = tokens.getFirst().toLowerCase();
        var args = tokens.subList(1, tokens.size());

        try {
            switch (command) {
                case "help" -> handleHelp();
                case "locations" -> handleLocations();
                case "current" -> handleCurrent(args);
                case "compare" -> handleCompare(args);
                case "summary" -> handleSummary(args);
                case "warmest" -> handleWarmest();
                case "coldest" -> handleColdest();
                case "all" -> handleAll();
                case "quit", "exit" -> handleQuit();
                default -> out.printf("⚠️  Unknown command: '%s'. Type 'help' for available commands.%n", command);
            }
        } catch (IllegalArgumentException e) {
            out.println("⚠️  Input Error: " + e.getMessage());
        } catch (WeatherProviderException e) {
            out.println("❌ Provider Failure: " + e.getMessage());
        } catch (Exception e) {
            out.println("❌ Unexpected Error: " + e.getMessage());
        }
    }

    private void handleHelp() {
        out.println("""
                Available Commands:
                  help                           - Display available commands and their syntax
                  locations                      - List all configured locations known to the system
                  current <location>             - Display current weather and measurements for a location
                  compare <loc1> <loc2>          - Compare current weather between two locations (use quotes for names with spaces)
                  summary <location>             - Produce a readable summary of current weather conditions
                  warmest                        - Show the warmest location among all configured cities
                  coldest                        - Show the coldest location among all configured cities
                  all                            - Display current weather overview for all configured cities
                  quit                           - Exit the application cleanly
                """);
    }

    private void handleLocations() {
        var locations = service.getKnownLocations();
        out.println("Configured Locations:");
        for (Location loc : locations) {
            out.printf("  • %-15s (Lat: %s, Lon: %s)%n", loc.city(), loc.lat(), loc.lon());
        }
    }

    private void handleCurrent(List<String> args) {
        if (args.isEmpty()) {
            out.println("⚠️  Usage: current <location> (e.g. current Chicago or current \"New York\")");
            return;
        }
        var cityName = args.getFirst();
        var data = service.getWeather(cityName);

        out.printf("Weather for %s:%n", data.city());
        out.printf("  Temperature:       %5.1f°F  [%s]%n", data.temperature(), renderBar(data.temperature()));
        out.printf("  Feels Like:        %5.1f°F%n", data.apparentTemperature());
        out.printf("  Condition:         %s%n", data.condition());
        out.printf("  Relative Humidity: %5.0f%%%n", data.humidity());
        out.printf("  Wind Speed:        %5.1f mph%n", data.windSpeed());
    }

    private void handleCompare(List<String> args) {
        if (args.size() < 2) {
            out.println("⚠️  Usage: compare <location1> <location2> (e.g. compare Chicago \"New York\")");
            return;
        }
        var loc1 = args.get(0);
        var loc2 = args.get(1);

        WeatherComparison comparison = service.compareWeather(loc1, loc2);
        WeatherData w1 = comparison.first();
        WeatherData w2 = comparison.second();

        out.println("--------------------------------------------------------------------------------");
        out.printf("%-20s | %-25s | %-25s%n", "Metric", w1.city(), w2.city());
        out.println("--------------------------------------------------------------------------------");
        out.printf("%-20s | %5.1f°F                   | %5.1f°F%n", "Temperature", w1.temperature(), w2.temperature());
        out.printf("%-20s | %5.1f°F                   | %5.1f°F%n", "Feels Like", w1.apparentTemperature(), w2.apparentTemperature());
        out.printf("%-20s | %-25s | %-25s%n", "Condition", w1.condition(), w2.condition());
        out.printf("%-20s | %5.0f%%                    | %5.0f%%%n", "Humidity", w1.humidity(), w2.humidity());
        out.printf("%-20s | %5.1f mph                 | %5.1f mph%n", "Wind Speed", w1.windSpeed(), w2.windSpeed());
        out.println("--------------------------------------------------------------------------------");
        out.printf("Difference (%s - %s): Temp: %+5.1f°F, Humidity: %+5.0f%%, Wind: %+5.1f mph%n",
                w1.city(), w2.city(),
                comparison.temperatureDifference(),
                comparison.humidityDifference(),
                comparison.windSpeedDifference());
    }

    private void handleSummary(List<String> args) {
        if (args.isEmpty()) {
            out.println("⚠️  Usage: summary <location> (e.g. summary Chicago)");
            return;
        }
        var cityName = args.getFirst();
        var summary = service.generateSummary(cityName);
        out.println(summary);
    }

    private void handleWarmest() {
        var warmest = service.getWarmestLocation();
        out.printf("🔥 Warmest Location: %s at %5.1f°F (%s, feels like %5.1f°F)%n",
                warmest.city(), warmest.temperature(), warmest.condition(), warmest.apparentTemperature());
    }

    private void handleColdest() {
        var coldest = service.getColdestLocation();
        out.printf("❄️  Coldest Location: %s at %5.1f°F (%s, feels like %5.1f°F)%n",
                coldest.city(), coldest.temperature(), coldest.condition(), coldest.apparentTemperature());
    }

    private void handleAll() {
        var list = service.getAllWeatherData();
        out.println("Current Conditions Overview:");
        for (WeatherData d : list) {
            out.printf("  %-15s | %5.1f°F | %-16s | Humidity: %3.0f%% | Wind: %4.1f mph [%s]%n",
                    d.city(), d.temperature(), d.condition(), d.humidity(), d.windSpeed(), renderBar(d.temperature()));
        }
    }

    private void handleQuit() {
        out.println("Goodbye!");
        running = false;
    }

    /**
     * Renders a visual text-based bar for temperature.
     */
    public static String renderBar(double temp) {
        int length = (int) Math.max(0, Math.min(30, temp / 2));
        return "■".repeat(length) + " ".repeat(Math.max(0, 30 - length));
    }

    /**
     * Tokenizes a command line into arguments, preserving quoted strings.
     */
    public static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return tokens;
        }

        Matcher matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(input.trim());
        while (matcher.find()) {
            String token = matcher.group(1).trim();
            if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                token = token.substring(1, token.length() - 1).trim();
            }
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
