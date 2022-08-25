package com.aguo.flowlimit.core.interceptor;

import com.aguo.flowlimit.core.aspect.AbstractGlobalTokenBucketFlowLimitAspect;
import com.aguo.flowlimit.core.utils.InterceptorUtil;
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
 * @Description: 抽象的流量限制器，
 */
public abstract class AbstractGlobalTokenBucketFlowLimitInterceptor
        extends AbstractGlobalTokenBucketFlowLimitAspect
        implements IFlowLimitInterceptor, WebMvcConfigurer {
    // 成员变量
    /**
     * 存放HttpServletRequest，HttpServletResponse
     */
    private final ThreadLocal<Map<String, Object>> threadLocalMap = new ThreadLocal<>();
    /**
     * 拦截器自己，在AutoConfiguration中获取用户实现的拦截器
     */
    private AbstractGlobalTokenBucketFlowLimitInterceptor own;

    /**
     * 拦截器模式下，不需要指定切点
     */
    @Override
    public final void pointcut() {

    }


    @Override
    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!isEnabled()) return true;
        HashMap<String, Object> map = new HashMap<>();
        map.put("request", request);
        map.put("response", response);
        map.put("handler", handler);
        threadLocalMap.set(map);
        try {
            return (boolean) flowLimitProcess(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        threadLocalMap.remove();//防止内存泄漏
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册用户的拦截器
        setInterceptorPathPatterns(registry.addInterceptor(getOwn()));
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

    public void setOwn(AbstractGlobalTokenBucketFlowLimitInterceptor own) {
        this.own = own;
    }

    @Override
    protected boolean filterRequest(JoinPoint obj) {
        return InterceptorUtil.filterRequest(this, threadLocalMap);
    }

    @Override
    protected Object rejectHandle(JoinPoint obj) throws Throwable {
        return InterceptorUtil.rejectHandle(this, threadLocalMap);
    }

    /**
     * 令牌桶是全局限流的，因为单独限流性能开销太大
     */
    @Override
    public final String appendCounterKeyWithUserId(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return null;
    }

    @Override
    protected Object otherHandle(JoinPoint obj, boolean isReject, Object rejectResult) throws Throwable {
        //true放行
        if (ObjectUtils.isNotEmpty(rejectResult) && rejectResult instanceof Boolean) {
            return rejectResult;
        }
        //被拒绝 isReject=true，返回false
        //没有被拒绝
        return !isReject;
    }
}
