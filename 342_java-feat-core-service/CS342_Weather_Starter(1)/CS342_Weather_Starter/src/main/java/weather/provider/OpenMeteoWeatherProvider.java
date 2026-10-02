package weather.provider;

import weather.model.Location;
import weather.model.WeatherData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production implementation of WeatherDataProvider using the Open-Meteo Forecast API.
 */
public class OpenMeteoWeatherProvider implements WeatherDataProvider {

    private static final String DEFAULT_BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private final HttpClient httpClient;
    private final String baseUrl;

    public OpenMeteoWeatherProvider() {
        this(HttpClient.newHttpClient(), DEFAULT_BASE_URL);
    }

    public OpenMeteoWeatherProvider(HttpClient httpClient) {
        this(httpClient, DEFAULT_BASE_URL);
    }

    public OpenMeteoWeatherProvider(HttpClient httpClient, String baseUrl) {
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
    }

    @Override
    public WeatherData getCurrentWeather(Location location) throws WeatherProviderException {
        Objects.requireNonNull(location, "Location cannot be null");

        String url = String.format(
                "%s?latitude=%s&longitude=%s&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&temperature_unit=fahrenheit&wind_speed_unit=mph",
                baseUrl, location.lat(), location.lon()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new WeatherProviderException(
                        "Open-Meteo API error for " + location.city() + ": HTTP " + response.statusCode()
                );
            }

            return parseWeatherData(location.city(), response.body());

        } catch (IOException e) {
            throw new WeatherProviderException(
                    "Network failure while fetching weather for " + location.city() + ": " + e.getMessage(), e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherProviderException("Weather fetch request was interrupted", e);
        }
    }

    /**
     * Parses the JSON payload from the Open-Meteo API response.
     * Ensures errors are thrown on malformed or missing data instead of returning silent fallbacks.
     */
    public static WeatherData parseWeatherData(String cityName, String json) {
        if (json == null || json.isBlank()) {
            throw new WeatherProviderException("Received empty response from Open-Meteo API");
        }

        // Locate current weather object to isolate from metadata/units
        int currentIdx = json.indexOf("\"current\":");
        if (currentIdx == -1) {
            throw new WeatherProviderException("API response missing 'current' weather block: " + json);
        }
        String currentSection = json.substring(currentIdx);

        double temp = extractDouble(currentSection, "\"temperature_2m\":\\s*([0-9.-]+)", "temperature_2m");
        double humidity = extractDouble(currentSection, "\"relative_humidity_2m\":\\s*([0-9.-]+)", "relative_humidity_2m");
        double apparentTemp = extractDouble(currentSection, "\"apparent_temperature\":\\s*([0-9.-]+)", "apparent_temperature");
        double windSpeed = extractDouble(currentSection, "\"wind_speed_10m\":\\s*([0-9.-]+)", "wind_speed_10m");
        int weatherCode = extractInt(currentSection, "\"weather_code\":\\s*([0-9]+)", "weather_code");

        String condition = decodeWeatherCode(weatherCode);

        return new WeatherData(cityName, temp, humidity, windSpeed, apparentTemp, condition);
    }

    private static double extractDouble(String source, String regex, String fieldName) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new WeatherProviderException("Failed to parse numeric value for " + fieldName, e);
            }
        }
        throw new WeatherProviderException("Missing required weather field in API response: " + fieldName);
    }

    private static int extractInt(String source, String regex, String fieldName) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new WeatherProviderException("Failed to parse integer value for " + fieldName, e);
            }
        }
        throw new WeatherProviderException("Missing required weather field in API response: " + fieldName);
    }

    /**
     * Maps WMO weather interpretation codes to human-readable weather descriptions.
     * Uses Java 25 enhanced switch expressions.
     */
    public static String decodeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing Drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing Rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Conditions (" + code + ")";
        };
    }
}
