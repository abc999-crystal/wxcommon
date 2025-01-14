package com.wx.common.config;

import com.google.common.collect.Maps;
import com.wx.common.handler.*;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxRuntimeException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpRedissonConfigImpl;
import me.chanjar.weixin.cp.constant.WxCpConsts;
import me.chanjar.weixin.cp.message.WxCpMessageRouter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 多实例配置
 *
 * @author <a href="https://github.com/0katekate0">Wang_Wong</a>
 */
@Configuration
@EnableConfigurationProperties(WxCpProperties.class)
public class WxCpConfiguration {

    private static final Map<String, WxCpMessageRouter> routers = Maps.newHashMap();
    private static Map<String, WxCpService> cpServices = Maps.newHashMap();
    private final LogHandler logHandler;
    private final NullHandler nullHandler;
    private final LocationHandler locationHandler;
    private final MenuHandler menuHandler;
    private final MsgHandler msgHandler;
    private final ContactChangeHandler contactChangeHandler;
    private final UnsubscribeHandler unsubscribeHandler;
    private final SubscribeHandler subscribeHandler;
    private final WxCpProperties properties;
    private final RedissonClient redissonClient;

    @Autowired
    public WxCpConfiguration(LogHandler logHandler, NullHandler nullHandler, LocationHandler locationHandler,
                             MenuHandler menuHandler, MsgHandler msgHandler, ContactChangeHandler contactChangeHandler, UnsubscribeHandler unsubscribeHandler,
                             SubscribeHandler subscribeHandler, WxCpProperties properties, RedissonClient redissonClient) {
        this.logHandler = logHandler;
        this.nullHandler = nullHandler;
        this.locationHandler = locationHandler;
        this.menuHandler = menuHandler;
        this.msgHandler = msgHandler;
        this.contactChangeHandler = contactChangeHandler;
        this.unsubscribeHandler = unsubscribeHandler;
        this.subscribeHandler = subscribeHandler;

        this.properties = properties;
        this.redissonClient = redissonClient;
    }


    public static Map<String, WxCpMessageRouter> getRouters() {
        return routers;
    }

    public static WxCpService getCpService(String corpId, Integer agentId) {
        WxCpService cpService = cpServices.get(corpId + agentId);
        return Optional.ofNullable(cpService).orElseThrow(() -> new WxRuntimeException("未配置此service"));
    }

    @PostConstruct
    public void initServices() {
        cpServices = this.properties.getAppConfigs().stream().map(a -> {

            /**
             * 第二种方式，请参考： todo
             * https://github.com/binarywang/weixin-java-mp-demo/blob/master/src/main/java/com/github/binarywang/demo/wx/mp/config/WxMpConfiguration.java
             */
//            WxRedisOps redisTemplateOps = new RedisTemplateWxRedisOps(stringRedisTemplate);
//            WxCpRedisConfigImpl redisConfig = new WxCpRedisConfigImpl(todo);

            WxCpRedissonConfigImpl config = new WxCpRedissonConfigImpl(redissonClient, "workRedis:");
            config.setCorpId(a.getCorpId());
            config.setAgentId(a.getAgentId());
            config.setCorpSecret(a.getSecret());
            config.setToken(a.getToken());
            config.setAesKey(a.getAesKey());

            WxCpServiceImpl service = new WxCpServiceImpl();
            service.setWxCpConfigStorage(config);

            routers.put(a.getCorpId() + a.getAgentId(), this.newRouter(service));
            return service;
        }).collect(Collectors.toMap(service -> service.getWxCpConfigStorage().getCorpId() + service.getWxCpConfigStorage().getAgentId(), a -> a));
    }

    private WxCpMessageRouter newRouter(WxCpService wxCpService) {
        final WxCpMessageRouter newRouter = new WxCpMessageRouter(wxCpService);

        // 记录所有事件的日志 （异步执行）
        newRouter.rule().handler(this.logHandler).next();

        // 自定义菜单事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.MenuButtonType.CLICK).handler(this.menuHandler).end();

        // 点击菜单链接事件（这里使用了一个空的处理器，可以根据自己需要进行扩展）
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.MenuButtonType.VIEW).handler(this.nullHandler).end();

        // 关注事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.EventType.SUBSCRIBE).handler(this.subscribeHandler)
                .end();

        // 取消关注事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.EventType.UNSUBSCRIBE)
                .handler(this.unsubscribeHandler).end();

        // 上报地理位置事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.EventType.LOCATION).handler(this.locationHandler)
                .end();

        // 接收地理位置消息
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.LOCATION)
                .handler(this.locationHandler).end();

        // 扫码事件（这里使用了一个空的处理器，可以根据自己需要进行扩展）
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.EventType.SCAN).handler(this.nullHandler).end();

        // 通讯录变更事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxCpConsts.EventType.CHANGE_CONTACT).handler(this.contactChangeHandler).end();

        // 默认
        newRouter.rule().async(false).handler(this.msgHandler).end();

        return newRouter;
    }

}
