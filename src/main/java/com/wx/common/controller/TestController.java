package com.wx.common.controller;

import com.wx.common.service.WxContactService;
import me.chanjar.weixin.cp.bean.WxCpUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description TODO
 * @Date 2025/1/13 17:37
 * @Version V1.0.0
 * @Author zdd55
 */
@RestController
@RequestMapping("/export")
public class TestController {

    @Autowired
    private WxContactService wxContactService;

    @RequestMapping("/getWxCpUserList")
    public List<WxCpUser> getWxCpUserList() {
        return wxContactService.getWxCpUserList();
    }
}
