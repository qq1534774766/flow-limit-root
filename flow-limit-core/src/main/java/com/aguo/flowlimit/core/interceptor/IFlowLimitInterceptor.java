package com.aguo.flowlimit.core.interceptor;

import com.aguo.flowlimit.core.IFlowLimit;
import org.aspectj.lang.JoinPoint;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/28 16:03
 * @Description: TODO
 */
public interface IFlowLimitInterceptor extends HandlerInterceptor, IFlowLimit<JoinPoint> {
    /**
     * 过滤不进行限制的请求，比如登录注册、文件下载、静态资源等
     *
     * @return true:表示过滤该请求，即不限制该请求，false限制该请求
     */
    boolean filterRequest(HttpServletRequest request, HttpServletResponse response, Object handler);

    /**
     * 重构计数器的key，未开启全局计数，即计数器要拼接的用户ID，对每一个用户单独限流
     *
     * @return 重构逻辑
     */
    String appendCounterKeyWithUserId(HttpServletRequest request, HttpServletResponse response, Object handler);

    /**
     * 在限制发生之前是否继续限制
     * <br/>
     * 可以反馈客户端滑动验证码，手机验证码登录验证操作。
     *
     * @return TRUE：用户完成验证->清空计数器->放行。FALSE：未完成验证，执行拒绝策略。
     */
    boolean beforeLimitingHappenWhetherContinueLimit(HttpServletRequest request, HttpServletResponse response, Object handler);

    /**
     * 拒绝策略，真正执行拒绝操作
     * <br/>
     * 可以进行拒绝操作,如 1.抛出异常
     *
     * @return
     */
    Object rejectHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
}
