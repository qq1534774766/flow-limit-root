package com.aguo.flowlimit.config;

import com.aguo.flowlimit.core.interceptor.AbstractRedisFlowLimitInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/29 11:10
 * @Description: TODO
 */
//@Component
public class RedisFlowLimitInterceptorConfig extends AbstractRedisFlowLimitInterceptor {


    @Override
    public boolean filterRequest(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return false;
    }

    @Override
    public String appendCounterKeyWithUserId(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return null;
    }

    @Override
    public boolean beforeLimitingHappenWhetherContinueLimit(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return false;
    }

    @Override
    public Object rejectHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        response.setCharacterEncoding("utf-8");
        response.getWriter().write("接口调用频繁");
        response.setStatus(404);
        return handler;
    }

    @Override
    public void setInterceptorPathPatterns(InterceptorRegistration registry) {
        registry.addPathPatterns("/**/**");
    }
}
