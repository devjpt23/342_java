package weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.cli.WeatherCLI;
import weather.service.WeatherService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for WeatherCLI.
 * Verifies command parsing, argument handling, service invocation, and presentation.
 */
class WeatherCLITest {

    private TestWeatherDataProvider testProvider;
    private WeatherService service;
    private ByteArrayOutputStream outputStream;
    private PrintStream printStream;
    private WeatherCLI cli;

    @BeforeEach
    void setUp() {
        testProvider = new TestWeatherDataProvider();
        service = new WeatherService(testProvider);
        outputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
        cli = new WeatherCLI(service, new ByteArrayInputStream(new byte[0]), printStream);
    }

    private String getCapturedOutput() {
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("1. 'help' command displays all available commands and syntax")
    void testHelpCommand() {
        cli.executeCommand("help");
        String output = getCapturedOutput();

        assertTrue(output.contains("help"));
        assertTrue(output.contains("locations"));
        assertTrue(output.contains("current <location>"));
        assertTrue(output.contains("compare <loc1> <loc2>"));
        assertTrue(output.contains("summary <location>"));
        assertTrue(output.contains("warmest"));
        assertTrue(output.contains("coldest"));
        assertTrue(output.contains("all"));
        assertTrue(output.contains("quit"));
    }

    @Test
    @DisplayName("2. 'locations' command lists all configured cities")
    void testLocationsCommand() {
        cli.executeCommand("locations");
        String output = getCapturedOutput();

        assertTrue(output.contains("Chicago"));
        assertTrue(output.contains("Los Angeles"));
        assertTrue(output.contains("New York"));
    }

    @Test
    @DisplayName("3. 'current' command displays temperature and additional measurements")
    void testCurrentCommand() {
        cli.executeCommand("current Chicago");
        String output = getCapturedOutput();

        assertTrue(output.contains("Weather for Chicago:"));
        assertTrue(output.contains("65.0°F"));
        assertTrue(output.contains("Partly cloudy"));
        assertTrue(output.contains("Relative Humidity:"));
        assertTrue(output.contains("55%"));
        assertTrue(output.contains("Wind Speed:"));
        assertTrue(output.contains("12.0 mph"));
    }

    @Test
    @DisplayName("4. 'compare' command properly parses quoted arguments and displays comparison table")
    void testCompareCommandWithQuotes() {
        cli.executeCommand("compare Chicago \"New York\"");
        String output = getCapturedOutput();

        assertTrue(output.contains("Chicago"));
        assertTrue(output.contains("New York"));
        assertTrue(output.contains("Temperature"));
        assertTrue(output.contains("Difference (Chicago - New York)"));
        assertTrue(output.contains("Temp:  +7.0°F"));
    }

    @Test
    @DisplayName("5. 'summary' command produces readable summary sentence")
    void testSummaryCommand() {
        cli.executeCommand("summary Chicago");
        String output = getCapturedOutput();

        assertTrue(output.contains("Currently in Chicago"));
        assertTrue(output.contains("65.0°F"));
        assertTrue(output.contains("Partly cloudy"));
        assertTrue(output.contains("55%"));
        assertTrue(output.contains("12.0 mph"));
    }

    @Test
    @DisplayName("6. Additional feature: 'warmest', 'coldest', and 'all' commands")
    void testAdditionalFeatures() {
        cli.executeCommand("warmest");
        String output1 = getCapturedOutput();
        assertTrue(output1.contains("Warmest Location: Los Angeles at  78.0°F"));

        outputStream.reset();
        cli.executeCommand("coldest");
        String output2 = getCapturedOutput();
        assertTrue(output2.contains("Coldest Location: New York at  58.0°F"));

        outputStream.reset();
        cli.executeCommand("all");
        String output3 = getCapturedOutput();
        assertTrue(output3.contains("Chicago"));
        assertTrue(output3.contains("Los Angeles"));
        assertTrue(output3.contains("New York"));
    }

    @Test
    @DisplayName("7. Invalid command handling displays error without crashing")
    void testInvalidCommandHandling() {
        cli.executeCommand("invalidCommand123");
        String output = getCapturedOutput();

        assertTrue(output.contains("Unknown command: 'invalidcommand123'"));
        assertTrue(output.contains("Type 'help'"));
    }

    @Test
    @DisplayName("8. Missing arguments handling displays proper usage instructions")
    void testMissingArgumentsHandling() {
        cli.executeCommand("current");
        String outputCurrent = getCapturedOutput();
        assertTrue(outputCurrent.contains("Usage: current <location>"));

        outputStream.reset();
        cli.executeCommand("compare Chicago");
        String outputCompare = getCapturedOutput();
        assertTrue(outputCompare.contains("Usage: compare <location1> <location2>"));

        outputStream.reset();
        cli.executeCommand("summary");
        String outputSummary = getCapturedOutput();
        assertTrue(outputSummary.contains("Usage: summary <location>"));
    }

    @Test
    @DisplayName("9. Unknown location input displays clean error message")
    void testUnknownLocationInput() {
        cli.executeCommand("current Narnia");
        String output = getCapturedOutput();

        assertTrue(output.contains("Input Error: Unknown location: Narnia"));
    }

    @Test
    @DisplayName("10. Provider failure is caught and displayed gracefully")
    void testProviderFailureGracefulHandling() {
        testProvider.setShouldFail(true);
        testProvider.setFailureMessage("Connection timed out to Open-Meteo");

        cli.executeCommand("current Chicago");
        String output = getCapturedOutput();

        assertTrue(output.contains("Provider Failure: Connection timed out to Open-Meteo"));
    }

    @Test
    @DisplayName("11. Interactive start session exits cleanly on 'quit'")
    void testInteractiveSessionQuit() {
        String simulatedInput = "locations\nquit\n";
        ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);

        WeatherCLI interactiveCli = new WeatherCLI(service, in, ps);
        interactiveCli.start();

        String result = out.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Weather Information Service CLI"));
        assertTrue(result.contains("Configured Locations:"));
        assertTrue(result.contains("Goodbye!"));
    }
}
