package com.wx.common.service;

import com.wx.common.config.WxCpConfiguration;
import com.wx.common.config.WxCpProperties;
import com.wx.common.utils.WxMsgUtil;
import me.chanjar.weixin.cp.api.WxCpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description 异步发送消息服务
 * @Date 2025/1/9 16:21
 * @Version V1.0.0
 * @Author zdd55
 */
@Component
public class WxMessageService {

    private static final Logger logger = LoggerFactory.getLogger(WxMessageService.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final WxCpProperties properties;

    public WxMessageService(WxCpProperties properties) {
        this.properties = properties;
    }

    public void sendWxTextMsgAsync(String msg, String toUserIds) {
        executorService.submit(() -> {
            String corpId = properties.getAppConfigs().get(0).getCorpId();
            Integer agentId = properties.getAppConfigs().get(0).getAgentId();
            WxCpService wxCpService = WxCpConfiguration.getCpService(corpId, agentId);
            long startTime = System.currentTimeMillis();
            try {
                WxMsgUtil.sendTextMsg(wxCpService, agentId, msg, toUserIds, null, null);
            } catch (Exception e) {
                logger.error("发送消息失败：" + msg, e);
            }
            long endTime = System.currentTimeMillis();
            logger.info("发送消息：" + msg + "成功；耗时时长：" + (endTime - startTime) + " ms");
        });
    }

}

