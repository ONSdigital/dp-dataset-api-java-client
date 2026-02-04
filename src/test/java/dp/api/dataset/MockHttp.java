package dp.api.dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockHttp {

    private static final ObjectMapper json = new ObjectMapper();


    public static CloseableHttpResponse response(int httpStatus) {

        CloseableHttpResponse mockHttpResponse = mock(CloseableHttpResponse.class);
        when(mockHttpResponse.getCode()).thenReturn(httpStatus);

        return mockHttpResponse;
    }

    public static void responseBody(CloseableHttpResponse mockHttpResponse, Object responseBody) throws JsonProcessingException {
        String responseJSON = json.writeValueAsString(responseBody);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity(responseJSON));
    }
}
