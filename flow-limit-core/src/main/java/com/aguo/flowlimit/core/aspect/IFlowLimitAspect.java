package com.aguo.flowlimit.core.aspect;

import com.aguo.flowlimit.core.IFlowLimit;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
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

    @Override
    default Object otherHandle(JoinPoint obj, boolean isReject, Object rejectResult) throws Throwable {
        if (!isReject && obj instanceof ProceedingJoinPoint) {
            //默认：拒绝策略未执行或执行了但选择放行，rejectResult即为null，若使用的是AOP中的环绕增强，则执行
            return ((ProceedingJoinPoint) obj).proceed();
        }
        //执行拒绝策略并拒绝  -->取消调用接口
        // 非环绕方法。  -->无需调用，即null
        return rejectResult;
    }
}
