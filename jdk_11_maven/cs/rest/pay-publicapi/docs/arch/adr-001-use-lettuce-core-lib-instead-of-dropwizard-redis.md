# ADR 001 - Use lettuce-core library instead of dropwizard redis

## Context

We were using an [unsupported dropwizard-redis](https://github.com/benjamin-bader/droptools/tree/master/dropwizard-redis) bundle. Dropwizard now offer a managed bundle which provides a managed redis. This bundle uses `lettuce-core` instead of `jedis`.

[We wanted to upgrade to this new library](https://payments-platform.atlassian.net/browse/PP-6343).

The advantages of using this library are:

* Configuration
* Client lifecycle management
* Client health checks
* Dropwizard Metrics integration

However, the dropwizard-redis library's io.dropwizard.redis.RedisClientFactory class 
[expects](https://github.com/dropwizard/dropwizard-redis/blob/master/src/main/java/io/dropwizard/redis/RedisClientFactory.java#L54) 
to make a connection with Redis on application startup. If a connection cannot be made an exception will be thrown 
which causes the application to fail to start up. 

This has some unintended consequences:

* Mandating a connection on startup causes many integration tests to fail. Making a redis connection available for every relevant test is a fairly big change.
* Mandating a connection might affect running Publicapi in dev/local environments.

In order to work around this issue we could either:

1. adapt the dropwizard-redis library so that it doesn't crash if the redis connection is not available
2. use lettuce-core directly

| Option  | Pros | Cons |
|---------|---------|---------|
| adapt   | use common component; metrics instrumentation | more effort than justified |
| use lettuce-core directly | simpler; not hard; we don't need the extra features of dropwizard-redis; we don't need healthchecks because redis is optional | |

On balance we think there are no advantages to us in using dropwizard-redis so we'll just use the lettuce-core library directly.

## Decision

We will use the [lettuce-core](https://github.com/lettuce-io/lettuce-core) directly. 

## Status

Accepted 
