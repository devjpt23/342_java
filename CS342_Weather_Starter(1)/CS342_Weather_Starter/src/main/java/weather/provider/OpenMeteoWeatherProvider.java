package weather.provider;

import weather.model.Location;
import weather.model.WeatherData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenMeteoWeatherProvider implements WeatherDataProvider {

    private static final String API_URL = "https://api.open-meteo.com/v1/forecast";

    @Override
    public WeatherData getCurrentWeather(Location location) {
        String url = API_URL
                + "?latitude=" + location.lat()
                + "&longitude=" + location.lon()
                + "&current=temperature_2m,apparent_temperature,wind_speed_10m"
                + "&temperature_unit=fahrenheit"
                + "&wind_speed_unit=mph";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Open-Meteo returned HTTP " + response.statusCode()
                                + " for " + location.city()
                );
            }

            return parseWeatherData(location.city(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Weather request was interrupted for " + location.city(),
                    e
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not retrieve weather for " + location.city(),
                    e
            );
        }
    }

    static WeatherData parseWeatherData(String city, String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Weather response cannot be empty");
        }

        double temperature = extractNumber(json, "temperature_2m");
        double feelsLikeTemperature = extractNumber(json, "apparent_temperature");
        double windSpeed = extractNumber(json, "wind_speed_10m");

        if (windSpeed < 0) {
            throw new IllegalArgumentException("Wind speed cannot be negative");
        }

        return new WeatherData(
                city,
                temperature,
                feelsLikeTemperature,
                windSpeed
        );
    }

    private static double extractNumber(String json, String fieldName) {
        String numberPattern = "(-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)";
        Pattern pattern = Pattern.compile(
                "\\\"" + Pattern.quote(fieldName) + "\\\"\\s*:\\s*" + numberPattern
        );
        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Weather response is missing " + fieldName
            );
        }

        double value;
        try {
            value = Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Weather response contains an invalid " + fieldName,
                    e
            );
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Weather response contains a non-finite " + fieldName
            );
        }

        return value;
    }
}
