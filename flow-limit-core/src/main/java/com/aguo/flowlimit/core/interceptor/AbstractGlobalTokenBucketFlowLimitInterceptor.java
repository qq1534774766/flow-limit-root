package com.aguo.flowlimit.core.interceptor;

import com.aguo.flowlimit.core.aspect.AbstractGlobalTokenBucketFlowLimitAspect;
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
import java.util.Map;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/22 14:38
 * @Description: 抽象的流量限制器，子类需要继承本类
 */
@Slf4j
public abstract class AbstractGlobalTokenBucketFlowLimitInterceptor
        implements IFlowLimitInterceptor, WebMvcConfigurer {
    // 成员变量
    private GlobalTokenBucketFlowLimitAspectImpl aspectImpl = new GlobalTokenBucketFlowLimitAspectImpl();
    /**
     * 存放HttpServletRequest，HttpServletResponse
     */
    private final ThreadLocal<Map<String, Object>> threadLocalMap = new ThreadLocal<>();
    /**
     * 拦截器自己，在AutoConfiguration中获取用户实现的拦截器
     */
    private AbstractGlobalTokenBucketFlowLimitInterceptor own;

    @Override
    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!aspectImpl.isEnabled()) return true;
        HashMap<String, Object> map = new HashMap<>();
        map.put("request", request);
        map.put("response", response);
        map.put("handler", handler);
        threadLocalMap.set(map);
        try {
            return (boolean) aspectImpl.flowLimitProcess(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean limitProcess(JoinPoint joinPoint) {
        return aspectImpl.limitProcess(null);
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
     * @param permitsPerSecond
     * @param warmupPeriod
     * @param timeout
     * @param tokenAcquire
     */
    public void build(AbstractGlobalTokenBucketFlowLimitInterceptor own,
                      double permitsPerSecond,
                      long warmupPeriod,
                      long timeout,
                      int tokenAcquire) {
        aspectImpl.build(permitsPerSecond, warmupPeriod, timeout, tokenAcquire);
        this.own = own;
    }

    /**
     * 设置拦截器的拦截配置，比如路径配置等
     *
     * @param registry
     */
    public abstract void setInterceptorPathPatterns(InterceptorRegistration registry);

    public AbstractGlobalTokenBucketFlowLimitInterceptor getOwn() {
        return own;
    }

    public class GlobalTokenBucketFlowLimitAspectImpl extends AbstractGlobalTokenBucketFlowLimitAspect {
        @Override
        protected boolean filterRequest(JoinPoint obj) {
            return InterceptorUtil.filterRequest(AbstractGlobalTokenBucketFlowLimitInterceptor.this, threadLocalMap);
        }

        @Override
        protected Object rejectHandle(JoinPoint obj) throws Throwable {
            return InterceptorUtil.rejectHandle(AbstractGlobalTokenBucketFlowLimitInterceptor.this, threadLocalMap);
        }

        @Override
        public final void pointcut() {
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

    }

    /**
     * 令牌桶是全局限流的，因为单独限流性能开销太大
     */
    @Override
    public final String appendCounterKeyWithUserId(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return null;
    }

}
