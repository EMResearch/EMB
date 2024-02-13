package uk.gov.pay.api.resources;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class LedgerOrConnectorStrategyTemplate<T> {

    private static final Logger logger = LoggerFactory.getLogger(LedgerOrConnectorStrategyTemplate.class);
    private String strategy;
    private List<String> VALID_STRATEGIES = ImmutableList.of("ledger-only", "connector-only");

    public LedgerOrConnectorStrategyTemplate(String strategy) {
        this.strategy = strategy;
    }

    private void validate() {
        if (!StringUtils.isBlank(strategy) && !VALID_STRATEGIES.contains(strategy)) {
            logger.warn("Not valid strategy (valid values are \"ledger-only\", \"connector-only\" or empty); using the default strategy");
            strategy = null;
        }
    }

    public T validateAndExecute() {
        validate();

        if ("connector-only".equals(strategy)) {
            return executeConnectorOnlyStrategy();
        } else if ("ledger-only".equals(strategy)) {
            return executeLedgerOnlyStrategy();
        }

        return executeDefaultStrategy();
    }

    protected abstract T executeLedgerOnlyStrategy();

    protected abstract T executeDefaultStrategy();

    protected abstract T executeConnectorOnlyStrategy();
}


