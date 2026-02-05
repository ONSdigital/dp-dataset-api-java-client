package dp.api.dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dp.api.dataset.exception.BadRequestException;
import dp.api.dataset.exception.DatasetAlreadyExistsException;
import dp.api.dataset.exception.DatasetNotFoundException;
import dp.api.dataset.exception.ForbiddenException;
import dp.api.dataset.exception.InstanceNotFoundException;
import dp.api.dataset.exception.UnauthorisedException;
import dp.api.dataset.exception.UnexpectedResponseException;
import dp.api.dataset.model.Dataset;
import dp.api.dataset.model.DatasetResponse;
import dp.api.dataset.model.DatasetVersion;
import dp.api.dataset.model.Instance;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatasetAPIClientTest {

    private static final ObjectMapper json = new ObjectMapper();

    private static final String authTokenHeaderName = "Internal-token";
    private static final String serviceTokenHeaderName = "Authorization";

    private static final String datasetAPIURL = "";
    private static final String datasetAPIAuthToken = "12345";
    private static final String serviceAuthToken = "67856";
    private static final String instanceID = "123";
    private static final String datasetID = "321";
    private static final String datasetTitle = "the dataset title";
    private static final String edition = "current";
    private static final String version = "1";

    @Test
    public void testDatasetAPI_invalidURI() {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);

        // Given an invalid URI
        String invalidURI = "{{}}";

        // When a new DatasetAPIClient is created
        // Then the expected exception is thrown
        assertThrows(URISyntaxException.class,
                () -> new DatasetAPIClient(invalidURI, datasetAPIAuthToken, serviceAuthToken, mockHttpClient));
    }

    @Test
    public void testDatasetAPI_createDataset() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a new dataset
        Dataset dataset = createDataset();

        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_CREATED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        DatasetResponse mockDatasetResponse = mockDatasetResponse(mockHttpResponse);
        Dataset expectedDataset = mockDatasetResponse.getNext();

        // When createDataset is called
        Dataset actualDataset = datasetAPIClient.createDataset(datasetID, dataset);

        HttpUriRequestBase httpRequest = captureHttpRequestWithBody(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);

        // Then the request should contain the provided dataset in the body
        String body = IOUtils.toString(httpRequest.getEntity().getContent());
        Dataset requestDataset = json.readValue(body, Dataset.class);

        assertEquals(datasetID, requestDataset.getId());
        assertEquals(datasetTitle, requestDataset.getTitle());

        // Then the response should be whats returned from the dataset API
        assertNotNull(actualDataset);
        assertEquals(expectedDataset.getId(), actualDataset.getId());
        assertEquals(expectedDataset.getTitle(), actualDataset.getTitle());
    }

    @Test
    public void testDatasetAPI_createDataset_unknownResponseFields() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a mock dataset API response that has unknown fields (fields that are not defined in the model)

        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_CREATED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = createDataset();

        String responseJSON = "{\"id\":\"123\",\"unknown_field\":\"543\",\"next\":{ \"id\":\"123\" }}";
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity(responseJSON));

        // When createDataset is called
        Dataset actualDataset = datasetAPIClient.createDataset(datasetID, dataset);

        // Then the response should be whats returned from the dataset API
        assertNotNull(actualDataset);
        assertEquals("123", actualDataset.getId());
    }

    @Test
    public void testDatasetAPI_createDataset_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = new Dataset();

        // When createDataset is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.createDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_createDataset_unauthorised() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 401
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_UNAUTHORIZED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = new Dataset();

        // When createDataset is called
        // Then the expected exception is thrown
        assertThrows(UnauthorisedException.class,
                () -> datasetAPIClient.createDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_createDataset_forbidden() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 403
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_FORBIDDEN);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = new Dataset();

        // When createDataset is called
        // Then the expected exception is thrown
        assertThrows(DatasetAlreadyExistsException.class,
                () -> datasetAPIClient.createDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_createDataset_emptyDatasetID() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty dataset ID
        String datasetID = "";
        Dataset dataset = new Dataset();

        // When createDataset is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.createDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_getDataset() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a mock dataset response from the dataset API
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_OK);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        DatasetResponse mockDatasetResponse = mockDatasetResponse(mockHttpResponse);
        Dataset expectedDataset = mockDatasetResponse.getNext();

        // When getDataset is called
        Dataset actualDataset = datasetAPIClient.getDataset(datasetID);

        assertNotNull(actualDataset);

        HttpUriRequestBase httpRequest = captureHttpRequest(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);

        // Then the response should be whats returned from the dataset API
        assertEquals(expectedDataset.getId(), actualDataset.getId());
        assertEquals(expectedDataset.getTitle(), actualDataset.getTitle());
    }

    @Test
    public void testDatasetAPI_getDataset_datasetNotFound() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 404
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_NOT_FOUND);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getDataset is called
        // Then the expected exception is thrown
        assertThrows(DatasetNotFoundException.class,
                () -> datasetAPIClient.getDataset(datasetID));
    }

    @Test
    public void testDatasetAPI_getDataset_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getDataset is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.getDataset(datasetID));
    }

    @Test
    public void testDatasetAPI_getDataset_unauthorised() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 401
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_UNAUTHORIZED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getDataset is called
        // Then the expected exception is thrown
        assertThrows(UnauthorisedException.class,
                () -> datasetAPIClient.getDataset(datasetID));
    }

    @Test
    public void testDatasetAPI_getDataset_emptyDatasetID() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty dataset ID
        String datasetID = "";

        // When updateDataset is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.getDataset(datasetID));
    }

    @Test
    public void testDatasetAPI_updateDataset() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an updated dataset
        Dataset dataset = createDataset();

        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_OK);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When updateDataset is called
        datasetAPIClient.updateDataset(datasetID, dataset);

        HttpUriRequestBase httpRequest = captureHttpRequestWithBody(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);

        // Then the request should contain the provided dataset in the body
        String body = IOUtils.toString(httpRequest.getEntity().getContent());
        Dataset requestDataset = json.readValue(body, Dataset.class);

        assertEquals(dataset.getId(), requestDataset.getId());
        assertEquals(dataset.getTitle(), requestDataset.getTitle());
    }

    @Test
    public void testDatasetAPI_updateDataset_datasetNotFound() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 404
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_NOT_FOUND);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = new Dataset();

        // When updateDataset is called
        // Then the expected exception is thrown
        assertThrows(DatasetNotFoundException.class,
                () -> datasetAPIClient.updateDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_updateDataset_unauthorised() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 401
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_UNAUTHORIZED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = new Dataset();

        // When updateDataset is called
        // Then the expected exception is thrown
        assertThrows(UnauthorisedException.class,
                () -> datasetAPIClient.updateDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_updateDataset_badRequest() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 400
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_BAD_REQUEST);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = new Dataset();

        // When updateDataset is called
        // Then the expected exception is thrown
        assertThrows(BadRequestException.class,
                () -> datasetAPIClient.updateDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_updateDataset_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Dataset dataset = new Dataset();

        // When updateDataset is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.updateDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_updateDataset_emptyDatasetID() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty dataset ID
        String datasetID = "";
        Dataset dataset = new Dataset();

        // When updateDataset is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.updateDataset(datasetID, dataset));
    }

    @Test
    public void testDatasetAPI_getInstance() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a mock dataset response from the dataset API
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_OK);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        Instance expectedInstance = mockInstanceResponse(mockHttpResponse);

        // When getInstance is called
        Instance actualInstance = datasetAPIClient.getInstance(datasetID);

        assertNotNull(actualInstance);

        HttpUriRequestBase httpRequest = captureHttpRequest(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);

        // Then the response should be whats returned from the dataset API
        assertEquals(expectedInstance.getId(), actualInstance.getId());
    }

    @Test
    public void testDatasetAPI_getInstance_instanceNotFound() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 404
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_NOT_FOUND);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getInstance is called
        // Then the expected exception is thrown
        assertThrows(InstanceNotFoundException.class,
                () -> datasetAPIClient.getInstance(instanceID));
    }

    @Test
    public void testDatasetAPI_getInstance_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getInstance is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.getInstance(instanceID));
    }

    @Test
    public void testDatasetAPI_getInstance_emptyInstanceID() throws URISyntaxException {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty instance ID
        String instanceID = "";

        // When getInstance is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.getInstance(instanceID));
    }

    @Test
    public void testDatasetAPI_getDatasetVersion() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a mock dataset response from the dataset API
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_OK);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        DatasetVersion expectedVersion = mockVersionResponse(mockHttpResponse);

        // When getDatasetVersion is called
        DatasetVersion actualDatasetVersion = datasetAPIClient.getDatasetVersion(datasetID, edition, version);

        assertNotNull(actualDatasetVersion);

        HttpUriRequestBase httpRequest = captureHttpRequest(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);

        // Then the response should be whats returned from the dataset API
        assertEquals(expectedVersion.getId(), actualDatasetVersion.getId());
    }

    @Test
    public void testDatasetAPI_getDatasetVersion_datasetNotFound() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 404
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_NOT_FOUND);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(DatasetNotFoundException.class,
                () -> datasetAPIClient.getDatasetVersion(datasetID, edition, version));
    }

    @Test
    public void testDatasetAPI_getDatasetVersion_unauthorised() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 401
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_UNAUTHORIZED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(UnauthorisedException.class,
                () -> datasetAPIClient.getDatasetVersion(datasetID, edition, version));
    }

    @Test
    public void testDatasetAPI_getDatasetVersion_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When getDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.getDatasetVersion(datasetID, edition, version));
    }

    @Test
    public void testDatasetAPI_getDatasetVersion_emptyDatasetID() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty dataset ID
        String datasetID = "";

        // When getDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.getDatasetVersion(datasetID, edition, version));
    }

    @Test
    public void testDatasetAPI_getDatasetVersion_emptyEdition() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty edition
        String edition = "";

        // When getDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.getDatasetVersion(datasetID, edition, version));
    }

    @Test
    public void testDatasetAPI_updateDatasetVersion() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an updated dataset
        DatasetVersion datasetVersion = createDatasetVersion();

        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_OK);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When updateDatasetVersion is called
        datasetAPIClient.updateDatasetVersion(datasetID, edition, version, datasetVersion);

        HttpUriRequestBase httpRequest = captureHttpRequestWithBody(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);

        // Then the request should contain the provided dataset version in the body
        String body = IOUtils.toString(httpRequest.getEntity().getContent());
        DatasetVersion requestDatasetVersion = json.readValue(body, DatasetVersion.class);

        assertEquals(datasetVersion.getId(), requestDatasetVersion.getId());
    }

    @Test
    public void testDatasetAPI_updateDatasetVersion_datasetNotFound() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 404
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_NOT_FOUND);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        DatasetVersion datasetVersion = createDatasetVersion();

        // When updateDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(DatasetNotFoundException.class,
                () -> datasetAPIClient.updateDatasetVersion(datasetID, edition, version, datasetVersion));
    }

    @Test
    public void testDatasetAPI_updateDatasetVersion_unauthorised() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 401
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_UNAUTHORIZED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        DatasetVersion datasetVersion = createDatasetVersion();

        // When getDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(UnauthorisedException.class,
                () -> datasetAPIClient.updateDatasetVersion(datasetID, edition, version, datasetVersion));
    }

    @Test
    public void testDatasetAPI_updateDatasetVersion_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        DatasetVersion datasetVersion = createDatasetVersion();

        // When updateDatasetVersion is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.updateDatasetVersion(datasetID, edition, version, datasetVersion));
    }

    @Test
    public void testDatasetAPI_detachVersion() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a mock dataset response from the dataset API
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_OK);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When detachVersion is called
        datasetAPIClient.detachVersion(datasetID, version, edition);

        HttpUriRequestBase httpRequest = captureHttpRequest(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);
    }

    @Test
    public void testDatasetAPI_detachVersion_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When detachVersion is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.detachVersion(datasetID, version, edition));
    }

    @Test
    public void testDatasetAPI_detachVersion_unauthorised() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 401
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_UNAUTHORIZED);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When detachVersion is called
        // Then the expected exception is thrown
        assertThrows(UnauthorisedException.class,
                () -> datasetAPIClient.detachVersion(datasetID, version, edition));
    }

    @Test
    public void testDatasetAPI_detachVersion_forbidden() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 403
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_FORBIDDEN);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When detachVersion is called
        // Then the expected exception is thrown
        assertThrows(ForbiddenException.class,
                () -> datasetAPIClient.detachVersion(datasetID, version, edition));
    }

    @Test
    public void testDatasetAPI_detachVersion_emptyDatasetID() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty dataset ID
        String datasetID = "";

        // When detachVersion is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.detachVersion(datasetID, version, edition));
    }

    @Test
    public void testDatasetAPI_detachVersion_emptyVersion() throws URISyntaxException {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty version
        String version = "";

        // When detachVersion is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.detachVersion(datasetID, version, edition));
    }

    @Test
    public void testDatasetAPI_detachVersion_emptyEdition() throws URISyntaxException {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty edition
        String edition = "";

        // When detachVersion is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.detachVersion(datasetID, version, edition));
    }

    @Test
    public void testDatasetAPI_deleteDataset() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a mock dataset response from the dataset API
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_NO_CONTENT);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When deleteDataset is called
        datasetAPIClient.deleteDataset(datasetID);

        HttpUriRequestBase httpRequest = captureHttpRequest(mockHttpClient);

        // Then the request should contain the authentication header
        String actualAuthToken = httpRequest.getFirstHeader(authTokenHeaderName).getValue();
        assertEquals(datasetAPIAuthToken, actualAuthToken);

        // Then the request should contain the service token header
        String actualServiceToken = httpRequest.getFirstHeader(serviceTokenHeaderName).getValue();
        assertEquals(serviceAuthToken, actualServiceToken);
    }

    @Test
    public void testDatasetAPI_deleteDataset_internalError() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 500
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When deleteDataset is called
        // Then the expected exception is thrown
        assertThrows(UnexpectedResponseException.class,
                () -> datasetAPIClient.deleteDataset(datasetID));
    }

    @Test
    public void testDatasetAPI_deleteDataset_emptyDatasetID() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given an empty dataset ID
        String datasetID = "";

        // When updateDataset is called
        // Then the expected exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> datasetAPIClient.deleteDataset(datasetID));
    }

    @Test
    public void testDatasetAPI_deleteDataset_forbidden() throws Exception {

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // Given a request to the dataset API that returns a 403
        CloseableHttpResponse mockHttpResponse = MockHttp.response(HttpStatus.SC_FORBIDDEN);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class))).thenReturn(mockHttpResponse);

        // When deleteDataset is called
        // Then the expected exception is thrown
        assertThrows(ForbiddenException.class,
                () -> datasetAPIClient.deleteDataset(datasetID));
    }

    @Test
    public void testDatasetAPI_close() throws Exception {

        // Given a dataset API client implements closable
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        DatasetClient datasetAPIClient = getDatasetClient(mockHttpClient);

        // When close is called
        datasetAPIClient.close();

        // Then close is called on the underlying httpClient
        verify(mockHttpClient, times(1)).close();
    }

    private DatasetClient getDatasetClient(CloseableHttpClient mockHttpClient) throws URISyntaxException {
        return new DatasetAPIClient(datasetAPIURL, datasetAPIAuthToken, serviceAuthToken, mockHttpClient);
    }

    private DatasetResponse mockDatasetResponse(CloseableHttpResponse mockHttpResponse) throws JsonProcessingException, UnsupportedEncodingException {
        DatasetResponse responseBody = new DatasetResponse();
        responseBody.setId(datasetID);
        responseBody.setNext(new Dataset());

        MockHttp.responseBody(mockHttpResponse, responseBody);

        return responseBody;
    }

    private Instance mockInstanceResponse(CloseableHttpResponse mockHttpResponse) throws JsonProcessingException, UnsupportedEncodingException {
        Instance instance = new Instance();
        instance.setId(instanceID);

        MockHttp.responseBody(mockHttpResponse, instance);

        return instance;
    }

    private HttpUriRequestBase captureHttpRequestWithBody(CloseableHttpClient mockHttpClient) throws IOException {
        ArgumentCaptor<HttpUriRequestBase> requestCaptor = ArgumentCaptor.forClass(HttpUriRequestBase.class);
        verify(mockHttpClient).execute(requestCaptor.capture());
        return requestCaptor.getValue();
    }

    private HttpUriRequestBase captureHttpRequest(CloseableHttpClient mockHttpClient) throws IOException {
        ArgumentCaptor<HttpUriRequestBase> requestCaptor = ArgumentCaptor.forClass(HttpUriRequestBase.class);
        verify(mockHttpClient).execute(requestCaptor.capture());
        return requestCaptor.getValue();
    }

    private Dataset createDataset() {
        Dataset dataset = new Dataset();
        dataset.setId(datasetID);
        dataset.setTitle(datasetTitle);
        return dataset;
    }

    private DatasetVersion createDatasetVersion() {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersion(version);
        return datasetVersion;
    }

    private DatasetVersion mockVersionResponse(CloseableHttpResponse mockHttpResponse) throws UnsupportedEncodingException, JsonProcessingException {
        DatasetVersion datasetVersion = createDatasetVersion();
        MockHttp.responseBody(mockHttpResponse, datasetVersion);

        return datasetVersion;
    }
}
