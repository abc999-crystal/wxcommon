package com.wx.common.config;

import me.chanjar.weixin.cp.api.WxCpOAuth2Service;
import me.chanjar.weixin.cp.api.WxCpService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Description TODO
 * @Date 2025/1/13 17:23
 * @Version V1.0.0
 * @Author zdd55
 */
@Component
public class WxCpServiceFactory {

    @Resource
    private WxCpProperties properties;

    /**
     * 根据应用索引获取 WxCpService 实例
     *
     * @param appIndex 应用索引
     * @return WxCpService 实例
     */
    public WxCpService getWxCpService(int appIndex) {
        WxCpProperties.AppConfig appConfig = properties.getAppConfigs().get(appIndex);
        return WxCpConfiguration.getCpService(appConfig.getCorpId(), appConfig.getAgentId());
    }


    /**
     * 根据应用索引获取 WxCpOAuth2Service 实例，应是 应用 实例
     *
     * @param appIndex 应用索引
     * @return WxCpOAuth2Service 实例
     */
    public WxCpOAuth2Service getWxCpOAuth2Service(int appIndex) {
        return getWxCpService(appIndex).getOauth2Service();
    }

}
