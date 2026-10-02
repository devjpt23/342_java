package weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.Location;
import weather.model.WeatherComparison;
import weather.model.WeatherData;
import weather.provider.WeatherProviderException;
import weather.service.WeatherService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for WeatherService.
 * Uses controlled test implementation of WeatherDataProvider to ensure 100% offline isolation.
 */
class WeatherServiceTest {

    private TestWeatherDataProvider testProvider;
    private WeatherService service;

    @BeforeEach
    void setUp() {
        testProvider = new TestWeatherDataProvider();
        service = new WeatherService(testProvider);
    }

    @Test
    @DisplayName("1. Successfully retrieves current weather for known location")
    void testGetCurrentWeatherSuccess() {
        WeatherData weather = service.getWeather("Chicago");

        assertNotNull(weather);
        assertEquals("Chicago", weather.city());
        assertEquals(65.0, weather.temperature(), 0.001);
        assertEquals(55.0, weather.humidity(), 0.001);
        assertEquals(12.0, weather.windSpeed(), 0.001);
        assertEquals("Partly cloudy", weather.condition());
    }

    @Test
    @DisplayName("2. Correctly returns and verifies all default known locations")
    void testKnownLocationsList() {
        List<Location> locations = service.getKnownLocations();

        assertEquals(3, locations.size());
        List<String> cityNames = locations.stream().map(Location::city).toList();
        assertTrue(cityNames.contains("Chicago"));
        assertTrue(cityNames.contains("Los Angeles"));
        assertTrue(cityNames.contains("New York"));
    }

    @Test
    @DisplayName("3. Correctly handles case-insensitive lookup of known locations")
    void testFindLocationCaseInsensitive() {
        Optional<Location> lower = service.findLocation("chicago");
        Optional<Location> upper = service.findLocation("NEW YORK");
        Optional<Location> mixed = service.findLocation("   Los Angeles   ");

        assertTrue(lower.isPresent());
        assertEquals("Chicago", lower.get().city());

        assertTrue(upper.isPresent());
        assertEquals("New York", upper.get().city());

        assertTrue(mixed.isPresent());
        assertEquals("Los Angeles", mixed.get().city());
    }

    @Test
    @DisplayName("4. Correctly throws exception when requesting unknown location")
    void testHandlingUnknownLocationThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getWeather("Atlantis")
        );

        assertTrue(exception.getMessage().contains("Unknown location: Atlantis"));
    }

    @Test
    @DisplayName("5. Correctly computes weather comparison metrics between two locations")
    void testWeatherComparisonLogic() {
        WeatherComparison comparison = service.compareWeather("Chicago", "Los Angeles");

        assertNotNull(comparison);
        assertEquals("Chicago", comparison.first().city());
        assertEquals("Los Angeles", comparison.second().city());

        // Chicago temp = 65.0, LA temp = 78.0 -> diff = -13.0
        assertEquals(-13.0, comparison.temperatureDifference(), 0.001);
        // Chicago humidity = 55.0, LA humidity = 40.0 -> diff = 15.0
        assertEquals(15.0, comparison.humidityDifference(), 0.001);
        // Chicago wind = 12.0, LA wind = 5.0 -> diff = 7.0
        assertEquals(7.0, comparison.windSpeedDifference(), 0.001);
    }

    @Test
    @DisplayName("6. Generates descriptive summary containing essential weather metrics")
    void testSummaryGenerationLogic() {
        String summary = service.generateSummary("Chicago");

        assertNotNull(summary);
        assertTrue(summary.contains("Chicago"));
        assertTrue(summary.contains("65.0°F"));
        assertTrue(summary.contains("Partly cloudy"));
        assertTrue(summary.contains("55%"));
        assertTrue(summary.contains("12.0 mph"));
    }

    @Test
    @DisplayName("7. Additional Feature: Correctly identifies warmest configured location")
    void testAdditionalFeatureWarmestLocation() {
        WeatherData warmest = service.getWarmestLocation();

        assertNotNull(warmest);
        assertEquals("Los Angeles", warmest.city());
        assertEquals(78.0, warmest.temperature(), 0.001);
    }

    @Test
    @DisplayName("8. Additional Feature: Correctly identifies coldest configured location")
    void testAdditionalFeatureColdestLocation() {
        WeatherData coldest = service.getColdestLocation();

        assertNotNull(coldest);
        assertEquals("New York", coldest.city());
        assertEquals(58.0, coldest.temperature(), 0.001);
    }

    @Test
    @DisplayName("9. Additional Feature: Retrieves weather data for all configured locations")
    void testGetAllWeatherData() {
        List<WeatherData> allData = service.getAllWeatherData();

        assertEquals(3, allData.size());
        List<String> cities = allData.stream().map(WeatherData::city).toList();
        assertTrue(cities.contains("Chicago"));
        assertTrue(cities.contains("Los Angeles"));
        assertTrue(cities.contains("New York"));
    }

    @Test
    @DisplayName("10. Explicitly propagates provider failures without silent fake fallbacks")
    void testWeatherDataProviderFailureHandling() {
        testProvider.setShouldFail(true);
        testProvider.setFailureMessage("HTTP 500: Server Internal Error");

        WeatherProviderException ex = assertThrows(
                WeatherProviderException.class,
                () -> service.getWeather("Chicago")
        );

        assertTrue(ex.getMessage().contains("HTTP 500: Server Internal Error"));
    }

    @Test
    @DisplayName("11. Supports dynamic registration of custom locations")
    void testAddCustomLocation() {
        Location miami = new Location("Miami", "25.76", "-80.19");
        service.addLocation(miami);

        assertTrue(service.findLocation("Miami").isPresent());
        WeatherData data = service.getWeather("Miami");
        assertEquals("Miami", data.city());

        // Duplicate addition should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> service.addLocation(miami));
    }

    @Test
    @DisplayName("12. Supports dependency injection of custom location lists")
    void testCustomLocationsInjection() {
        List<Location> customLocations = List.of(
                new Location("Seattle", "47.60", "-122.33"),
                new Location("Austin", "30.26", "-97.74")
        );
        WeatherService customService = new WeatherService(testProvider, customLocations);

        assertEquals(2, customService.getKnownLocations().size());
        assertTrue(customService.findLocation("Seattle").isPresent());
        assertFalse(customService.findLocation("Chicago").isPresent());
    }
}
