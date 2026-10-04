package weather.service;

import weather.model.Location;
import weather.model.WeatherComparison;
import weather.model.WeatherData;
import weather.provider.WeatherDataProvider;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class WeatherService {

    private static final List<Location> DEFAULT_LOCATIONS = List.of(
            new Location("Chicago", "41.85", "-87.65"),
            new Location("Los Angeles", "34.05", "-118.24"),
            new Location("New York", "40.71", "-74.01")
    );

    private final WeatherDataProvider provider;
    private final List<Location> locations = DEFAULT_LOCATIONS;

    public WeatherService(WeatherDataProvider provider) {
        this.provider = Objects.requireNonNull(provider, "WeatherDataProvider cannot be null");
    }

    public List<Location> getLocations() {
        return locations;
    }

    public WeatherData getCurrentWeather(String cityName) {
        Location location = findLocation(cityName);
        return provider.getCurrentWeather(location);
    }

    public WeatherComparison compare(String firstCity, String secondCity) {
        WeatherData first = getCurrentWeather(firstCity);
        WeatherData second = getCurrentWeather(secondCity);

        return new WeatherComparison(first, second);
    }

    public String getSummary(String cityName) {
        WeatherData data = getCurrentWeather(cityName);

        return String.format(
                "%s is %.1f°F, feels like %.1f°F, with winds of %.1f mph.",
                data.city(),
                data.temperature(),
                data.feelsLikeTemperature(),
                data.windSpeed()
        );
    }

    public WeatherData getWindiestLocation() {
        // this method will tave every location, retrieve the weather and comapre the wind speeds. then it will return the wind speed with highest speed.
        return locations.stream()
                .map(provider::getCurrentWeather)
                .max(Comparator.comparingDouble(WeatherData::windSpeed))
                .orElseThrow(() -> new NoSuchElementException("No locations are configured"));
    }

    private Location findLocation(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("Location name cannot be empty");
        }

        String requestedCity = cityName.trim();

        return locations.stream()
                .filter(location -> location.city().equalsIgnoreCase(requestedCity))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown location: " + cityName
                ));
    }
}
