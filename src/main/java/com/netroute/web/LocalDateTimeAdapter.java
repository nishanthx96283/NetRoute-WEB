package com.netroute.web;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom Gson {@link TypeAdapter} for {@link LocalDateTime}.
 * Serializes {@link LocalDateTime} to an ISO-8601 string and deserializes
 * it back using {@link LocalDateTime#parse}.
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    private final DateTimeFormatter formatter;

    public LocalDateTimeAdapter() {
        this(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public LocalDateTimeAdapter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.format(formatter));
        }
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String text = in.nextString();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            // Support strings formatted with space instead of 'T' (e.g., standard SQL timestamps)
            if (text.contains(" ")) {
                return LocalDateTime.parse(text.replace(" ", "T"));
            }
            throw e;
        }
    }
}
