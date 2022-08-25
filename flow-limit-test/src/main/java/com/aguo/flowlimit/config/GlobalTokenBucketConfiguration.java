package com.aguo.flowlimit.config;

import com.aguo.flowlimit.core.aspect.AbstractGlobalTokenBucketFlowLimitAspect;
import org.aspectj.lang.JoinPoint;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/22 14:09
 * @Description: TODO
 */
//@Configuration
public class GlobalTokenBucketConfiguration extends AbstractGlobalTokenBucketFlowLimitAspect {
    @Override
    protected boolean filterRequest(JoinPoint obj) {
        return false;
    }

    @Override
    protected Object rejectHandle(JoinPoint obj) throws Throwable {
        return null;
    }

    @Override
    public void pointcut() {

    }
}
