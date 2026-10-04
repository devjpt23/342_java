package weather;

import weather.model.Location;
import weather.model.WeatherData;
import weather.provider.WeatherDataProvider;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FakeWeatherDataProvider implements WeatherDataProvider {

    private static final Map<String, WeatherData> WEATHER_BY_CITY = Map.of(
            "chicago", new WeatherData("Chicago", 65.0, 60.0, 20.0),
            "los angeles", new WeatherData("Los Angeles", 75.0, 74.0, 8.0),
            "new york", new WeatherData("New York", 70.0, 68.0, 15.0)
    );

    private final boolean shouldFail;

    public FakeWeatherDataProvider() {
        this(false);
    }

    public FakeWeatherDataProvider(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public WeatherData getCurrentWeather(Location location) {
        Objects.requireNonNull(location, "Location cannot be null");

        if (shouldFail) {
            throw new IllegalStateException("Fake weather provider failure");
        }

        String cityKey = location.city().toLowerCase(Locale.ROOT);
        WeatherData weatherData = WEATHER_BY_CITY.get(cityKey);

        if (weatherData == null) {
            throw new IllegalArgumentException(
                    "No fake weather configured for " + location.city()
            );
        }

        return weatherData;
    }
}
