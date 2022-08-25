package com.aguo.flowlimit.core;

import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/25 10:02
 * @Description: 限流、反爬抽象顶级类。 《模板方法模式》，子类可以继承该类，以实现不同的限制策略
 * <br/>
 */
@Data
public abstract class AbstractFlowLimit<T> implements IFlowLimit<T> {
    /**
     * 流量限制控制开关，根据实现类所需要的bean决定是否能开启。
     * 如Redis需要RedisTemplate，否则不启用流量限制
     */
    private /*volatile*/ boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    /**
     * 过滤不进行限制的请求，比如登录注册、文件下载、静态资源等
     *
     * @return true:表示过滤该请求，即不限制该请求，false限制该请求
     */
    protected abstract boolean filterRequest(T obj);

    /**
     * 定义模板方法，禁止子类重写方法
     */
    public final Object flowLimitProcess(T obj) throws Throwable {
        if (!enabled) {
            return otherHandle(obj, false, null);
        }
        if (filterRequest(obj)) {
            return otherHandle(obj, false, null);
        }
        //限流逻辑
        Object rejectResult = null;
        boolean isReject = false;
        //1.限流操作。
        if (limitProcess(obj)) {
            // 2.限流前置操作
            if (beforeLimitingHappenWhetherContinueLimit(obj)) {
                resetLimiter(obj);
            } else {
                //执行拒绝策略
                isReject = true;
                rejectResult = rejectHandle(obj);
            }
        }
        //其他操作，如验证通过重置限制限流器等。最后返回执行结果
        return otherHandle(obj, isReject, rejectResult);
    }


    /**
     * 在限制发生之前是否继续限制
     * <br/>
     * 可以反馈客户端滑动验证码，手机验证码登录验证操作。
     *
     * @return TRUE：用户完成验证->清空限流器->放行。FALSE：未完成验证，执行拒绝策略。
     */
    protected abstract boolean beforeLimitingHappenWhetherContinueLimit(T obj);

    /**
     * 拒绝策略，真正执行拒绝操作
     * <br/>
     * 可以进行拒绝操作,如 1.抛出异常，或者2.返回错误信息。
     *
     * @return 1.抛出异常：无需返回任何东西 <br/>
     * 2.错误信息：返回的类型与Controller返回类型<strong>必须</strong>一致
     */
    protected abstract Object rejectHandle(T obj) throws Throwable;

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
    protected Object otherHandle(T obj, boolean isReject, Object rejectResult) throws Throwable {
        if (!isReject && obj instanceof ProceedingJoinPoint) {
            //默认：拒绝策略未执行或执行了但选择放行，rejectResult即为null，若使用的是AOP中的环绕增强，则执行
            return ((ProceedingJoinPoint) obj).proceed();
        }
        //执行拒绝策略并拒绝  -->取消调用接口
        // 非环绕方法。  -->无需调用，即null
        return rejectResult;
    }

    /**
     * 重置限流器、限流等
     *
     * @param obj 连接点
     * @return 保留返回、按需使用
     */
    public abstract Object resetLimiter(T obj);
}
