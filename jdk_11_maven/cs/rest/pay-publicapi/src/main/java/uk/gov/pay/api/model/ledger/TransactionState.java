package uk.gov.pay.api.model.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionState {

    private String status;
    private boolean finished;
    private String message;
    private String code;

    public String getStatus() {
        return status;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public TransactionState() {
    }

    public TransactionState(String status, boolean finished) {
        this.status = status;
        this.finished = finished;
    }
}
