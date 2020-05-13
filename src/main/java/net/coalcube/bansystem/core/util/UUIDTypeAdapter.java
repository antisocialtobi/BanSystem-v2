package net.coalcube.bansystem.core.util;

import java.io.IOException;
import java.util.UUID;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class UUIDTypeAdapter extends TypeAdapter<UUID> {

    public static UUID fromString(String input) {
        return UUID.fromString(input.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    public static String fromUUID(UUID value) {
        return value.toString().replace("-", "");
    }

    @Override
    public UUID read(JsonReader in) throws IOException {
        return UUIDTypeAdapter.fromString(in.nextString());
    }

    @Override
    public void write(JsonWriter out, UUID value) throws IOException {
        out.value(UUIDTypeAdapter.fromUUID(value));
    }

}