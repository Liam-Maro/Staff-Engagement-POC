package com.staffengagement.portfolio.dto;

import com.staffengagement.portfolio.validation.ValidUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank @Size(max = 2048) @ValidUrl String url,
        @NotBlank @Size(max = 100) String label
) {}
