package weather.model;

import java.util.Objects;

/**
 * Immutable record representing current weather data for a city.
 * Holds temperature (°F) and additional measurements (humidity, wind speed, apparent temperature, condition).
 */
public record WeatherData(
        String city,
        double temperature,
        double humidity,
        double windSpeed,
        double apparentTemperature,
        String condition
) {
    public WeatherData {
        Objects.requireNonNull(city, "City name must not be null");
        Objects.requireNonNull(condition, "Condition must not be null");
    }

    /**
     * Convenience constructor with default values for secondary measurements.
     */
    public WeatherData(String city, double temperature) {
        this(city, temperature, 0.0, 0.0, temperature, "Clear");
    }

    /**
     * Convenience constructor without apparent temperature.
     */
    public WeatherData(String city, double temperature, double humidity, double windSpeed, String condition) {
        this(city, temperature, humidity, windSpeed, temperature, condition);
    }
}
