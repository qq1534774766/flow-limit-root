package com.aguo.flowlimit.controller;

import com.aguo.flowlimit.entity.Result;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/25 16:29
 * @Description: TODO
 */
//@RestController
//@RequestMapping
public class IndexController {
    @RequestMapping("/")
    public Result index() {
        return new Result(200, "success");
    }
}
