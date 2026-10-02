package weather.model;

import java.util.Objects;

/**
 * Represents a geographical location with city name and coordinates.
 */
public record Location(String city, String lat, String lon) {
    public Location {
        Objects.requireNonNull(city, "City name must not be null");
        Objects.requireNonNull(lat, "Latitude must not be null");
        Objects.requireNonNull(lon, "Longitude must not be null");
        if (city.isBlank()) {
            throw new IllegalArgumentException("City name cannot be blank");
        }
    }
}
