package weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.WeatherData;
import weather.provider.OpenMeteoWeatherProvider;
import weather.provider.WeatherProviderException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying OpenMeteoWeatherProvider parsing, code decoding,
 * and strict error handling (ensuring no silent fake fallbacks).
 */
class OpenMeteoWeatherProviderTest {

    private static final String VALID_JSON = """
            {
              "latitude": 41.85,
              "longitude": -87.65,
              "current": {
                "time": "2026-10-02T19:30",
                "interval": 900,
                "temperature_2m": 61.9,
                "relative_humidity_2m": 56,
                "apparent_temperature": 57.5,
                "weather_code": 0,
                "wind_speed_10m": 10.1
              }
            }
            """;

    @Test
    @DisplayName("Successfully parses valid Open-Meteo JSON into WeatherData")
    void testParseValidJson() {
        WeatherData data = OpenMeteoWeatherProvider.parseWeatherData("Chicago", VALID_JSON);

        assertEquals("Chicago", data.city());
        assertEquals(61.9, data.temperature(), 0.001);
        assertEquals(56.0, data.humidity(), 0.001);
        assertEquals(57.5, data.apparentTemperature(), 0.001);
        assertEquals(10.1, data.windSpeed(), 0.001);
        assertEquals("Clear sky", data.condition());
    }

    @Test
    @DisplayName("Throws WeatherProviderException on missing temperature instead of returning fake 72.0")
    void testMissingTemperatureThrowsException() {
        String missingTempJson = """
                {
                  "current": {
                    "relative_humidity_2m": 56,
                    "apparent_temperature": 57.5,
                    "weather_code": 0,
                    "wind_speed_10m": 10.1
                  }
                }
                """;

        WeatherProviderException ex = assertThrows(
                WeatherProviderException.class,
                () -> OpenMeteoWeatherProvider.parseWeatherData("Chicago", missingTempJson)
        );

        assertTrue(ex.getMessage().contains("Missing required weather field"));
    }

    @Test
    @DisplayName("Throws WeatherProviderException on null or blank JSON")
    void testNullOrBlankJsonThrowsException() {
        assertThrows(WeatherProviderException.class, () -> OpenMeteoWeatherProvider.parseWeatherData("Chicago", null));
        assertThrows(WeatherProviderException.class, () -> OpenMeteoWeatherProvider.parseWeatherData("Chicago", "   "));
    }

    @Test
    @DisplayName("Throws WeatherProviderException on missing 'current' block")
    void testMissingCurrentBlock() {
        String invalidJson = "{\"latitude\": 41.85, \"longitude\": -87.65}";
        assertThrows(WeatherProviderException.class, () -> OpenMeteoWeatherProvider.parseWeatherData("Chicago", invalidJson));
    }

    @Test
    @DisplayName("Correctly decodes diverse WMO weather codes")
    void testWeatherCodeDecoding() {
        assertEquals("Clear sky", OpenMeteoWeatherProvider.decodeWeatherCode(0));
        assertEquals("Partly cloudy", OpenMeteoWeatherProvider.decodeWeatherCode(2));
        assertEquals("Rain", OpenMeteoWeatherProvider.decodeWeatherCode(61));
        assertEquals("Snow", OpenMeteoWeatherProvider.decodeWeatherCode(71));
        assertEquals("Thunderstorm", OpenMeteoWeatherProvider.decodeWeatherCode(95));
        assertEquals("Conditions (999)", OpenMeteoWeatherProvider.decodeWeatherCode(999));
    }
}
