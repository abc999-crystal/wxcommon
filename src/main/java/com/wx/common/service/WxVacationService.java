package com.wx.common.service;

import com.wx.common.config.WxCpConfiguration;
import com.wx.common.config.WxCpProperties;
import com.wx.common.config.WxCpServiceFactory;
import com.wx.common.constant.Vacation;
import me.chanjar.weixin.cp.api.WxCpOAuth2Service;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpBaseResp;
import me.chanjar.weixin.cp.bean.oa.WxCpUserVacationQuota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @Description 更新假期服务
 * @Date 2025/1/10 13:22
 * @Version V1.0.0
 * @Author zdd55
 */
@Component
public class WxVacationService {
    private static final Logger logger = LoggerFactory.getLogger(WxContactService.class);

    // 应用
    private final WxCpService wxCpService;

    public WxVacationService(WxCpServiceFactory wxCpServiceFactory) {
        this.wxCpService = wxCpServiceFactory.getWxCpService(0);
    }

    /**
     * 更新调休假期余额
     *
     * @param wxUserId  用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param remark    备注
     * @return 更新结果
     */
    public WxCpBaseResp updateVacation(String wxUserId, String startTime, String endTime, String remark) {
        try {
            // 获取用户的调休假信息
            WxCpUserVacationQuota userVacationQuota = wxCpService.getOaService().getUserVacationQuota(wxUserId);
            WxCpUserVacationQuota.VacationQuota vacationQuota = findTiaoxiuVacationQuota(userVacationQuota.getLists());

            // 计算加班时长（单位：秒）
            long overWorkTimeMillis = parseDate(endTime).getTime() - parseDate(startTime).getTime();
            int overWorkTimeSeconds = roundToNearestMultiple((int) (overWorkTimeMillis / 1000), 360);
            logger.info("加班时长精确到0.1小时后: " + overWorkTimeSeconds + "秒");
            int leftDuration = vacationQuota.getLeftDuration() + overWorkTimeSeconds;

            // 检查备注长度
            if (remark.length() > 200) throw new IllegalArgumentException("备注长度不能超过200个字符");

            // 更新调休假
            logger.info("更新调休参数:{wxUserId:" + wxUserId
                    + "vacationId:" + vacationQuota.getId()
                    + "leftDuration:" + leftDuration
                    + "remark:" + remark
                    + "}");

            return wxCpService.getOaService().setOneUserQuota(wxUserId, vacationQuota.getId(), leftDuration, 1, remark);
        } catch (Exception e) {
            logger.error("更新调休失败，错误信息为: " + e.getMessage());
            throw new RuntimeException("企业微信未正确计入加班时常，请稍后重试。");
        }
    }

    /**
     * 查找调休假信息
     */
    private WxCpUserVacationQuota.VacationQuota findTiaoxiuVacationQuota(List<WxCpUserVacationQuota.VacationQuota> lists) {
        return lists.stream()
                .filter(vacationQuota -> Vacation.TIAOXIU_VACATION.equals(vacationQuota.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到调休假信息"));
    }

    /**
     * 将值四舍五入到最接近的指定倍数
     */
    private int roundToNearestMultiple(int value, int multiple) {
        int remainder = value % multiple;
        return (remainder < multiple / 2) ? value - remainder : value + (multiple - remainder);
    }

    /**
     * 解析时间字符串为 Date 对象
     */
    private Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(dateStr);
    }

}