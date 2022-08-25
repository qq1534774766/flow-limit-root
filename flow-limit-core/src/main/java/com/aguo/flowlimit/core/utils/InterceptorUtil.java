package com.aguo.flowlimit.core.utils;


import com.aguo.flowlimit.core.interceptor.IFlowLimitInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/22 14:51
 * @Description: 拦截器工具类
 */
public class InterceptorUtil {
    // 适配时需要的转化方法，从ThreadLocal取出拦截器需要的字段
    public static HttpServletRequest getRequestFromThreadLocalSafely(ThreadLocal<Map<String, Object>> threadLocalMap) {
        return (HttpServletRequest) Optional.ofNullable(threadLocalMap.get())
                .map(o -> o.get("request"))
                .orElse(null);
    }

    public static HttpServletResponse getResponseFromThreadLocalSafely(ThreadLocal<Map<String, Object>> threadLocalMap) {
        return (HttpServletResponse) Optional.ofNullable(threadLocalMap.get())
                .map(o -> o.get("response"))
                .orElse(null);
    }

    public static Object getHandlerFromThreadLocalSafely(ThreadLocal<Map<String, Object>> threadLocalMap) {
        return Optional.ofNullable(threadLocalMap.get())
                .map(o -> o.get("handler"))
                .orElse(null);
    }

    //以下都是拦截器适配AOP的方法
    public static boolean filterRequest(IFlowLimitInterceptor interceptor, ThreadLocal<Map<String, Object>> threadLocalMap) {
        return interceptor.filterRequest(InterceptorUtil.getRequestFromThreadLocalSafely(threadLocalMap), InterceptorUtil.getResponseFromThreadLocalSafely(threadLocalMap),
                InterceptorUtil.getHandlerFromThreadLocalSafely(threadLocalMap));
    }

    public static Object rejectHandle(IFlowLimitInterceptor interceptor, ThreadLocal<Map<String, Object>> threadLocalMap) throws Exception {
        return interceptor.rejectHandle(InterceptorUtil.getRequestFromThreadLocalSafely(threadLocalMap), InterceptorUtil.getResponseFromThreadLocalSafely(threadLocalMap),
                InterceptorUtil.getHandlerFromThreadLocalSafely(threadLocalMap));
    }

    public static boolean beforeLimitingHappenWhetherContinueLimit(IFlowLimitInterceptor interceptor, ThreadLocal<Map<String, Object>> threadLocalMap) {
        return interceptor.beforeLimitingHappenWhetherContinueLimit(InterceptorUtil.getRequestFromThreadLocalSafely(threadLocalMap), InterceptorUtil.getResponseFromThreadLocalSafely(threadLocalMap),
                InterceptorUtil.getHandlerFromThreadLocalSafely(threadLocalMap));
    }

    public static String appendCounterKeyWithUserId(IFlowLimitInterceptor interceptor, ThreadLocal<Map<String, Object>> threadLocalMap) {
        return interceptor.appendCounterKeyWithUserId(InterceptorUtil.getRequestFromThreadLocalSafely(threadLocalMap), InterceptorUtil.getResponseFromThreadLocalSafely(threadLocalMap),
                InterceptorUtil.getHandlerFromThreadLocalSafely(threadLocalMap));
    }


    //endregion
}
