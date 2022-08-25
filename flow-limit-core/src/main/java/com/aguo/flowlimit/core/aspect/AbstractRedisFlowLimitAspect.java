package com.aguo.flowlimit.core.aspect;

import com.aguo.flowlimit.core.AbstractFlowLimit;
import com.aguo.flowlimit.core.utils.FlowLimitCacheHelper;
import com.aguo.flowlimit.core.utils.ShowUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/7/25 10:33
 * @Description: Redis数据源，计数器的方式限流。
 */
public abstract class AbstractRedisFlowLimitAspect extends AbstractFlowLimit<JoinPoint>
        implements IFlowLimitAspect<JoinPoint> {
    private TimeUnit timeUnit;
    private FlowLimitCacheHelper redisHelper;
    /**
     * 是否全局限制，即所有用户所有操作均被计数限制.
     * <br/>
     * FALSE则需要传递获取用户ID的方法。
     */
    private boolean enabledGlobalLimit;
    /**
     * baseKey，即全局key前缀
     * <br/>形式：
     * 注意：这里的每个key已经加到counterKey中
     */
    private String prefixKey;
    /**
     * 每个计数器的Key。
     * <pre>
     * 注意：这里的每个key已经有全局的前缀prefixKey
     *  <pre/>
     */
    private List<String> counterKeys;
    /**
     * 每个计数器的保持时长，单位是毫秒
     */
    private List<Long> counterHoldingTime;
    /**
     * 每个计数器对应的限流次数，即接口调用次数限制
     */
    private List<Integer> counterLimitNumber;

    public AbstractRedisFlowLimitAspect setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public AbstractRedisFlowLimitAspect setRedisHelper(FlowLimitCacheHelper redisHelper) {
        this.redisHelper = redisHelper;
        return this;
    }

    public AbstractRedisFlowLimitAspect setEnabledGlobalLimit(boolean enabledGlobalLimit) {
        this.enabledGlobalLimit = enabledGlobalLimit;
        return this;
    }

    public AbstractRedisFlowLimitAspect setPrefixKey(String prefixKey) {
        this.prefixKey = prefixKey;
        return this;
    }

    public AbstractRedisFlowLimitAspect setCounterKeys(List<String> counterKeys) {
        this.counterKeys = counterKeys;
        return this;
    }

    public AbstractRedisFlowLimitAspect setCounterHoldingTime(List<Long> counterHoldingTime) {
        this.counterHoldingTime = counterHoldingTime;
        return this;
    }

    public AbstractRedisFlowLimitAspect setCounterLimitNumber(List<Integer> counterLimitNumber) {
        this.counterLimitNumber = counterLimitNumber;
        return this;
    }

    public AbstractRedisFlowLimitAspect() {

    }

    /**
     * bean的初始化,构建本bean对象。务必最后调用
     *
     * @return this
     */
    public AbstractRedisFlowLimitAspect build() {
        initCounterKeys();
        if (enabledFlowLimit()) {
            ShowUtil.showBanner();
        }
        return this;
    }

    /**
     * 对公共计数器key进行拼接
     */
    private void initCounterKeys() {
        String appendKeyWithMode = appendCounterKeyWithMode();
        this.counterKeys = Optional.ofNullable(this.counterKeys)
                .map(keys -> keys.stream().map(key -> prefixKey + key + appendKeyWithMode).collect(Collectors.toList()))
                .orElse(getCounterKeysUseUUID());
    }

    /**
     * 如果配置文件中没有配置counter的key，那么则会使用UUID作为key
     *
     * @return 拼接完成的key
     */
    private ArrayList<String> getCounterKeysUseUUID() {
        String appendKeyWithMode = appendCounterKeyWithMode();
        ArrayList<String> keys = new ArrayList<>();
        for (int i = 0; i < this.counterHoldingTime.size(); i++) {
            keys.add(prefixKey + "flowlimit:" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5) + ":" + appendKeyWithMode);
        }
        return keys;
    }


    /**
     * 追加模式，有AOP模式和拦截器模式。前面要有个分号
     *
     * @return 模式
     */
    public String appendCounterKeyWithMode() {
        return "aspect:";
    }

    /**
     * 判断流量限制是否能正常开启。
     *
     * @return true 开启
     */
    private boolean enabledFlowLimit() {
        boolean enabled = redisHelper != null &&
                Optional.ofNullable(counterKeys).map(key -> !key.isEmpty()).orElse(false) &&
                Optional.ofNullable(counterHoldingTime)
                        .map(cht -> cht.size() == Optional.ofNullable(counterLimitNumber)
                                .map(List::size)
                                .orElse(-1))
                        .orElse(false);
        setEnabled(enabled);
        return enabled;
    }

    /**
     * 定义增强方式，默认使用环绕增强
     * <br/>
     * 不建议子类重写。如需重写，则<strong>必须</strong>回调父类的 flowLimitProcess(joinPoint)方法！
     */
    @Around("pointcut()")
    public Object adviceMode(JoinPoint joinPoint) throws Throwable {
        return this.flowLimitProcess(joinPoint);
    }

    /**
     * 限流逻辑
     *
     * @param joinPoint 连接点
     * @return TRUE 限流
     */
    @Override
    public final boolean limitProcess(JoinPoint joinPoint) {
        List<String> counterKey = getFinalCounterKeys(joinPoint);
        //当前计数器是否限制？
        boolean currentIsLimit = false;
        //遍历计数器
        for (int i = 0; i < counterKey.size() && !currentIsLimit; i++) {
            currentIsLimit = counterProcess(counterKey.get(i), counterHoldingTime.get(i), counterLimitNumber.get(i));
        }
        //当且仅当所有计数器都返回false才不限制
        return currentIsLimit;
    }

    /**
     * 如果开启全局限制，那么会拼接用户的ID作为key
     *
     * @param joinPoint 连接点
     * @return 最终的Key
     */
    private List<String> getFinalCounterKeys(JoinPoint joinPoint) {
        if (!enabledGlobalLimit) {
            //未开启全局计数，即计数器要拼接的用户ID，对每一个用户单独限流
            String userId = appendCounterKeyWithUserId(joinPoint);
            if (StringUtils.hasText(userId)) {
                return this.counterKeys.stream()
                        .map(key ->
                                key.concat("userId:").concat(Optional
                                        .ofNullable(userId)
                                        .orElse("")))
                        .collect(Collectors.toList());
            }
        }
        return this.counterKeys;
    }

    /**
     * 重构计数器的key，未开启全局计数，即计数器要拼接的用户ID，对每一个用户单独限流
     *
     * @param joinPoint 连接点
     * @return 重构逻辑
     */
    protected abstract String appendCounterKeyWithUserId(JoinPoint joinPoint);

    /**
     * 对key进行细粒的操作,即计数器自增
     * 会用LUA脚本实现,如果Redis宕机，那么会拦截所有请求。
     *
     * @param key      当前key
     * @param timeout  超时时间
     * @param countMax 最大计数
     * @return ture 当前key的计数器超出限制，禁止访问
     */
    private boolean counterProcess(String key, Long timeout, Integer countMax) {
        return redisHelper.increaseKeySafely(key, Math.max(timeUnit.toMillis(timeout), 1L), countMax);
    }


    /**
     * 重置所有的流量限制的计数器
     */
    @Override
    public final Object resetLimiter(JoinPoint joinPoint) {
        for (String key : getFinalCounterKeys(joinPoint)) {
            redisHelper.deleteKey(key);
        }
        return null;
    }


}
