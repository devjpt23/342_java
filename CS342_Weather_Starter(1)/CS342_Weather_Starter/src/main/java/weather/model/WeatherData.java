package weather.model;

// this is return type for any weather provider.
public record WeatherData(
        String city,
        double temperature,
        double feelsLikeTemperature,
        double windSpeed
) {}

