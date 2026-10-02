package weather;

import weather.cli.WeatherCLI;
import weather.provider.OpenMeteoWeatherProvider;
import weather.service.WeatherService;

/**
 * Composition root for the Weather Information Service application.
 * Assembles production dependencies via constructor-based dependency injection
 * and launches the CLI interface.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Instantiate concrete weather provider
        OpenMeteoWeatherProvider provider = new OpenMeteoWeatherProvider();

        // 2. Inject provider into domain service
        WeatherService service = new WeatherService(provider);

        // 3. Inject service into command line interface
        WeatherCLI cli = new WeatherCLI(service);

        // 4. Start user interaction
        cli.start();
    }
}
