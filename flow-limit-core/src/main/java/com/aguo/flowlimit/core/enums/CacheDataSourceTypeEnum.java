package com.aguo.flowlimit.core.enums;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/18 11:42
 * @Description: 数据源枚举
 */
public enum CacheDataSourceTypeEnum {
    Redis(1, "Redis数据源"),
    Local(2, "本地数据源"),
    MySql(3, "MySQL数据源");

    private final Integer code;
    private final String describe;

    CacheDataSourceTypeEnum(Integer code, String describe) {
        this.code = code;
        this.describe = describe;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescribe() {
        return describe;
    }

}
