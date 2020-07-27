package me.decentos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchDto {
    private String cityFrom;
    private String cityFromCode;
    private String cityTo;
    private String cityToCode;
}
