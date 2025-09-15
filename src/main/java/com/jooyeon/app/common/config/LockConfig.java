package com.jooyeon.app.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 분산락 설정
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 *
 * Redis 분산락 설정 예시:
 * - RedissonClient 빈 등록
 * - Redis 연결 설정
 * - 락 타임아웃 및 재시도 설정
 */
@Configuration
@EnableAspectJAutoProxy
public class LockConfig {

    // 실제 Redis 환경에서는 다음과 같은 설정이 필요:
    // 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다

    /*
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://localhost:6379")
              .setConnectionMinimumIdleSize(1)
              .setConnectionPoolSize(5)
              .setIdleConnectionTimeout(10000)
              .setConnectTimeout(10000)
              .setTimeout(3000)
              .setRetryAttempts(3)
              .setRetryInterval(1500);

        return Redisson.create(config);
    }

    @Bean
    public RedisLockService redisLockService(RedissonClient redissonClient) {
        return new RedissonLockService(redissonClient);
    }
    */
}