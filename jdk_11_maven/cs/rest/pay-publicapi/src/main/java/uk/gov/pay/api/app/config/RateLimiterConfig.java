package uk.gov.pay.api.app.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RateLimiterConfig extends Configuration {

    @Min(1)
    private int noOfReq;

    @Min(1)
    private int noOfReqForPost;

    @Min(1)
    private int noOfReqForElevatedAccounts;

    @Min(1)
    private int noOfPostReqForElevatedAccounts;

    @Min(1)
    private int noOfReqPerNode;

    @Min(1)
    private int noOfReqForPostPerNode;

    @Valid
    @JsonDeserialize(converter = StringToListConverter.class)
    private List<String> elevatedAccounts;

    @Min(500)
    @Max(60000)
    private int perMillis;

    @Valid
    @JsonDeserialize(converter = StringToListConverter.class)
    private List<String> lowTrafficAccounts;

    @Min(1)
    private int noOfReqForLowTrafficAccounts;

    @Min(1)
    private int noOfPostReqForLowTrafficAccounts;

    @Min(1000)
    @Max(3_599_999)
    private int intervalInMillisForLowTrafficAccounts;

    public int getNoOfReq() {
        return noOfReq;
    }

    public int getPerMillis() {
        return perMillis;
    }

    public int getNoOfReqForPost() {
        return noOfReqForPost;
    }

    public int getNoOfReqPerNode() {
        return noOfReqPerNode;
    }

    public int getNoOfReqForPostPerNode() {
        return noOfReqForPostPerNode;
    }

    public int getNoOfReqForElevatedAccounts() {
        return noOfReqForElevatedAccounts;
    }

    public int getNoOfPostReqForElevatedAccounts() {
        return noOfPostReqForElevatedAccounts;
    }

    public List<String> getElevatedAccounts() {
        return Optional.ofNullable(elevatedAccounts).orElse(Collections.emptyList());
    }

    public List<String> getLowTrafficAccounts() {
        return Optional.ofNullable(lowTrafficAccounts).orElse(Collections.emptyList());
    }

    public int getNoOfReqForLowTrafficAccounts() {
        return noOfReqForLowTrafficAccounts;
    }

    public int getNoOfPostReqForLowTrafficAccounts() {
        return noOfPostReqForLowTrafficAccounts;
    }

    public int getIntervalInMillisForLowTrafficAccounts() {
        return intervalInMillisForLowTrafficAccounts;
    }
}
