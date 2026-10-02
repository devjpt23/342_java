package weather.provider;

/**
 * Exception thrown when a weather data provider encounters a failure,
 * such as a network error, HTTP error status, or malformed API response.
 */
public class WeatherProviderException extends RuntimeException {

    public WeatherProviderException(String message) {
        super(message);
    }

    public WeatherProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
