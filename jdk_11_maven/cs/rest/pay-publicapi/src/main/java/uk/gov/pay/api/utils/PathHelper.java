package uk.gov.pay.api.utils;

import org.apache.commons.lang3.StringUtils;

public class PathHelper {

    public static String getPathType(String pathValue, String method) {
        String path = StringUtils.removeEnd(pathValue, "/");

        if (path.endsWith("/capture")) {
            return "capture_payment";
        }

        if (path.endsWith("/payments") && method.equals("POST")) {
            return "create_payment";
        }

        return "";
    }
}
