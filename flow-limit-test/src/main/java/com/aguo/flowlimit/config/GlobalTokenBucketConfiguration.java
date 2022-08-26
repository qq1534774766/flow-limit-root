package com.aguo.flowlimit.config;

import com.aguo.flowlimit.core.aspect.AbstractGlobalTokenBucketFlowLimitAspect;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/22 14:09
 * @Description: TODO
 */
//@Component
//@Aspect
public class GlobalTokenBucketConfiguration extends AbstractGlobalTokenBucketFlowLimitAspect {
    @Override
    protected boolean filterRequest(JoinPoint obj) {
        return false;
    }

    @Override
    protected Object rejectHandle(JoinPoint obj) throws Throwable {
        throw new Exception("频繁");
    }

    @Override
    @Pointcut("within(com.aguo.flowlimit.controller.IndexController)" +
            "&&@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void pointcut() {

    }
}
