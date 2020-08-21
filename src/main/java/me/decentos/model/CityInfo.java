package me.decentos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CityInfo {

    @JsonProperty(value = "code")
    private String code;

    @JsonProperty(value = "name")
    private String name;
}
