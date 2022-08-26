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

    /**
     * 其他操作，拒绝策略选择抛出异常的形式，则该方法不会被执行到！
     * <br/>
     * 用于放行环绕增强@Around正常调用操作。
     * <br/>
     * 当未执行拒绝策略时，且是环绕增强时，会自动执行相应方法。
     * <br/>
     * 你也可以重写该方法，自定义实现。
     *
     * @param obj          连接点
     * @param rejectResult 拒绝策略执行结果。当且仅当拒绝策略被执行才不为null
     * @return 可以是Processobj.process()的方法执行结果，前提是使用的是环绕增强！
     */
    Object otherHandle(T obj, boolean isReject, Object rejectResult) throws Throwable;

}
