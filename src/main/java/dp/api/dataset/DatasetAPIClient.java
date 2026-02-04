package dp.api.dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dp.api.dataset.exception.BadRequestException;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.exception.DatasetAPIResponseParseException;
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
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.core5.http.ParseException;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * HTTP client for the dataset API.
 */
public class DatasetAPIClient implements DatasetClient {

    private final URI datasetAPIURL;
    private final String datasetAPIAuthToken;
    private final String serviceAuthToken;

    private final CloseableHttpClient client;

    private static final String authTokenHeaderName = "Internal-token";
    private static final String serviceTokenHeaderName = "Authorization";
    private static final ObjectMapper json = new ObjectMapper();

    /**
     * Create a new instance of DatasetAPIClient
     *
     * @param datasetAPIURL       - The URL of the dataset API
     * @param datasetAPIAuthToken - The authentication token for the dataset API
     * @param client              - The HTTP client to use internally
     */
    public DatasetAPIClient(String datasetAPIURL,
                            String datasetAPIAuthToken,
                            String serviceAuthToken,
                            CloseableHttpClient client) throws URISyntaxException {

        this.datasetAPIURL = new URI(datasetAPIURL);
        this.datasetAPIAuthToken = datasetAPIAuthToken;
        this.client = client;
        this.serviceAuthToken = serviceAuthToken;
    }

    /**
     * Create a new instance of DatasetAPIClient
     *
     * @param datasetAPIURL       - The URL of the dataset API
     * @param datasetAPIAuthToken - The authentication token for the dataset API
     */
    public DatasetAPIClient(String datasetAPIURL, String datasetAPIAuthToken, String serviceAuthToken) throws URISyntaxException {

        this(datasetAPIURL, datasetAPIAuthToken, serviceAuthToken, createDefaultHttpClient());
    }

    private static CloseableHttpClient createDefaultHttpClient() {

        return HttpClients.custom()
                .setRetryStrategy(new RetryStrategy())
                .build();
    }

    /**
     * Get the instance for the given instance ID.
     *
     * @param instanceID
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public Instance getInstance(String instanceID) throws IOException, DatasetAPIException {

        validateInstanceID(instanceID);

        String path = "/instances/" + instanceID;
        URI uri = datasetAPIURL.resolve(path);

        HttpGet req = new HttpGet(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);

        try (CloseableHttpResponse resp = executeRequest(req)) {
            int statusCode = resp.getCode();

            switch (statusCode) {
                case HttpStatus.SC_OK:
                    return parseResponseBody(resp, Instance.class);
                case HttpStatus.SC_NOT_FOUND:
                    throw new InstanceNotFoundException(formatErrResponse(req, resp));
                default:
                    throw new UnexpectedResponseException(
                            formatErrResponse(req, resp), resp.getCode());
            }
        } catch (ParseException e) {
            throw new DatasetAPIResponseParseException("failed to parse response from dataset api");
        }
    }

    /**
     * Create a new dataset
     *
     * @param datasetID
     * @param dataset
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public Dataset createDataset(String datasetID, Dataset dataset) throws IOException, DatasetAPIException {

        validateDatasetID(datasetID);

        String path = "/datasets/" + datasetID;
        URI uri = datasetAPIURL.resolve(path);

        HttpPost req = new HttpPost(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);
        req.setHeader("Content-Type", "application/json");

        addBody(dataset, req);

        try (CloseableHttpResponse resp = executeRequest(req)) {

            int statusCode = resp.getCode();

            switch (statusCode) {
                case HttpStatus.SC_CREATED:
                    DatasetResponse datasetResponse = parseResponseBody(resp, DatasetResponse.class);
                    return datasetResponse.getNext();
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new UnauthorisedException();
                case HttpStatus.SC_FORBIDDEN:
                    throw new DatasetAlreadyExistsException();
                default:
                    throw new UnexpectedResponseException(
                            formatErrResponse(req, resp), resp.getCode());
            }
        } catch (ParseException e) {
            throw new DatasetAPIResponseParseException("failed to parse response from dataset api");
        }
    }

    /**
     * Get the dataset for the given dataset ID.
     *
     * @param datasetID
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public Dataset getDataset(String datasetID) throws IOException, DatasetAPIException {

        validateDatasetID(datasetID);

        String path = "/datasets/" + datasetID;
        URI uri = datasetAPIURL.resolve(path);

        HttpGet req = new HttpGet(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);

        try (CloseableHttpResponse resp = executeRequest(req)) {
            validate200ResponseCode(req, resp);
            DatasetResponse datasetResponse = parseResponseBody(resp, DatasetResponse.class);
            return datasetResponse.getNext();
        } catch (ParseException e) {
            throw new DatasetAPIResponseParseException("failed to parse response from dataset api");
        }
    }

    /**
     * Delete the dataset for the given dataset ID.
     *
     * @param datasetID
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public void deleteDataset(String datasetID) throws IOException, DatasetAPIException {

        validateDatasetID(datasetID);

        String path = "/datasets/" + datasetID;
        URI uri = datasetAPIURL.resolve(path);

        HttpDelete req = new HttpDelete(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);

        try (CloseableHttpResponse resp = executeRequest(req)) {
            int statusCode = resp.getCode();

            switch (statusCode) {
                case HttpStatus.SC_NO_CONTENT:
                    return;
                default:
                    validate200ResponseCode(req, resp);
            }
        }
    }

    /**
     * Detach the given version for the given edition for given dataset ID.
     *
     * @param datasetID
     * @param edition
     * @param version
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public void detachVersion(String datasetID, String edition, String version) throws IOException, DatasetAPIException {

        validateDatasetID(datasetID);
        validateEdition(edition);
        validateVersion(version);

        String path = String.format("/datasets/%s/editions/%s/versions/%s", datasetID, edition, version);
        URI uri = datasetAPIURL.resolve(path);

        HttpDelete req = new HttpDelete(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);

        try (CloseableHttpResponse resp = executeRequest(req)) {
            validate200ResponseCode(req, resp);
        }
    }


    /**
     * Update the dataset for the given dataset ID with the given dataset instance data.
     *
     * @param datasetID
     * @param dataset
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public void updateDataset(String datasetID, Dataset dataset) throws IOException, DatasetAPIException {

        validateDatasetID(datasetID);

        String path = "/datasets/" + datasetID;
        URI uri = datasetAPIURL.resolve(path);

        HttpPut req = new HttpPut(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);
        req.setHeader("Content-Type", "application/json");

        addBody(dataset, req);

        try (CloseableHttpResponse resp = executeRequest(req)) {

            int statusCode = resp.getCode();

            switch (statusCode) {
                case HttpStatus.SC_OK:
                    return;
                case HttpStatus.SC_NOT_FOUND:
                    throw new DatasetNotFoundException(formatErrResponse(req, resp));
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new UnauthorisedException();
                case HttpStatus.SC_BAD_REQUEST:
                    throw new BadRequestException("invalid dataset request");
                default:
                    throw new UnexpectedResponseException(
                            formatErrResponse(req, resp), resp.getCode());
            }
        }
    }

    /**
     * Get a particular version of a dataset.
     *
     * @param datasetID
     * @param edition
     * @param version
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public DatasetVersion getDatasetVersion(String datasetID, String edition, String version) throws IOException, DatasetAPIException {

        validateDatasetID(datasetID);
        validateEdition(edition);
        validateVersion(version);

        String path = String.format("/datasets/%s/editions/%s/versions/%s", datasetID, edition, version);
        URI uri = datasetAPIURL.resolve(path);

        HttpGet req = new HttpGet(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);

        try (CloseableHttpResponse resp = executeRequest(req)) {
            validate200ResponseCode(req, resp);
            return parseResponseBody(resp, DatasetVersion.class);
        }catch (ParseException e) {
            throw new DatasetAPIResponseParseException("failed to parse response from dataset api");
        }
    }

    /**
     * Update the dataset version
     *
     * @param datasetID
     * @param edition
     * @param version
     * @param datasetVersion
     * @return
     * @throws IOException
     * @throws DatasetAPIException
     */
    @Override
    public void updateDatasetVersion(String datasetID, String edition, String version, DatasetVersion datasetVersion) throws IOException, DatasetAPIException {

        validateDatasetID(datasetID);
        validateEdition(edition);
        validateVersion(version);

        String path = String.format("/datasets/%s/editions/%s/versions/%s", datasetID, edition, version);
        URI uri = datasetAPIURL.resolve(path);

        HttpPut req = new HttpPut(uri);
        req.addHeader(authTokenHeaderName, datasetAPIAuthToken);
        req.addHeader(serviceTokenHeaderName, serviceAuthToken);
        req.setHeader("Content-Type", "application/json");

        addBody(datasetVersion, req);

        try (CloseableHttpResponse resp = executeRequest(req)) {
            validate200ResponseCode(req, resp);
        }
    }

    private void validate200ResponseCode(HttpUriRequestBase httpRequest, CloseableHttpResponse response)
            throws DatasetNotFoundException, UnexpectedResponseException, UnauthorisedException, ForbiddenException {
        switch (response.getCode()) {
            case HttpStatus.SC_OK:
                return;
            case HttpStatus.SC_FORBIDDEN:
                throw new ForbiddenException();
            case HttpStatus.SC_NOT_FOUND:
                throw new DatasetNotFoundException(formatErrResponse(httpRequest, response));
            case HttpStatus.SC_UNAUTHORIZED:
                throw new UnauthorisedException();
            default:
                throw new UnexpectedResponseException(
                        formatErrResponse(httpRequest, response), response.getCode());
        }
    }

    private void addBody(Object object, HttpUriRequestBase httpRequest) throws JsonProcessingException {

        String body = json.writeValueAsString(object);
        StringEntity stringEntity = new StringEntity(body);
        httpRequest.setEntity(stringEntity);
    }

    private void validateDatasetID(String datasetID) {
        Args.check(isNotEmpty(datasetID), "a dataset id must be provided.");
    }

    private void validateEdition(String edition) {
        Args.check(isNotEmpty(edition), "an edition must be provided.");
    }

    private void validateVersion(String version) {
        Args.check(isNotEmpty(version), "a version must be provided.");
    }

    private void validateInstanceID(String instanceID) {
        Args.check(isNotEmpty(instanceID), "a instance id must be provided.");
    }

    private static boolean isNotEmpty(String str) {
        return str != null && str.length() > 0;
    }

    private <T> T parseResponseBody(CloseableHttpResponse response, Class<T> type) throws IOException, ParseException  {
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return json.readValue(responseString, type);
    }

    private String formatErrResponse(HttpUriRequestBase httpRequest, CloseableHttpResponse response) {
        int responseCode = response.getCode();

        try {
            String requestURI = httpRequest.getUri().toString();
            return String.format("the dataset api returned a %s response for %s",
                            responseCode,
                            requestURI);
        } catch (URISyntaxException e) {
            return String.format("the dataset api returned a %s response for %s",
                responseCode,
                httpRequest.getRequestUri());
        }
    }

    private CloseableHttpResponse executeRequest(HttpUriRequest req) throws IOException {
        info().beginHTTP(req).log("executing dataset-api request");
        CloseableHttpResponse resp = client.execute(req);
        info().endHTTP(req, resp).log("execute dataset-api request compeleted");
        return resp;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
