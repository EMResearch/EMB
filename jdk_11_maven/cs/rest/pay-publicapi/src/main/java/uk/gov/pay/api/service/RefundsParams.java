package uk.gov.pay.api.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class RefundsParams {

    private static final String PAGE = "page";
    private static final String DISPLAY_SIZE = "display_size";
    private static final String DEFAULT_PAGE = "1";
    private static final String DEFAULT_DISPLAY_SIZE = "500";
    private static final String FROM_DATE = "from_date";
    private static final String TO_DATE = "to_date";
    private static final String FROM_SETTLED_DATE = "from_settled_date";
    private static final String TO_SETTLED_DATE = "to_settled_date";
    
    private String fromDate;
    private String toDate;
    private String page;
    private String displaySize;
    private String fromSettledDate;
    private String toSettledDate;

    public RefundsParams(String fromDate, String toDate, String page,
                         String displaySize, String fromSettledDate, String toSettledDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.page = page;
        this.displaySize = displaySize;
        this.fromSettledDate = fromSettledDate;
        this.toSettledDate = toSettledDate;
    }

    public Map<String, String> getParamsAsMap() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(FROM_DATE, fromDate);
        params.put(TO_DATE, toDate);
        params.put(PAGE, Optional.ofNullable(page).orElse(DEFAULT_PAGE));
        params.put(DISPLAY_SIZE, Optional.ofNullable(displaySize).orElse(DEFAULT_DISPLAY_SIZE));
        params.put(FROM_SETTLED_DATE, fromSettledDate);
        params.put(TO_SETTLED_DATE, toSettledDate);
        
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
}
