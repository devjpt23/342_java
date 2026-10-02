package weather;

import weather.model.Location;
import weather.model.WeatherData;
import weather.provider.WeatherDataProvider;
import weather.provider.WeatherProviderException;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlled test implementation of WeatherDataProvider for automated unit testing.
 * Isolates tests from the real external API and network connection.
 */
public class TestWeatherDataProvider implements WeatherDataProvider {

    private final Map<String, WeatherData> cannedData = new HashMap<>();
    private boolean shouldFail = false;
    private String failureMessage = "Simulated provider network outage";

    public TestWeatherDataProvider() {
        // Seed default controlled test data
        cannedData.put("Chicago", new WeatherData("Chicago", 65.0, 55.0, 12.0, 63.0, "Partly cloudy"));
        cannedData.put("Los Angeles", new WeatherData("Los Angeles", 78.0, 40.0, 5.0, 78.0, "Clear sky"));
        cannedData.put("New York", new WeatherData("New York", 58.0, 70.0, 8.0, 55.0, "Rain"));
    }

    public void setWeatherData(String city, WeatherData data) {
        cannedData.put(city, data);
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    @Override
    public WeatherData getCurrentWeather(Location location) throws WeatherProviderException {
        if (shouldFail) {
            throw new WeatherProviderException(failureMessage);
        }

        WeatherData data = cannedData.get(location.city());
        if (data == null) {
            // Default mock data if location was added dynamically
            return new WeatherData(location.city(), 50.0, 50.0, 10.0, 50.0, "Mainly clear");
        }
        return data;
    }
}
