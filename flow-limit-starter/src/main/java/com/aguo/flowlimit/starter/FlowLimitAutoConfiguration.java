package com.aguo.flowlimit.starter;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/26 11:01
 * @Description: 流量限制自动配置类，会被springboot自动加载
 */
@Configuration
@EnableConfigurationProperties({FlowLimitProperties.class})
@Import({FlowLimitConfiguration.RedisFlowLimitConfiguration.class,
        FlowLimitConfiguration.CacheConfiguration.class,
        FlowLimitConfiguration.GlobalTokenBucketConfiguration.class,
        FlowLimitConfiguration.FinalConfiguration.class})
public class FlowLimitAutoConfiguration {


}

