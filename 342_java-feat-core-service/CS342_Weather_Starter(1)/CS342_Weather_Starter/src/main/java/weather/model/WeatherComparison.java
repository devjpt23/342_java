package weather.model;

import java.util.Objects;

/**
 * Immutable model representing a comparison between weather conditions at two locations.
 */
public record WeatherComparison(
        WeatherData first,
        WeatherData second,
        double temperatureDifference,
        double humidityDifference,
        double windSpeedDifference
) {
    public WeatherComparison {
        Objects.requireNonNull(first, "First weather data cannot be null");
        Objects.requireNonNull(second, "Second weather data cannot be null");
    }

    public WeatherComparison(WeatherData first, WeatherData second) {
        this(
                first,
                second,
                first.temperature() - second.temperature(),
                first.humidity() - second.humidity(),
                first.windSpeed() - second.windSpeed()
        );
    }
}
