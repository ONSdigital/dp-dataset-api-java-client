package dp.api.dataset.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties
public class DatasetVersion {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String edition;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String version;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String release_date;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private State state;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String collection_id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DatasetLinks links;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRelease_date() {
        return release_date;
    }

    public void setRelease_date(String release_date) {
        this.release_date = release_date;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getCollection_id() {
        return collection_id;
    }

    public void setCollection_id(String collection_id) {
        this.collection_id = collection_id;
    }

    public DatasetLinks getLinks() {
        return links;
    }

    public void setLinks(DatasetLinks links) {
        this.links = links;
    }
}
