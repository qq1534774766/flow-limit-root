package com.aguo.flowlimit.config;

import com.aguo.flowlimit.core.aspect.AbstractRedisFlowLimitAspect;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/26 17:56
 * @Description: TODO
 */
//@Configuration
//@Aspect
public class RedisFlowLimitConfig extends AbstractRedisFlowLimitAspect {

    @Pointcut("within(cn.sinohealth.flowlimit.springboot.starter.test.TestController)" +
            "&&@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void pointcut() {
    }

    @Override
    protected boolean filterRequest(JoinPoint joinPoint) {
        return false;
    }

    @Override
    protected boolean beforeLimitingHappenWhetherContinueLimit(JoinPoint joinPoint) {
        return false;
    }

    @Override
    protected Object rejectHandle(JoinPoint joinPoint) throws Throwable {
        throw new Exception("AOP拦截接口");
    }


    @Override
    protected String appendCounterKeyWithUserId(JoinPoint joinPoint) {
        return "2";
    }
}
