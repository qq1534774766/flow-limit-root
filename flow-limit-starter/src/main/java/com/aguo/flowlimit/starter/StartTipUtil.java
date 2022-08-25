package com.aguo.flowlimit.starter;

import com.aguo.flowlimit.core.IFlowLimit;
import com.aguo.flowlimit.core.aspect.IFlowLimitAspect;
import com.aguo.flowlimit.core.interceptor.IFlowLimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/19 15:59
 * @Description: 流量限制启动提示类
 */
@Slf4j
public class StartTipUtil {



    /**
     * starter启动前的必要检查和日志输出
     *
     * @param applicationContext
     */
    public static void findFlowLimitInstance(ApplicationContext applicationContext) {
        Map<String, IFlowLimit> iFlowLimitMap = applicationContext.getBeansOfType(IFlowLimit.class);
        Map<String, IFlowLimitAspect> iFlowLimitAspectMap = applicationContext.getBeansOfType(IFlowLimitAspect.class);
        Map<String, IFlowLimitInterceptor> iFlowLimitInterceptorMap = applicationContext.getBeansOfType(IFlowLimitInterceptor.class);
        Map<String, FlowLimitProperties> flowLimitProperties = applicationContext.getBeansOfType(FlowLimitProperties.class);
        AtomicBoolean enableRedisFlowLimit = new AtomicBoolean(false);
        flowLimitProperties.values().forEach(it -> enableRedisFlowLimit.set(ObjectUtils.isEmpty(it.getCounterFlowLimitProperties())));
        if (iFlowLimitMap.isEmpty()) {
            log.error("1.Redis流量限制器未启动!");
            log.error("2,请确保抽象类类Abstract XX FlowLimit Aspect/Interceptor被继承实现，且子类被Spring托管");
//            if (iFlowLimitAspectMap.isEmpty()) {
//                log.error("2.请确保{}被继承实现，且子类被Spring托管", AbstractRedisFlowLimitAspect.class.getSimpleName());
//            } else if (iFlowLimitInterceptorMap.isEmpty()) {
//                log.error("2.请确保{}被继承实现，且子类被Spring托管", AbstractRedisFlowLimitInterceptor.class.getSimpleName());
//            }  else if (iFlowLimitInterceptorMap.isEmpty()) {
//                log.error("2.请确保{}被继承实现，且子类被Spring托管", AbstractRedisFlowLimitInterceptor.class.getSimpleName());
//            }
        } else {
            if (!enableRedisFlowLimit.get()) {
                for (IFlowLimit i : iFlowLimitMap.values()) {
                    log.info("发现[流量限制启动器]实现类：{}", i.getClass().getName());
                }
            }
        }
    }

    /**
     * 友好提示
     *
     * @param flowLimitProperties
     * @return
     */
    public static FlowLimitProperties.CounterFlowLimitProperties tipCounterKeyAndProperties(FlowLimitProperties flowLimitProperties) {
        FlowLimitProperties.CounterFlowLimitProperties redisFlowLimitProperties = flowLimitProperties.getCounterFlowLimitProperties();
        if (ObjectUtils.isEmpty(redisFlowLimitProperties)) return null;
        int size1 = redisFlowLimitProperties.getCounterLimitNumber().size();
        int size2 = redisFlowLimitProperties.getCounterHoldingTime().size();
        int size3 = Optional.ofNullable(redisFlowLimitProperties.getCounterKeys()).map(List::size).orElse(0);
        if (size3 == 0) {
            log.error("未指定计数器的key，建议在application.yaml指定，否则默认计数器的key使用的是UUID");
            log.error("可在flowlimit->redis-flow-limit-properties->counter-keys指定");
        }
        if ((size3 != 0 && (!(size1 == size2 && size1 == size3))) || size1 != size2) {
            log.error("1.Redis流量限制器未启动!");
            log.error("application.yaml中，redis计数器的key数量与相应配置的属性数量不一致！");
        }
        return redisFlowLimitProperties;
    }
}
