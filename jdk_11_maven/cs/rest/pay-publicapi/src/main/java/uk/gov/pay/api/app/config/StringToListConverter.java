package uk.gov.pay.api.app.config;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StringToListConverter extends StdConverter<String, List<String>> {

    @Override
    public List<String> convert(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
