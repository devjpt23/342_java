package weather;

import weather.cli.WeatherCLI;
import weather.provider.OpenMeteoWeatherProvider;
import weather.provider.WeatherDataProvider;
import weather.service.WeatherService;

public class Main {

    public static void main(String[] args) {
        WeatherDataProvider provider = new OpenMeteoWeatherProvider();
        WeatherService service = new WeatherService(provider);
        WeatherCLI cli = new WeatherCLI(service);

        cli.start();
    }
}
