package uk.gov.pay.api.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DisputesSearchParams {

    private static final String PAGE = "page";
    private static final String DISPLAY_SIZE = "display_size";
    private static final String DEFAULT_PAGE = "1";
    private static final String DEFAULT_DISPLAY_SIZE = "500";
    private static final String FROM_DATE = "from_date";
    private static final String TO_DATE = "to_date";
    private static final String FROM_SETTLED_DATE = "from_settled_date";
    private static final String TO_SETTLED_DATE = "to_settled_date";
    private static final String STATUS = "status";

    private String fromDate;
    private String toDate;
    private String page;
    private String displaySize;
    private String fromSettledDate;
    private String toSettledDate;
    private String state;

    private DisputesSearchParams(Builder builder) {
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.page = builder.page;
        this.displaySize = builder.displaySize;
        this.fromSettledDate = builder.fromSettledDate;
        this.toSettledDate = builder.toSettledDate;
        this.state = builder.status;
    }

    public Map<String, String> getParamsAsMap() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(FROM_DATE, fromDate);
        params.put(TO_DATE, toDate);
        params.put(PAGE, Optional.ofNullable(page).orElse(DEFAULT_PAGE));
        params.put(DISPLAY_SIZE, Optional.ofNullable(displaySize).orElse(DEFAULT_DISPLAY_SIZE));
        params.put(FROM_SETTLED_DATE, fromSettledDate);
        params.put(TO_SETTLED_DATE, toSettledDate);
        params.put(STATUS, state);
        
        return params;
    }

    public String getPage() {
        return page;
    }

    public String getDisplaySize() {
        return displaySize;
    }

    public String getFromDate() {
        return fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public String getFromSettledDate() {
        return fromSettledDate;
    }

    public String getToSettledDate() {
        return toSettledDate;
    }

    public String getState() {
        return state;
    }

    public static class Builder {
        private String fromDate;
        private String toDate;
        private String page;
        private String displaySize;
        private String fromSettledDate;
        private String toSettledDate;
        private String status;

        public Builder() {
        }

        public DisputesSearchParams build() {
            return new DisputesSearchParams(this);
        }

        public Builder withFromDate(String fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder withToDate(String toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder withPage(String page) {
            this.page = page;
            return this;
        }

        public Builder withDisplaySize(String displaySize) {
            this.displaySize = displaySize;
            return this;
        }

        public Builder withFromSettledDate(String fromSettledDate) {
            this.fromSettledDate = fromSettledDate;
            return this;
        }

        public Builder withToSettledDate(String toSettledDate) {
            this.toSettledDate = toSettledDate;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }
    }
}
