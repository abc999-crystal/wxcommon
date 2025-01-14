package com.wx.common.service;

import com.wx.common.config.WxCpServiceFactory;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpUser;
import me.chanjar.weixin.cp.bean.user.WxCpDeptUserResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @Description 通讯录服务
 * @Date 2025/1/9 16:21
 * @Version V1.0.0
 * @Author zdd55
 */
@Component
public class WxContactService {

    private static final Logger logger = LoggerFactory.getLogger(WxContactService.class);

    // 通讯录服务
    private final WxCpService wxCpTXLService;
    // 应用服务
    private final WxCpService wxCpService;

    public WxContactService(WxCpServiceFactory wxCpServiceFactory) {
        this.wxCpTXLService = wxCpServiceFactory.getWxCpService(1);
        this.wxCpService = wxCpServiceFactory.getWxCpService(0);
    }

    /**
     * 获取指定部门及其下的子部门。
     *
     * @param deptId。非必需，可为null
     * @return
     */
    public List<WxCpDepart> getWxCpDeptList(Long deptId) {
        try {
            return wxCpService.getDepartmentService().list(deptId);
        } catch (WxErrorException e) {
            logger.error("获取企业微信部门列表失败，错误信息为: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取所有部门下的用户
     *
     * @return
     */
    public List<WxCpUser> getWxCpUserList() {
        // 获取所有部门下的用户id
        WxCpDeptUserResult wxCpDeptUserResult = null;
        try {
            wxCpDeptUserResult = wxCpTXLService.getUserService().getUserListId(null, 1000);
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
        List<WxCpDeptUserResult.DeptUserList> deptUserList = wxCpDeptUserResult.getDeptUser();

        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        List<WxCpUser> wxCpUserList = Collections.synchronizedList(new ArrayList<>());

        // 将用户列表分批处理
        List<CompletableFuture<List<WxCpUser>>> futures = deptUserList.stream()
                .map(userList -> CompletableFuture.supplyAsync(() -> {
                    List<WxCpUser> users = new ArrayList<>();
                    WxCpUser wxCpUser = fetchUserWithRetry(userList.getUserId(), wxCpService);
                    if (wxCpUser != null) {
                        users.add(wxCpUser);
                        logger.info("获取到用户: " + wxCpUser.getName() + "，userId: " + wxCpUser.getUserId());
                    }
                    return users;
                }, executorService))
                .collect(Collectors.toList());

        List<WxCpUser> finalWxCpUserList = wxCpUserList;
        futures.forEach(future -> {
            try {
                finalWxCpUserList.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        executorService.shutdown();

        //wxCpUserList去重，根据userId
        wxCpUserList = finalWxCpUserList.stream().
                collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(WxCpUser::getUserId))), ArrayList::new));

        return wxCpUserList;
    }

    // 重试机制
    public WxCpUser fetchUserWithRetry(String userId, WxCpService wxCpService) {
        int attempt = 0;
        WxCpUser wxCpUser = null;
        int maxRetry = 3;
        long retryDelay = 1000L;

        while (attempt < maxRetry) {
            try {
                wxCpUser = wxCpService.getUserService().getById(userId);
                return wxCpUser;
            } catch (Exception e) {
                attempt++;
                logger.warn("获取用户失败: {}, 重试次数: {}", userId, attempt);
                if (attempt >= maxRetry) {
                    logger.error("获取用户失败，达到最大重试次数: {}", userId);
                    return null;
                }
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("线程被中断，停止重试: {}", userId);
                    return null;
                }
            }
        }
        return null;
    }
}
