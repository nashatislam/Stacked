package application;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDate;

/** Gson adapter to avoid JPMS reflection access errors for java.time.LocalDate. */
public final class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    @Override
    public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString()); // ISO-8601
    }

    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context)
            throws JsonParseException {
        return LocalDate.parse(json.getAsString());
    }
}