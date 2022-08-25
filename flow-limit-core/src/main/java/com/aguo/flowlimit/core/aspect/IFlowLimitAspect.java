package com.aguo.flowlimit.core.aspect;

import com.aguo.flowlimit.core.IFlowLimit;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/28 13:55
 * @Description: TODO
 */
public interface IFlowLimitAspect<T extends JoinPoint> extends IFlowLimit<JoinPoint> {
    /**
     * 切入点
     */
    void pointcut();

    /**
     * 定义增强方式，默认使用环绕增强
     * <br/>
     * 不建议子类重写。如需重写，则<strong>必须</strong>回调父类的 flowLimitProcess(joinPoint)方法！
     */
    @Around("pointcut()")
    Object adviceMode(T obj) throws Throwable;


}
