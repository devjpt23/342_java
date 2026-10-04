package weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weather.model.WeatherComparison;
import weather.model.WeatherData;
import weather.service.WeatherService;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherServiceTest {

    private WeatherService service;

    @BeforeEach
    void setUp() {
        service = new WeatherService(new FakeWeatherDataProvider());
    }

    @Test
    void retrievesCurrentWeatherForChicago() {
        WeatherData weather = service.getCurrentWeather("Chicago");

        assertAll(
                () -> assertEquals("Chicago", weather.city()),
                () -> assertEquals(65.0, weather.temperature()),
                () -> assertEquals(60.0, weather.feelsLikeTemperature()),
                () -> assertEquals(20.0, weather.windSpeed())
        );
    }

    @Test
    void recognizesLosAngeles() {
        WeatherData weather = service.getCurrentWeather("Los Angeles");

        assertEquals("Los Angeles", weather.city());
        assertEquals(75.0, weather.temperature());
    }

    @Test
    void matchesLocationNamesCaseInsensitively() {
        WeatherData weather = service.getCurrentWeather("new york");

        assertEquals("New York", weather.city());
        assertEquals(15.0, weather.windSpeed());
    }

    @Test
    void rejectsUnknownLocation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getCurrentWeather("Detroit")
        );

        assertTrue(exception.getMessage().contains("Unknown location"));
    }

    @Test
    void comparesTwoLocations() {
        WeatherComparison comparison = service.compare("Chicago", "Los Angeles");

        assertAll(
                () -> assertEquals("Chicago", comparison.first().city()),
                () -> assertEquals("Los Angeles", comparison.second().city()),
                () -> assertEquals(-10.0, comparison.temperatureDifference()),
                () -> assertEquals(-14.0, comparison.feelsLikeTemperatureDifference()),
                () -> assertEquals(12.0, comparison.windSpeedDifference())
        );
    }

    @Test
    void generatesReadableSummary() {
        String summary = service.getSummary("Chicago");

        assertEquals(
                "Chicago is 65.0°F, feels like 60.0°F, with winds of 20.0 mph.",
                summary
        );
    }

    @Test
    void findsWindiestLocation() {
        WeatherData windiest = service.getWindiestLocation();

        assertEquals("Chicago", windiest.city());
        assertEquals(20.0, windiest.windSpeed());
    }

    @Test
    void propagatesProviderFailure() {
        WeatherService failingService = new WeatherService(
                new FakeWeatherDataProvider(true)
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> failingService.getCurrentWeather("Chicago")
        );

        assertEquals("Fake weather provider failure", exception.getMessage());
    }
}
