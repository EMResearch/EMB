package uk.gov.pay.api.app.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StringToListConverterTest {

    private StringToListConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new StringToListConverter();
    }

    @ParameterizedTest
    @MethodSource("parametersForConvertsStringInputToListOfStrings")
    public void convertsStringInputToListOfStrings(String input, List<String> expectedOutput) {
        assertThat(converter.convert(input), is(expectedOutput));
    }

    static Object[] parametersForConvertsStringInputToListOfStrings() {
        return new Object[]{
                new Object[]{null, Collections.emptyList()},
                new Object[]{"", Collections.emptyList()},
                new Object[]{", ,   ,", Collections.emptyList()},
                new Object[]{"a", List.of("a")},
                new Object[]{"a, b, b", List.of("a", "b", "b")},
                new Object[]{"a, , b", List.of("a", "b")}
        };
    }
}
