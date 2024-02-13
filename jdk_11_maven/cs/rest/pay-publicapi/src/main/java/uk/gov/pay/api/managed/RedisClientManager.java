package uk.gov.pay.api.managed;

import io.dropwizard.lifecycle.Managed;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RedisClientManager implements Managed {
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> statefulRedisConnection;

    @Inject
    public RedisClientManager(RedisClient redisClient) {
        this.redisClient = redisClient;
    }
    
    public StatefulRedisConnection getRedisConnection() {
        if (statefulRedisConnection == null) {
            statefulRedisConnection = redisClient.connect();
        }
        return statefulRedisConnection;
    }

    @Override
    public void start() throws Exception {}

    @Override
    public void stop() throws Exception {
        if (statefulRedisConnection != null) {
            statefulRedisConnection.close();
        }
        redisClient.shutdown();
    }
}
