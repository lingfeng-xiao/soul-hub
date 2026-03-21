package com.openclaw.digitalbeings.interfaces.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.PrintWriter;

public final class CliJsonSupport {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private CliJsonSupport() {
    }

    public static void printJson(PrintWriter out, Object data) {
        try {
            out.println(JSON_MAPPER.writeValueAsString(data));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize CLI output as JSON.", exception);
        }
    }
}
