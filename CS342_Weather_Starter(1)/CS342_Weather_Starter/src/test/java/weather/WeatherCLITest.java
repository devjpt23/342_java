package weather;

import org.junit.jupiter.api.Test;
import weather.cli.WeatherCLI;
import weather.provider.WeatherDataProvider;
import weather.service.WeatherService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherCLITest {

    @Test
    void helpDisplaysAllCommandSyntax() {
        String output = runCli(
                new FakeWeatherDataProvider(),
                "help\nquit\n"
        );

        assertAll(
                () -> assertTrue(output.contains("current <location>")),
                () -> assertTrue(output.contains("compare <location1> <location2>")),
                () -> assertTrue(output.contains("summary <location>")),
                () -> assertTrue(output.contains("windiest")),
                () -> assertTrue(output.contains("quit"))
        );
    }

    @Test
    void locationsDisplaysAllConfiguredCities() {
        String output = runCli(
                new FakeWeatherDataProvider(),
                "locations\nquit\n"
        );

        assertAll(
                () -> assertTrue(output.contains("Chicago")),
                () -> assertTrue(output.contains("Los Angeles")),
                () -> assertTrue(output.contains("New York"))
        );
    }

    @Test
    void currentAndSummaryDisplayWeatherMeasurements() {
        String output = runCli(
                new FakeWeatherDataProvider(),
                "current Chicago\nsummary Chicago\nquit\n"
        );

        assertAll(
                () -> assertTrue(output.contains("Current weather for Chicago")),
                () -> assertTrue(output.contains("Temperature: 65.0°F")),
                () -> assertTrue(output.contains("Feels like:  60.0°F")),
                () -> assertTrue(output.contains("Wind speed:  20.0 mph")),
                () -> assertTrue(output.contains("Chicago is 65.0°F"))
        );
    }

    @Test
    void compareAndWindiestDisplayExpectedResults() {
        String output = runCli(
                new FakeWeatherDataProvider(),
                "compare Chicago \"New York\"\nwindiest\nquit\n"
        );

        assertAll(
                () -> assertTrue(output.contains("Measurement")),
                () -> assertTrue(output.contains("Chicago")),
                () -> assertTrue(output.contains("New York")),
                () -> assertTrue(output.contains("5.0 mph wind speed")),
                () -> assertTrue(output.contains("windiest configured location is Chicago"))
        );
    }

    @Test
    void invalidInputAndProviderFailureDisplayErrorsAndStillQuit() {
        String output = runCli(
                new FakeWeatherDataProvider(true),
                "pizza\ncurrent\ncurrent Chicago\nquit\n"
        );

        assertAll(
                () -> assertTrue(output.contains("Unknown command: pizza")),
                () -> assertTrue(output.contains("Usage: current <location>")),
                () -> assertTrue(output.contains("Fake weather provider failure")),
                () -> assertTrue(output.contains("Goodbye!"))
        );
    }

    private String runCli(WeatherDataProvider provider, String commands) {
        ByteArrayInputStream input = new ByteArrayInputStream(
                commands.getBytes(StandardCharsets.UTF_8)
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(
                output,
                true,
                StandardCharsets.UTF_8
        );

        WeatherService service = new WeatherService(provider);
        WeatherCLI cli = new WeatherCLI(service, input, printStream);
        cli.start();

        return output.toString(StandardCharsets.UTF_8);
    }
}
