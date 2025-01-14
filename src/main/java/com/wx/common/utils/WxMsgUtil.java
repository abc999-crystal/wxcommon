package com.wx.common.utils;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpMessageServiceImpl;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;

/**
 * @Auther zdd
 * @Date 2025/01/10 13:47
 * @Version v1.0.0
 * @Description MsgSendUtil
 */
public class WxMsgUtil {
    /**
     * 发送文本消息
     *
     * @param agentId     企业号ID
     * @param content     消息内容
     * @param toUser      非必填，UserID列表（消息接收者，多个接收者用‘|’分隔）。特殊情况：指定为@all，则向关注该企业应用的全部成员发送
     * @param toParty     非必填，PartyID列表，多个接受者用‘|’分隔。当toUser为@all时忽略本参数
     * @param toTag       非必填，TagID列表，多个接受者用‘|’分隔。当toUser为@all时忽略本参数
     * @param wxCpService 微信企业号服务
     * @throws WxErrorException 如果发生微信异常
     */
    public static void sendTextMsg(WxCpService wxCpService,
                                   Integer agentId,
                                   String content,
                                   String toUser,
                                   String toParty,
                                   String toTag) throws WxErrorException {
        // 创建WxCpMessageServiceImpl实例
        WxCpMessageServiceImpl wxCpMessageService = new WxCpMessageServiceImpl(wxCpService);
        // 创建WxCpMessage实例
        WxCpMessage wxCpMessage = WxCpMessage
                .TEXT() // 创建文本消息
                .agentId(agentId) // 企业号应用ID
                .toUser(toUser)
                .toParty(toParty)
                .toTag(toTag)
                .content(content) // 消息内容
                .build(); // 构建消息
        // 发送消息
        wxCpMessageService.send(wxCpMessage);
    }

    public static void sendFileMsg(WxCpService wxCpService,
                                   Integer agentId,
                                   String toUser,
                                   String toParty,
                                   String toTag,
                                   String mediaId
    ) throws WxErrorException {
        // 创建WxCpMessageServiceImpl实例
        WxCpMessageServiceImpl wxCpMessageService = new WxCpMessageServiceImpl(wxCpService);
        WxCpMessage wxCpMessage = WxCpMessage
                .FILE()
                .agentId(agentId) // 企业号应用ID
                .toUser(toUser)
                .toParty(toParty)
                .toTag(toTag)
                .mediaId(mediaId)
                .build();
        // 发送消息
        wxCpMessageService.send(wxCpMessage);
    }
}
