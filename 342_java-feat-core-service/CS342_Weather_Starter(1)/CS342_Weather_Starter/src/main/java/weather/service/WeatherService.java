package weather.service;

import weather.model.Location;
import weather.model.WeatherComparison;
import weather.model.WeatherData;
import weather.provider.WeatherDataProvider;
import weather.provider.WeatherProviderException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Service encapsulating weather application logic.
 * Decoupled from concrete HTTP providers via constructor-based dependency injection.
 * Does not print directly to console.
 */
public class WeatherService {

    public static final List<Location> DEFAULT_LOCATIONS = List.of(
            new Location("Chicago", "41.85", "-87.65"),
            new Location("Los Angeles", "34.05", "-118.24"),
            new Location("New York", "40.71", "-74.01")
    );

    private final WeatherDataProvider provider;
    private final List<Location> knownLocations;

    public WeatherService(WeatherDataProvider provider) {
        this(provider, DEFAULT_LOCATIONS);
    }

    public WeatherService(WeatherDataProvider provider, List<Location> knownLocations) {
        this.provider = Objects.requireNonNull(provider, "WeatherDataProvider cannot be null");
        Objects.requireNonNull(knownLocations, "Known locations cannot be null");
        this.knownLocations = new ArrayList<>(knownLocations);
    }

    /**
     * Returns an unmodifiable list of known locations configured in the system.
     */
    public List<Location> getKnownLocations() {
        return Collections.unmodifiableList(knownLocations);
    }

    /**
     * Looks up a location by city name (case-insensitive).
     */
    public Optional<Location> findLocation(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return Optional.empty();
        }
        var target = cityName.trim();
        return knownLocations.stream()
                .filter(loc -> loc.city().equalsIgnoreCase(target))
                .findFirst();
    }

    /**
     * Adds a new known location to the service.
     */
    public void addLocation(Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        if (findLocation(location.city()).isPresent()) {
            throw new IllegalArgumentException("Location already exists: " + location.city());
        }
        knownLocations.add(location);
    }

    /**
     * Retrieves weather data for a given Location.
     */
    public WeatherData getWeather(Location location) throws WeatherProviderException {
        Objects.requireNonNull(location, "Location cannot be null");
        return provider.getCurrentWeather(location);
    }

    /**
     * Retrieves weather data for a city by name.
     * Throws IllegalArgumentException if the city is not a known location.
     */
    public WeatherData getWeather(String cityName) throws WeatherProviderException {
        return findLocation(cityName)
                .map(this::getWeather)
                .orElseThrow(() -> new IllegalArgumentException("Unknown location: " + cityName));
    }

    /**
     * Compares current weather between two configured cities.
     */
    public WeatherComparison compareWeather(String cityName1, String cityName2) {
        var first = getWeather(cityName1);
        var second = getWeather(cityName2);
        return new WeatherComparison(first, second);
    }

    /**
     * Generates a readable natural-language summary for a given city.
     */
    public String generateSummary(String cityName) {
        var data = getWeather(cityName);
        return String.format(
                "Currently in %s, conditions are %s with a temperature of %.1f°F (feels like %.1f°F). " +
                "Relative humidity is at %.0f%% with winds blowing at %.1f mph.",
                data.city(),
                data.condition(),
                data.temperature(),
                data.apparentTemperature(),
                data.humidity(),
                data.windSpeed()
        );
    }

    /**
     * Additional Feature: Finds the warmest known location using stream reduction.
     */
    public WeatherData getWarmestLocation() {
        if (knownLocations.isEmpty()) {
            throw new NoSuchElementException("No locations configured in WeatherService");
        }
        return knownLocations.stream()
                .map(this::getWeather)
                .max(Comparator.comparingDouble(WeatherData::temperature))
                .orElseThrow();
    }

    /**
     * Additional Feature: Finds the coldest known location using stream reduction.
     */
    public WeatherData getColdestLocation() {
        if (knownLocations.isEmpty()) {
            throw new NoSuchElementException("No locations configured in WeatherService");
        }
        return knownLocations.stream()
                .map(this::getWeather)
                .min(Comparator.comparingDouble(WeatherData::temperature))
                .orElseThrow();
    }

    /**
     * Additional Feature: Retrieves weather data for all known locations.
     */
    public List<WeatherData> getAllWeatherData() {
        return knownLocations.stream()
                .map(this::getWeather)
                .toList();
    }
}
