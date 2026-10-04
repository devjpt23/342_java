package weather.model;

import java.util.Objects;

public record WeatherComparison(
        WeatherData first,
        WeatherData second
) {
    public WeatherComparison {
        Objects.requireNonNull(first, "First weather data cannot be null");
        Objects.requireNonNull(second, "Second weather data cannot be null");
    }

    public double temperatureDifference() {
        return first.temperature() - second.temperature();
    }

    public double feelsLikeTemperatureDifference() {
        return first.feelsLikeTemperature() - second.feelsLikeTemperature();
    }

    public double windSpeedDifference() {
        return first.windSpeed() - second.windSpeed();
    }
}
