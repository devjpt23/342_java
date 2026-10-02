package weather.provider;

import weather.model.Location;
import weather.model.WeatherData;

/**
 * Abstraction representing a source of weather information.
 * Allows decoupling application and service logic from external APIs.
 */
public interface WeatherDataProvider {

    /**
     * Retrieves current weather information for a specific location.
     *
     * @param location the target location
     * @return current weather data
     * @throws WeatherProviderException if communication fails or response is malformed
     */
    WeatherData getCurrentWeather(Location location) throws WeatherProviderException;
}
