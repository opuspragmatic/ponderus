package io.pragmatic.ponderus.dto;

import java.util.UUID;

import io.pragmatic.ponderus.domain.Option;

public record OptionResponse(
    UUID id,
    UUID projectId,
    String name,
    Integer position
) {
    public static OptionResponse from(Option option){
        return new OptionResponse(
            option.getId(), 
            option.getProject().getId(), 
            option.getName(), 
            option.getPosition()
        );
    }
}
