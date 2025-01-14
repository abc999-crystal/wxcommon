package com.wx.common.service;

import com.wx.common.config.WxCpServiceFactory;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpOAuth2Service;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpOauth2UserInfo;
import me.chanjar.weixin.cp.bean.WxCpUserDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Description 企业微信授权服务
 * @Date 2025/1/13 17:15
 * @Version V1.0.0
 * @Author zdd55
 */
@Component
public class WxOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(WxContactService.class);

    private final WxCpService wxCpService;

    private final WxCpOAuth2Service oauth2Service;

    public WxOAuthService(WxCpServiceFactory wxCpServiceFactory) {
        this.wxCpService = wxCpServiceFactory.getWxCpService(0);
        this.oauth2Service = wxCpServiceFactory.getWxCpOAuth2Service(0);
    }

    /**
     * 根据授权码获取用户详细信息
     *
     * @param authCode
     * @return
     */
    public WxCpUserDetail getUserDetailByAuthCode(String authCode) {
        // 授权 获取用户信息
        WxCpOauth2UserInfo wxCpOauth2UserInfo = null;
        long startTime = System.currentTimeMillis();
        try {
            wxCpOauth2UserInfo = oauth2Service.getUserInfo(authCode);
        } catch (WxErrorException e) {
            logger.error("授权失败", e);
            throw new RuntimeException("授权失败[" + e.getMessage() + "]", e);
        }
        long endTime = System.currentTimeMillis();
        logger.info("授权耗时时长：{} ms", (endTime - startTime));
        // 获取用户详细信息
        WxCpUserDetail userDetail;
        try {
            userDetail = oauth2Service.getUserDetail(wxCpOauth2UserInfo.getUserTicket());
        } catch (WxErrorException e) {
            logger.error("获取用户信息失败", e);
            throw new RuntimeException("获取用户信息失败[" + e.getMessage() + "]", e);
        }
        return userDetail;
    }
}
