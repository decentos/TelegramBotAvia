package me.decentos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchTicketResult {

    @JsonProperty(value = "success")
    private boolean success;

    @JsonProperty(value = "data")
    private Object data;

    @JsonProperty(value = "error")
    private boolean error;

    @JsonProperty(value = "currency")
    private String currency;
}
