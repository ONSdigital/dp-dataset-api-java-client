package dp.api.dataset;

import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;

/**
 * Custom implementation of ServiceUnavailableRetryStrategy to retry any HTTP 5xx responses.
 */
public class RetryStrategy implements HttpRequestRetryStrategy {

    private final int maxRetries;
    private final long retryIntervalMs;

    public RetryStrategy(int maxRetries, long retryIntervalMs) {
        this.maxRetries = maxRetries;
        this.retryIntervalMs = retryIntervalMs;
    }

    public RetryStrategy() {
        this(3, 20);
    }

    @Override
    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        return executionCount <= maxRetries &&
                response.getCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    public boolean retryRequest(HttpRequest request, IOException exception, int executionCount, HttpContext context) {
        return executionCount <= maxRetries;
    }

    @Override
    public TimeValue getRetryInterval(HttpResponse response, int executionCount, HttpContext context) {
        return TimeValue.ofMilliseconds(retryIntervalMs);
    }
}
