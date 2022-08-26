package com.aguo.flowlimit.core.interceptor;

import com.aguo.flowlimit.core.aspect.AbstractRedisFlowLimitAspect;
import com.aguo.flowlimit.core.utils.FlowLimitCacheHelper;
import com.aguo.flowlimit.core.utils.InterceptorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/29 11:07
 * @Description: 使用基于类的适配器模式，在RedisAspect基础上改造
 */
@Slf4j
public abstract class AbstractRedisFlowLimitInterceptor
        implements IFlowLimitInterceptor, WebMvcConfigurer {

    private AbstractRedisFlowLimitAspect redisFlowLimitAspect = new RedisFlowLimitAspectImpl();

    /**
     * 存放HttpServletRequest，HttpServletResponse
     */
    private final ThreadLocal<Map<String, Object>> threadLocalMap = new ThreadLocal<>();
    /**
     * 拦截器自己，在AutoConfiguration中获取用户实现的拦截器
     */
    private AbstractRedisFlowLimitInterceptor own;

    @Override
    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!redisFlowLimitAspect.isEnabled()) return true;
        HashMap<String, Object> map = new HashMap<>();
        map.put("request", request);
        map.put("response", response);
        map.put("handler", handler);
        threadLocalMap.set(map);
        try {
            return (boolean) redisFlowLimitAspect.flowLimitProcess(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean limitProcess(JoinPoint joinPoint) {
        return redisFlowLimitAspect.limitProcess(null);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册用户的拦截器
        setInterceptorPathPatterns(registry.addInterceptor(getOwn()));
        log.info("拦截器注册成功：{}", getOwn().getClass().getName());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        threadLocalMap.remove();//防止内存泄漏
    }

    /**
     * 因为是抽象类，没办法使用建造者模式，故使用本方法模拟。
     *
     * @param own
     */
    public void build(AbstractRedisFlowLimitInterceptor own,
                      TimeUnit timeUnit,
                      FlowLimitCacheHelper redisHelper,
                      boolean enabledGlobalLimit,
                      String prefixKey,
                      List<String> counterKeys,
                      List<Long> counterHoldingTime,
                      List<Integer> counterLimitNumber) {
        redisFlowLimitAspect.build(timeUnit, redisHelper, enabledGlobalLimit, prefixKey, counterKeys, counterHoldingTime, counterLimitNumber);
        this.own = own;
    }


    /**
     * 设置拦截器的拦截配置，比如路径配置等
     *
     * @param registry
     */
    public abstract void setInterceptorPathPatterns(InterceptorRegistration registry);


    public AbstractRedisFlowLimitInterceptor getOwn() {
        return own;
    }

    private class RedisFlowLimitAspectImpl extends AbstractRedisFlowLimitAspect {

        @Override
        protected boolean filterRequest(JoinPoint joinPoint) {
            return InterceptorUtil.filterRequest(AbstractRedisFlowLimitInterceptor.this, threadLocalMap);
        }


        @Override
        protected boolean beforeLimitingHappenWhetherContinueLimit(JoinPoint joinPoint) {
            return InterceptorUtil.beforeLimitingHappenWhetherContinueLimit(AbstractRedisFlowLimitInterceptor.this, threadLocalMap);
        }

        @Override
        protected Object rejectHandle(JoinPoint joinPoint) throws Throwable {
            return InterceptorUtil.rejectHandle(AbstractRedisFlowLimitInterceptor.this, threadLocalMap);
        }

        @Override
        public String appendCounterKeyWithMode() {
            return "interceptor:";
        }

        @Override
        protected String appendCounterKeyWithUserId(JoinPoint joinPoint) {
            return InterceptorUtil.appendCounterKeyWithUserId(AbstractRedisFlowLimitInterceptor.this, threadLocalMap);
        }

        @Override
        protected Object otherHandle(JoinPoint joinPoint, boolean isReject, Object rejectResult) throws Throwable {
            //true放行
            if (ObjectUtils.isNotEmpty(rejectResult) && rejectResult instanceof Boolean) {
                return rejectResult;
            }
            //被拒绝 isReject=true，返回false
            //没有被拒绝
            return !isReject;
        }

        @Override
        public final void pointcut() {
        }

    }
    //endregion
}
