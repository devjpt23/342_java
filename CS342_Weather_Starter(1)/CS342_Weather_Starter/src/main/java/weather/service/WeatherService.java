package weather.service;

import weather.provider.WeatherDataProvider;

public class WeatherService {
    private final WeatherDataProvider provider;

    public WeatherService(WeatherDataProvider provider) {
        this.provider = provider;
    }
}
