# 1.工程简介

> flow-limit-root，是一个限流与反爬的解决方案，提供应用抗住大量的非法请求。
>
> ps: flow-limit-root由flow-limit-spring-boot-starter重构而来，后者已经停止维护。

目录结构：

flow-limit-root

|---flow-limit-core

|---flow-limit-starter

|---flow-limit-test

- 更新日志：
  - v1.0:实现`Redis AOP`计数器限流。
  - v1.1:重构启动器结构，使用**`模板方法模式`**。
  - v1.2:新增Redis拦截器方式，本质是`Redis AOP`适配，即**`适配器模式`**。
  - v1.3:`AOP`与`Interceptor`可以一起使用，因其执行顺序`Interceptor`>`AOP`，因此需要准确的配置切点与拦截路径。
  - v1.4:配置文件，`prefix`、`counterKey`允许为null。修复重大Bug。
  - v1.5
    - 重构Cache帮助器为：**`工厂模式`**+**`策略模式`**。
    - 策略模式可以更好的拓展系统，目前**已经实现**`Redis`作为数据源、`caffeine`作为本地缓存数据源，**mysql尚未实现**。
    - 考虑到本地缓存是单机模式，不能分布式，所以`默认是Redis`
    - **当Redis无法使用或宕机时**，自动切换到本地数据源！延迟1小时后，自动切换回`Redis数据源`
  - v1.6
    - 新增AOP方式的**全局**令牌桶速度限制器，包含AOP与拦截器两种方式。

简单使用，只需引入依赖，简单配置一下就能使用，无侵入，易插拔，易使用。

# 2.快速开始

这里目前只演示了starter的方式快速使用。

## 2.1 引入依赖，依赖需在本地仓库或是局域网内服务器仓库

[访问中康仓库地址](http://192.168.16.87:8081/#browse/browse:maven-snapshots:cn%2Fsinohealth%2Fflow-limit-spring-boot-starter%2F1.6.0-SNAPSHOT%2F1.6.0-20220822.091036-3)

```xml
<dependency>
  <groupId>cn.sinohealth</groupId>
  <artifactId>flow-limit-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 2.2 编写application.yaml配置文件

- counter-flow-limit-properties 与 global-token-bucket-flow-limit-properties:
  - 选择一个配置就行，前者可以针对**单个用户限流**，后者**只能全局限流**，也可同时配置。
  - 前者是使用redis的计数器计数来进行限流的
  - 后者使用的是令牌桶的方式限流的，算法示意图如下
    - ![img](https://img-blog.csdnimg.cn/img_convert/f9367b5d8a30336fbf1c00a47967cceb.png)

```yaml
#配置Redis
spring:
  redis:
    host: 192.168.16.87
    port: 6379
# 配置本启动器
flowlimit:
  #是否启用流量限制
  enabled: true
  counter-flow-limit-properties:
    #数据源类型，有redis和local，默认redis
    data-source-type: local
      #是否启用全局限制，即所有用户所有操作均被一起计数限制.
      enabled-global-limit: true
      #即计数器的key前缀，可以为空，但不建议
      prefix-key: "icecreamtest::innovative-medicine:desktop-web:redis:flow:limit"
      #每个计数器的Key，注意计数器的key数量与相应配置值要一致，可以为空，但不建议。
      counter-keys:
        - "counter:second:3:"
        - "counter:minutes:2:"
        - "counter:minutes:5:"
        - "counter:hour:1:"
      - ...
    #每个计数器的保持时长，单位是秒
    counter-holding-time:
      - 6
      - 180
      - 300
      - 3600
      - ...
    #每个计数器对应的限流次数，即接口调用次数限制
    counter-limit-number:
      - 5
      - 80
      - 320
      - 240000
      - ...
  global-token-bucket-flow-limit-properties:
    #QPS，即限制一秒钟内最大的请求数量。
    permits-per-second: 50
    #超时时间，当QPS到达阈值时，允许请求等待的时长
    timeout: 10
    #预热期时长，单位毫秒
    warmup-period: 1000
```

## 2.3 实现代码

### 2.3.1 计数器算法

#### 2.3.1.1 AOP

- AbstractRedisFlowLimitAspect.class
  - 经典的计数器，计数器有保持时间，计数上线，当达到阈值时，会触发限流

新建一个类MyRedisFlowLimitConfig继承AbstractRedisFlowLimitAspect抽象类，实现抽象类的方法

```java
//交由Spring托管
@Configuration
//开启切面
@Aspect
public class MyRedisFlowLimitConfig extends AbstractRedisFlowLimitAspect {
  //选择需要被限制的Controller方法
    @Pointcut("within(cn.sinohealth.flowlimit.springboot.starter.test.TestController)" +
            "&&@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void pointcut() {
    }

    //过滤哪些请求，返回TRUE表示对该请求不进行计数限制
    @Override
    protected boolean filterRequest(JoinPoint joinPoint) {
      if (threadLocal.get().getUseID() **1){
        //放行超级管理员
            return true;
        }
        return false;
    }

  //当计数器达到上限时执行，返回TRUE则清空计数器放行，否则拒绝策略
  @Override
  protected boolean beforeLimitingHappenWhetherContinueLimit(JoinPoint joinPoint) {
    return false;
  }

  //拒绝策略，可以选择抛出异常，或者返回与Controller类型一样的数据封装
  @Override
  protected Object rejectHandle(JoinPoint joinPoint) throws Throwable {
    response.setCharacterEncoding("utf-8");
    response.getWriter().write("接口调用频繁");
    response.setStatus(610);
  }

  //追加用户的ID，enabled-global-limit: true时，会被调用，返回当前登录用户的ID以便限流只是针对当前用户生效。
  @Override
  protected String appendCounterKeyWithUserId(JoinPoint joinPoint) {
    return threadlocal.get().getUserId();
  }
}
```

#### 2.3.1.2 拦截器

新建MyRedisFlowLimitInterceptorConfig.class继承AbstractRedisFlowLimitInterceptor并实现其所有方法。

**父类已经将拦截器注册了，因此不需要手动在WebMvcConfiguration中注册拦截器，仅仅需要配置拦截路径即可**

```java
//交由Spring托管
@Component
public class MyRedisFlowLimitInterceptorConfig extends AbstractRedisFlowLimitInterceptor {
  //设置拦截器的拦截路径
    @Override
    public void setInterceptorPathPatterns(InterceptorRegistration registry) {
        registry.addPathPatterns("/api/**");
    }

    //过滤哪些请求，返回TRUE表示对该请求不进行计数限制
    @Override
    public boolean filterRequest(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return false;
    }

    //追加用户的ID，enabled-global-limit: true时，会被调用，返回当前登录用户的ID以便限流只是针对当前用户生效。
    @Override
    public String appendCounterKeyWithUserId(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return null;
    }

    //当计数器达到上限时执行，返回TRUE则清空计数器放行，否则拒绝策略
    @Override
    public boolean beforeLimitingHappenWhetherContinueLimit(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return false;
    }

    //拒绝策略，可以选择抛出异常，或者返回与Controller类型一样的数据封装
    @Override
    public void rejectHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
      response.setCharacterEncoding("utf-8");
      response.getWriter().write("接口调用频繁");
      response.setStatus(610);
    }

}
```

### 2.3.2 令牌桶算法

[更多详看](https://blog.csdn.net/resilient/article/details/121609412)

#### 2.3.2.1 AOP

新建MyGlobalTokenBucketFlowLimitAspect.class继承AbstractGlobalTokenBucketFlowLimitAspect并实现其所有方法。

```java

@Component
public class MyGlobalTokenBucketFlowLimitAspect extends AbstractGlobalTokenBucketFlowLimitAspect {
  //选择需要被限制的Controller方法
  @Override
  @Pointcut("within(cn.sinohealth.flowlimit.springboot.starter.test.TestController)" +
          "&&@annotation(org.springframework.web.bind.annotation.RequestMapping)")
  public void pointcut() {

  }

  //过滤哪些请求，返回TRUE表示对该请求不进行限制
  @Override
  protected boolean filterRequest(JoinPoint obj) {
    return false;
  }

  //拒绝策略，可以选择抛出异常，或者返回与Controller类型一样的数据封装
  @Override
  protected Object rejectHandle(JoinPoint obj) throws Throwable {
    response.setCharacterEncoding("utf-8");
    response.getWriter().write("接口调用频繁");
    response.setStatus(610);
  }
}
```

### 2.3.2.2 拦截器

新建MyAbstractRedisFlowLimitInterceptor.class继承AbstractRedisFlowLimitInterceptor并实现其所有方法。

```java

@Component
public class MyAbstractRedisFlowLimitInterceptor extends AbstractGlobalTokenBucketFlowLimitInterceptor {
  //设置拦截器的拦截路径
  @Override
  public void setInterceptorPathPatterns(InterceptorRegistration registry) {
    registry.addPathPatterns("/**/**");
  }

  //过滤哪些请求，返回TRUE表示对该请求不进行限制
  @Override
  public boolean filterRequest(HttpServletRequest request, HttpServletResponse response, Object handler) {
    return false;
  }

  //当计数器达到上限时执行，返回TRUE则清空计数器放行，否则拒绝策略
  @Override
  public boolean beforeLimitingHappenWhetherContinueLimit(HttpServletRequest request, HttpServletResponse response, Object handler) {
    return false;
  }

  //拒绝策略，可以选择抛出异常，或者返回与Controller类型一样的数据封装
  @Override
  public Object rejectHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    response.setCharacterEncoding("utf-8");
    response.setContentType("application/json;");
    response.getWriter().write("接口调用频繁");
    response.setStatus(500);
    return handler;
  }
}
```

# 3.注意

计数器算法抽象类都有一个公开的方法，resetLimiter();该方法能够重置计数器。

应用场景：当用户请求频繁，后端报错，前端要求用户验证。验证接口可以在Controller中方法，然后调用resetLimiter()方法即可。

```java
public class MyController {
  @Autowire
  private final RedisFlowLimitInterceptor redisLimitInterceptor;
  @Autowire
  private final RedisFlowLimitAspect redisLimitAspect;

  //RedisFlowLimitAspect的重置，AOP限流方式
  @ApiOperation(value = "流量限制器验证操作")
  @GetMapping(value = "/pb/sms/verify1")
  public BizResponse<Result<SmsSingleSend>> verifyCodeCheckRegister(HttpServletRequest request) {
    if (1 == doubleVerify(request)) {
      //清空计数器,因为使用的是对象适配器，所以拦截只能这么重置
      redisLimitInterceptor.getRedisFlowLimitAspect().resetLimiter(null);
      return BizResponse.ok(null);
    }
    return BizResponse.bizException(BizHttpStatusEnum.VERIFY_CODE_ERROR);
  }

  //RedisFlowLimitInterceptor，拦截器限流方式
  @ApiOperation(value = "流量限制器验证操作")
  @GetMapping(value = "/pb/sms/verify2")
  public BizResponse<Result<SmsSingleSend>> verifyCodeCheckRegister(HttpServletRequest request) {
    if (1 == doubleVerify(request)) {
      //清空计数器,AOP限流就比较简单
      redisLimitAspect.resetLimiter(null);
      return BizResponse.ok(null);
    }
    return BizResponse.bizException(BizHttpStatusEnum.VERIFY_CODE_ERROR);
  }
}
```

# 4. core核心类

核心类，即没有autoConfiguration的帮助，需要完全手动初始化Bean。只需要引入core包即可。

# 5. 实现原理

最最最核心的就是，顶级抽象类 AbstractFlowLimit.class

中，使用了模板方法，所有的限流流程都是基于这个抽象类的方法的~

```java
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
```





