package com.aguo.flowlimit.core.utils;

import com.aguo.flowlimit.core.enums.CacheDataSourceTypeEnum;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/3 13:32
 * @Description: 为了不影响其他项目Redis的序列化配置，于是聚合本starter所必须的RedisTemplate以避免对其他类造成影响
 * 策略模式！
 */
@Slf4j
public class FlowLimitCacheHelper {
    /**
     * 缓存使用的策略：1.redis 2.local
     */
    private static CacheDataSourceTypeEnum strategy;

    private CacheHelperFactory cacheHelperFactory;

    public FlowLimitCacheHelper(CacheDataSourceTypeEnum strategy,
                                RedisConnectionFactory redisConnectionFactory,
                                List<Long> counterHoldingTime, TimeUnit timeUnit) {
        build(strategy, redisConnectionFactory, counterHoldingTime, timeUnit);
    }

    /**
     * 建造缓存帮助器
     *
     * @param strategy
     * @param redisConnectionFactory
     * @param counterHoldingTime
     * @param timeUnit
     */
    public void build(CacheDataSourceTypeEnum strategy,
                      RedisConnectionFactory redisConnectionFactory,
                      List<Long> counterHoldingTime, TimeUnit timeUnit) {
        //指定策略
        if (ObjectUtils.isNotEmpty(strategy)) {
            FlowLimitCacheHelper.strategy = strategy;
        } else {
            FlowLimitCacheHelper.strategy = CacheDataSourceTypeEnum.Local;
        }
        //工厂初始化
        this.cacheHelperFactory = new CacheHelperFactory();
        if (ObjectUtils.isNotEmpty(redisConnectionFactory)) {
            initRedisStrategyService(redisConnectionFactory);
        }
        if (ObjectUtils.allNotNull(counterHoldingTime, timeUnit)) {
            initLocalStrategyService(counterHoldingTime, timeUnit);
        }
    }

    private void initRedisStrategyService(RedisConnectionFactory redisConnectionFactory) {
        //Redis策略初始化
        RedisStrategyServiceImpl redisStrategyServiceImpl = new RedisStrategyServiceImpl(redisConnectionFactory);
        this.cacheHelperFactory.addStrategyService(CacheDataSourceTypeEnum.Redis, redisStrategyServiceImpl);
    }

    private void initLocalStrategyService(List<Long> counterHoldingTime, TimeUnit timeUnit) {
        //本地缓存策略初始化
        LocalStrategyServiceImpl localStrategyServiceImpl = new LocalStrategyServiceImpl(counterHoldingTime, timeUnit);
        //构建缓存对象
        if (CacheDataSourceTypeEnum.Local == strategy) {

            localStrategyServiceImpl.buildCache();
        }
        this.cacheHelperFactory.addStrategyService(CacheDataSourceTypeEnum.Local, localStrategyServiceImpl);
    }

    public void MySqlStrategyService() {
        //MySql数据源初始化
        MySQLStrategyServiceImpl mySQLStrategyServiceImpl = new MySQLStrategyServiceImpl();
        //设置工厂策略
        this.cacheHelperFactory.addStrategyService(CacheDataSourceTypeEnum.MySql, mySQLStrategyServiceImpl);
    }

    public Boolean increaseKeySafely(String key, Long timeout, Integer CountMax) {
        return cacheHelperFactory.increaseKeySafely(key, timeout, CountMax);
    }

    public Integer getOne(String key) {
        return cacheHelperFactory.getOne(key);
    }

    public void deleteKey(String key) {
        cacheHelperFactory.deleteKey(key);
    }

    public interface IFlowLimitStrategyService {
        /**
         * 获取当前key的Value
         *
         * @param key key
         * @return Integer值
         * @throws Exception Redis可能宕机，异常被工厂对象捕获，会自动切换为本地数据源
         */
        Integer getOne(String key) throws Exception;

        /**
         * set一个缓存key
         *
         * @param key      key
         * @param value    Integer
         * @param timeOut  超时时长
         * @param timeUnit 超时单位
         * @throws Exception Redis可能宕机，异常被工厂对象捕获，会自动切换为本地数据源
         */
        void setOne(String key, Integer value, Long timeOut, TimeUnit timeUnit) throws Exception;

        /**
         * 删除指定的key
         *
         * @param key key
         * @throws Exception Redis可能宕机，异常被工厂对象捕获，会自动切换为本地数据源
         */
        void deleteKey(String key) throws Exception;

        /**
         * 自增key，谨用。如果自增的key是会过期的，刚好过期了发生自增会导致key永久有效
         *
         * @param key key
         * @throws Exception Redis可能宕机，异常被工厂对象捕获，会自动切换为本地数据源
         */
        void increaseKey(String key) throws Exception;

        /**
         * 安全的自增。Redis自增时，如果key刚好过期，那么key会永久有效，造成致命错误。因此本方法有相应逻辑逻辑可以避免。
         *
         * @param key      key
         * @param timeout  超时时长
         * @param CountMax 当前key的最大计数限制
         * @return 当前key记录值是否大于等于CountMax
         * @throws Exception Redis可能宕机，异常被工厂对象捕获，会自动切换为本地数据源
         */
        Boolean increaseKeySafely(String key, Long timeout, Integer CountMax) throws Exception;
    }

    public static class RedisStrategyServiceImpl implements IFlowLimitStrategyService {
        private static final String LUA_INC_SCRIPT_TEXT =
                "local counterKey = KEYS[1]; " +
                        "local timeout = ARGV[1]; " +
                        "local countMax = ARGV[2]; " +
                        "local currentCount = redis.call('get', counterKey); " +
                        "if currentCount and tonumber(currentCount) >= tonumber(countMax) then " +
                        "return 0; " +
                        "end " +
                        "currentCount = redis.call('incr',counterKey); " +
                        "if tonumber(currentCount) == 1 then " +
                        "redis.call('pexpire', counterKey, timeout); " +
                        "end " +
                        "return 1; ";
        private static final DefaultRedisScript<Long> REDIS_INC_SCRIPT = new DefaultRedisScript<>(LUA_INC_SCRIPT_TEXT, Long.class);
        private final RedisTemplate<String, Object> redisTemplate;

        public RedisStrategyServiceImpl(RedisConnectionFactory redisConnectionFactory) {
            this.redisTemplate = userInfoRedisTemplate(redisConnectionFactory);
        }

        @Override
        public Integer getOne(String key) throws Exception {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof Integer) {
                return (Integer) obj;
            }
            if (obj instanceof String) {
                return Integer.valueOf((String) obj);
            }
            return null;
        }

        @Override
        public void setOne(String key, Integer value, Long timeOut, TimeUnit timeUnit) throws Exception {
            redisTemplate.opsForValue().set(key, value, timeOut, timeUnit);
        }

        @Override
        public void deleteKey(String key) throws Exception {
            redisTemplate.delete(key);
        }

        @Override
        public void increaseKey(String key) throws Exception {
            redisTemplate.opsForValue().increment(key);
        }

        @Override
        public Boolean increaseKeySafely(String key, Long timeout, Integer CountMax) throws Exception {
            Long result = execute(Collections.singletonList(key), timeout, CountMax);
            return Optional.ofNullable(result).orElse(1L) == 0L;
        }

        /**
         * 执行Redis脚本
         *
         * @param keys key
         * @param args 参数
         * @return
         */
        public Long execute(List<String> keys, Object... args) {
            return redisTemplate.execute(REDIS_INC_SCRIPT, keys, args);
        }

        private RedisTemplate<String, Object> userInfoRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
            StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            redisTemplate.setKeySerializer(stringRedisSerializer);
            redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            redisTemplate.setHashKeySerializer(stringRedisSerializer);
            redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer());
            redisTemplate.afterPropertiesSet();
            return redisTemplate;
        }

        private RedisSerializer<Object> jackson2JsonRedisSerializer() {
            //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
            Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Object.class);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            serializer.setObjectMapper(mapper);
            return serializer;
        }
    }

    public static class LocalStrategyServiceImpl implements IFlowLimitStrategyService {
        List<Long> counterHoldingTime;
        TimeUnit timeUnit;
        /**
         * key:当前Caffeine配置的超时时长。value：Caffeine配置好的，需要构建成cache才能使用。
         */
        private Map<Long, Caffeine<Object, Object>> caffeineMap;
        /**
         * key:当前Caffeine配置的超时时长。value：Cache是已经构建好的缓存对象，可以直接使用的。
         */
        private Map<Long, Cache<String, Integer>> cacheMap;

        public LocalStrategyServiceImpl() {
        }

        public LocalStrategyServiceImpl(List<Long> counterHoldingTime, TimeUnit timeUnit) {
            this.counterHoldingTime = counterHoldingTime;
            this.timeUnit = timeUnit;
        }

        public LocalStrategyServiceImpl(Map<Long, Caffeine<Object, Object>> caffeineMap) {
            this.caffeineMap = caffeineMap;
        }

        /**
         * key:当前计数器的保持时长，Caffeine 缓存对象
         *
         * @return
         */
        public void initCaffeineMap() {
            this.caffeineMap = this.counterHoldingTime.stream()
                    .collect(Collectors.toMap(this.timeUnit::toMillis, holdingTime -> {
                        return Caffeine.newBuilder()
                                .initialCapacity(Short.MAX_VALUE) //初始大小
                                .maximumSize(Long.MAX_VALUE)  //最大大小
                                .expireAfterAccess(this.timeUnit.toMillis(holdingTime), TimeUnit.MILLISECONDS); //时间单位
                    }));
        }

        /**
         * 构建缓存
         */
        public synchronized void buildCache() {
            this.cacheMap = new HashMap<>();
            if (ObjectUtils.isEmpty(this.caffeineMap)) {
                //初始化caffeine
                initCaffeineMap();
            }
            //构建缓存
            caffeineMap.forEach((HoldTimeKey, value) -> this.cacheMap.put(HoldTimeKey, value.build()));
        }

        @Override
        public Integer getOne(String key) throws Exception {
            Integer result;
            for (Cache<String, Integer> value : cacheMap.values()) {
                result = value.getIfPresent(key);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        @Override
        public void setOne(String key, Integer value, Long timeOut, TimeUnit timeUnit) throws Exception {
            Optional.ofNullable(cacheMap.get(timeUnit.toMillis(timeOut)))
                    .ifPresent(o -> o.put(key, value));
        }

        @Override
        public void deleteKey(String key) throws Exception {
            for (Cache<String, Integer> cache : cacheMap.values()) {
                if (cache.getIfPresent(key) != null) {
                    cache.put(key, 0);
                    return;
                }
            }
        }

        @Override
        public void increaseKey(String key) throws Exception {
            Integer value;
            for (Cache<String, Integer> cache : cacheMap.values()) {
                if (ObjectUtils.isNotEmpty(value = cache.getIfPresent(key))) {
                    cache.put(key, Optional.ofNullable(value).orElse(0) + 1);
                    return;
                }
            }
        }


        @Override
        public Boolean increaseKeySafely(String key, Long timeout, Integer countMax) throws Exception {
            //根据超时时间获取缓存对象
            Cache<String, Integer> cache = cacheMap.get(timeout);
            //更新缓存
            Integer count;
            cache.put(key, Optional.ofNullable((count = cache.getIfPresent(key))).map(o -> o + 1).orElse(1));
            //判断有没超限制
            return Optional.ofNullable(count).orElse(0) >= countMax;
        }
    }

    public static class MySQLStrategyServiceImpl implements IFlowLimitStrategyService {

        @Override
        public Integer getOne(String key) throws Exception {
            return null;
        }

        @Override
        public void setOne(String key, Integer value, Long timeOut, TimeUnit timeUnit) throws Exception {
        }

        @Override
        public void deleteKey(String key) throws Exception {

        }

        @Override
        public void increaseKey(String key) throws Exception {

        }

        @Override
        public Boolean increaseKeySafely(String key, Long timeout, Integer CountMax) throws Exception {
            return null;
        }
    }

    public static class CacheHelperFactory {
        private static final ScheduledExecutorService EXECUTOR = new DefaultEventExecutor();
        private final Map<CacheDataSourceTypeEnum, IFlowLimitStrategyService> map = new HashMap<>();

        public void addStrategyService(CacheDataSourceTypeEnum dataSourceTypeEnum, IFlowLimitStrategyService strategyService) {
            map.put(dataSourceTypeEnum, strategyService);
        }

        public Integer getOne(String key) {
            try {
                return map.get(strategy).getOne(key);
            } catch (Exception e) {
                changeStrategy();
                return -1;
            }
        }

        public void setOne(String key, Integer value, Long timeOut, TimeUnit timeUnit) {
            try {
                map.get(strategy).setOne(key, value, timeOut, timeUnit);
            } catch (Exception e) {
                changeStrategy();
            }
        }

        public void deleteKey(String key) {
            try {
                map.get(strategy).deleteKey(key);
            } catch (Exception e) {
                changeStrategy();
            }
        }

        public void increaseKey(String key) {
            try {
                map.get(strategy).increaseKey(key);
            } catch (Exception e) {
                changeStrategy();
            }
        }

        public Boolean increaseKeySafely(String key, Long timeout, Integer CountMax) {
            try {
                return map.get(strategy).increaseKeySafely(key, timeout, CountMax);
            } catch (Exception e) {
                changeStrategy();
                return false;
            }
        }

        /**
         * 切换策略，加锁。默认使用Redis作为数据源。如果Redis宕机，则自动切换使用本地缓存，一个小时之后切换回Redis缓存。
         *
         * @return
         */
        private synchronized void changeStrategy() {
            log.error("Flow-Limit-Starter:数据源：【{}】失效，切换为【{}】作为数据源", strategy.getDescribe(), CacheDataSourceTypeEnum.Local.getDescribe());
            strategy = CacheDataSourceTypeEnum.Local;
            //构建缓存对象
            Optional.ofNullable(map.get(CacheDataSourceTypeEnum.Local))
                    .ifPresent(o -> ((LocalStrategyServiceImpl) o).buildCache());
            try {
                //取消之前的定时器
//                executor.shutdown();不取消也没影响。
                //开启新的定时器
                EXECUTOR.schedule(() -> {
                    strategy = CacheDataSourceTypeEnum.Redis;
                    log.warn("Flow-Limit-Starter：恢复【{}】作为数据源", strategy);
                }, 1L, TimeUnit.HOURS);
            } catch (Exception e) {
                log.error("启动延迟任务失败");
            }

        }
    }
}
