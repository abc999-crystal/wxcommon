package com.wx.common.handler;

import me.chanjar.weixin.cp.message.WxCpMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
public abstract class AbstractHandler implements WxCpMessageHandler {
    protected Logger logger = LoggerFactory.getLogger(getClass());
}
