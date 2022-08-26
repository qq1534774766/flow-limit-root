package com.aguo.flowlimit.core.aspect;

import com.aguo.flowlimit.core.AbstractFlowLimit;
import com.aguo.flowlimit.core.utils.ShowUtil;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/19 14:25
 * @Description: 全局流量限制，使用的是Google包下的RateLimiter类，本质就是 令牌桶方式限流.
 * 因为令牌桶，令牌是无状态的，无法记录用户的信息，因此只能作为全局限流使用。
 */
public abstract class AbstractGlobalTokenBucketFlowLimitAspect
        extends AbstractFlowLimit<JoinPoint> implements IFlowLimitAspect<JoinPoint> {
    private static RateLimiter rateLimiter;
    private static long timeout;
    private static int tokenAcquire = 1;

    public AbstractGlobalTokenBucketFlowLimitAspect() {
    }

    public static void setRateLimiter(double permitsPerSecond, long warmupPeriod) {
        rateLimiter = RateLimiter.create(permitsPerSecond, warmupPeriod, TimeUnit.MILLISECONDS);
    }

    /**
     * 对外提供，设置获取令牌的超时时长
     *
     * @param timeout
     */
    public static void setTimeout(long timeout) {
        AbstractGlobalTokenBucketFlowLimitAspect.timeout = timeout;
    }

    /**
     * 对外提供，设置单次请求需要消耗令牌数量
     *
     * @param tokenAcquire
     */
    public static void setTokenAcquire(int tokenAcquire) {
        AbstractGlobalTokenBucketFlowLimitAspect.tokenAcquire = tokenAcquire;
    }

    /**
     * bean的初始化,构建本bean对象。<br/>
     * 因为是抽象类，没办法使用建造者模式，故使用本方法模拟。
     *
     * @return this
     */
    public void build(double permitsPerSecond, long warmupPeriod, long timeout, int tokenAcquire) {
        setRateLimiter(permitsPerSecond, warmupPeriod);
        setTimeout(Math.max(0, timeout));
        setTokenAcquire(tokenAcquire);
        setEnabled(ObjectUtils.isNotEmpty(rateLimiter));
        if (isEnabled()) ShowUtil.showBanner();
    }

    @Around("pointcut()")
    public Object adviceMode(JoinPoint joinPoint) throws Throwable {
        return this.flowLimitProcess(joinPoint);
    }

    /**
     * 获取令牌成功返回FALSE，失败则返回TRUE。
     *
     * @param obj
     * @return FALSE 不限流，TRUE：限流
     */
    @Override
    public boolean limitProcess(JoinPoint obj) {
        return !rateLimiter.tryAcquire(tokenAcquire, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 对外提供设置限速器的速度的方法
     *
     * @param permitsPerSecond
     */
    public void setRateLimiterRate(double permitsPerSecond) {
        AbstractGlobalTokenBucketFlowLimitAspect.rateLimiter.setRate(permitsPerSecond);
    }

    /**
     * 令牌桶算法不能重置限速器。
     *
     * @param obj 连接点
     * @return
     */
    @Override
    public final Object resetLimiter(JoinPoint obj) {
        return null;
    }

    /**
     * 既然不能重构限速器，那么本方法同样没有意义
     *
     * @param obj
     * @return
     */
    @Override
    protected final boolean beforeLimitingHappenWhetherContinueLimit(JoinPoint obj) {
        return false;
    }

}
