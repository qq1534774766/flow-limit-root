package com.aguo.flowlimit.core;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/28 9:54
 * @Description: 流量限制顶级接口，策略模式，单一职责
 */
public interface IFlowLimit<T> {

    /**
     * 限流逻辑，如计数器方法、漏桶法、令牌桶等。
     *
     * @param obj
     * @return true:当前请求达到计数/限流上限。
     */
    boolean limitProcess(T obj);

}
