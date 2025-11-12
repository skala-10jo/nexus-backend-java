package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GlossaryTermRequest {

    @NotBlank(message = "Korean term is required")
    private String koreanTerm;

    private String englishTerm;

    private String vietnameseTerm;

    private String abbreviation;

    @NotBlank(message = "Definition is required")
    private String definition;

    private String context;

    private String exampleSentence;

    private String note;

    private String domain;
}
