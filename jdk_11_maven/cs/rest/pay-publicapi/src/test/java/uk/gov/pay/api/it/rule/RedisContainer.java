package uk.gov.pay.api.it.rule;

import io.lettuce.core.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static java.lang.String.format;

public class RedisContainer {

    private static final Logger logger = LoggerFactory.getLogger(RedisContainer.class);
    private static GenericContainer<?> REDIS_CONTAINER;
    private static final int PORT = 6379;
    
    static GenericContainer<?> getOrCreateRedisContainer() {
        if (REDIS_CONTAINER == null) {
            REDIS_CONTAINER = new GenericContainer<>("redis:5.0.6") // elasticache engine version
                    .withExposedPorts(PORT)
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));

            REDIS_CONTAINER.start();
            logger.info(format("Redis container started, mapped %o to host port %o", PORT, REDIS_CONTAINER.getMappedPort(PORT)));
        }
        return REDIS_CONTAINER;
    }
    
    static String getConnectionUrl() {
        return format("%s:%s", REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getMappedPort(PORT)) ;
    }

    static void clearRedisCache() {
        try (RedisClient redisClient = RedisClient.create(format("redis://%s", getConnectionUrl()))) {
            String response = redisClient.connect().sync().flushall();
            if (!response.equals("OK")) {
                logger.warn("Unexpected response from redis flushAll command: " + response);
            }
        }
    }
}
