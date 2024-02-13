package uk.gov.pay.api.utils;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class JsonStringBuilderTest {
    @Test
    public void testObjectToJsonString() {
        String message = "There was an error";
        String code = "#the code of error!";

        String result = new JsonStringBuilder()
                .addRoot("error")
                .add("message", message)
                .add("type", "card_error")
                .add("param", "number")
                .add("code", code)
                .noPrettyPrint()
                .build();

        assertEquals("{\"error\":{\"message\":\"There was an error\",\"type\":\"card_error\",\"param\":\"number\",\"code\":\"#the code of error!\"}}", result);
    }

    @Test
    public void nullValues() {
        String message = "There was an error";

        String result = new JsonStringBuilder()
                .addRoot("error")
                .add("message", message)
                .add("type", "card_error")
                .add("param", "number")
                .add("code", null)
                .noPrettyPrint()
                .build();

        assertEquals("{\"error\":{\"message\":\"There was an error\",\"type\":\"card_error\",\"param\":\"number\"}}", result);
    }

    @Test
    public void nestedMaps() {
        String message = "There was an error";

        String result = new JsonStringBuilder()
                .addRoot("error")
                .add("message", message)
                .add("type", "card_error")
                .add("param", "number")
                .addToMap("metadata", "orderid", "our-order-id")
                .addToMap("empty")
                .noPrettyPrint()
                .build();

        assertEquals("{\"error\":{\"message\":\"There was an error\",\"type\":\"card_error\",\"param\":\"number\",\"metadata\":{\"orderid\":\"our-order-id\"},\"empty\":{}}}", result);
    }

    @Test
    public void nestedMapsWithMapKeyVarArgs() {
        String message = "There was an error";

        String result = new JsonStringBuilder()
                .addRoot("error")
                .add("message", message)
                .add("type", "card_error")
                .add("param", "number")
                .addToMap("metadata", "orderid", "our-order-id")
                .addToNestedMap("error_meta", "meta data of error", "metadata", "meta_error")
                .noPrettyPrint()
                .build();

        assertEquals("{\"error\":{\"message\":\"There was an error\",\"type\":\"card_error\",\"param\":\"number\",\"metadata\":{\"orderid\":\"our-order-id\",\"meta_error\":{\"error_meta\":\"meta data of error\"}}}}", result);
    }
}
