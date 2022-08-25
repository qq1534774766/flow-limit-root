package com.aguo.flowlimit.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/25 16:29
 * @Description: TODO
 */
@Data
@AllArgsConstructor
public class Result {
    private Integer code;
    private Object data;
}
