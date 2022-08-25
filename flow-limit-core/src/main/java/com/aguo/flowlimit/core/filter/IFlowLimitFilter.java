package com.aguo.flowlimit.core.filter;

import com.aguo.flowlimit.core.IFlowLimit;

import javax.servlet.Filter;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/19 14:06
 * @Description: 过滤器顶级接口
 */
public interface IFlowLimitFilter extends Filter, IFlowLimit {


}
