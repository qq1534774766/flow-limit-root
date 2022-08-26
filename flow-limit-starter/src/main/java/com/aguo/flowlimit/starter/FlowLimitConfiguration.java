package com.aguo.flowlimit.starter;

import com.aguo.flowlimit.core.IFlowLimit;
import com.aguo.flowlimit.core.aspect.AbstractGlobalTokenBucketFlowLimitAspect;
import com.aguo.flowlimit.core.aspect.AbstractRedisFlowLimitAspect;
import com.aguo.flowlimit.core.interceptor.AbstractGlobalTokenBucketFlowLimitInterceptor;
import com.aguo.flowlimit.core.interceptor.AbstractRedisFlowLimitInterceptor;
import com.aguo.flowlimit.core.utils.FlowLimitCacheHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/27 9:55
 * @Description: 流量限制自动配置类，具体装配类
 */
@Slf4j
abstract class FlowLimitConfiguration {


    @Configuration
    @ConditionalOnProperty(prefix = "flowlimit", value = {"enabled"}, havingValue = "true")
    static class RedisFlowLimitConfiguration {
        /**
         * 顺序 1
         * 判断计数器的key是否合法
         *
         * @param flowLimitProperties
         * @return
         */
        @Bean
        @ConditionalOnBean({IFlowLimit.class})
        public FlowLimitProperties.CounterFlowLimitProperties redisFlowLimitProperties(FlowLimitProperties flowLimitProperties) {
            return StartTipUtil.tipCounterKeyAndProperties(flowLimitProperties);
        }

        /**
         * 顺序2
         * 初始化缓存帮助器，初始化了Redis和local两种数据源
         *
         * @param redisConnectionFactory
         * @return
         */
        @Bean
        @ConditionalOnBean({IFlowLimit.class})
        public FlowLimitCacheHelper redisFlowLimitHelper(FlowLimitProperties.CounterFlowLimitProperties properties,
                                                         @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
            if (ObjectUtils.isNotEmpty(properties)) {
                return new FlowLimitCacheHelper(properties.getDataSourceType(),
                        redisConnectionFactory,
                        properties.getCounterHoldingTime(),
                        properties.getCounterHoldingTimeUnit());
            } else {
                return null;
            }
        }


        @Autowired(required = false)
        public void redisFlowLimitAspect(AbstractRedisFlowLimitAspect aspect,
                                         FlowLimitCacheHelper cacheHelper,
                                         FlowLimitProperties.CounterFlowLimitProperties properties) {
            aspect.build(properties.getCounterHoldingTimeUnit(),
                    cacheHelper,
                    properties.isEnabledGlobalLimit(),
                    properties.getPrefixKey(),
                    properties.getCounterKeys(),
                    properties.getCounterHoldingTime(),
                    properties.getCounterLimitNumber());

        }

        /**
         * 设置拦截器的自我字段，自我字段保存的是用户的实现类，为了将用户实现的列注册到MVC中。 <br/>
         */
        @Autowired(required = false)
        public void redisFlowLimitInterceptor(AbstractRedisFlowLimitInterceptor interceptor,
                                              FlowLimitCacheHelper cacheHelper,
                                              FlowLimitProperties.CounterFlowLimitProperties properties) {
            interceptor.build(interceptor,
                    properties.getCounterHoldingTimeUnit(),
                    cacheHelper,
                    properties.isEnabledGlobalLimit(),
                    properties.getPrefixKey(),
                    properties.getCounterKeys(),
                    properties.getCounterHoldingTime(),
                    properties.getCounterLimitNumber());
        }

    }

    @Configuration
    @EnableCaching
    @ConditionalOnProperty(prefix = "flowlimit", value = {"enabled"}, havingValue = "true")
    static class CacheConfiguration {


    }

    @Configuration
    @ConditionalOnProperty(prefix = "flowlimit", value = {"enabled"}, havingValue = "true")
    static class GlobalTokenBucketConfiguration {
        @Bean
        public FlowLimitProperties.GlobalTokenBucketFlowLimitProperties globalTokenBucketFlowLimitProperties(FlowLimitProperties flowLimitProperties) {
            return flowLimitProperties.getGlobalTokenBucketFlowLimitProperties();
        }

        /**
         * 初始化
         */
        @Autowired(required = false)
        public void globalTokenBucketFlowLimitAspect(AbstractGlobalTokenBucketFlowLimitAspect aspect,
                                                     FlowLimitProperties.GlobalTokenBucketFlowLimitProperties properties) {
            aspect.build(Math.max(properties.getPermitsPerSecond(), 1L),
                    Math.max(properties.getWarmupPeriod(), 1L),
                    Math.max(properties.getTimeout(), 1L),
                    1);

        }

        /**
         * 设置拦截器的自我字段，自我字段保存的是用户的实现类，为了将用户实现的列注册到MVC中<br/>
         * 初始化拦截器的属性配置
         *
         * @param interceptor
         */
        @Autowired(required = false)
        public void globalTokenBucketFlowLimitInterceptor(AbstractGlobalTokenBucketFlowLimitInterceptor interceptor,
                                                          FlowLimitProperties.GlobalTokenBucketFlowLimitProperties properties) {
            interceptor.build(interceptor,
                    Math.max(properties.getPermitsPerSecond(), 1L),
                    Math.max(properties.getWarmupPeriod(), 1L),
                    Math.max(properties.getTimeout(), 1L),
                    1);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "flowlimit", value = {"enabled"}, havingValue = "true")
    static class FinalConfiguration implements ApplicationContextAware {
        /**
         * 把本启动器所有的抽象类的实现类，通通取出来，依次检测本启动器是否异常
         *
         * @param applicationContext
         * @throws BeansException
         */
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            StartTipUtil.findFlowLimitInstance(applicationContext);
        }
    }
}
