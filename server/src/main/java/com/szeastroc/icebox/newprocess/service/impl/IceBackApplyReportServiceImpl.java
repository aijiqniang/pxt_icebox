package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.newprocess.consumer.common.IceBackApplyReportMsg;
import com.szeastroc.icebox.newprocess.dao.IceBackApplyReportDao;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyReport;
import com.szeastroc.icebox.newprocess.service.IceBackApplyReportService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 冰柜退还表 (TIceBackApplyReport)表服务实现类
 *
 * @author chenchao
 * @since 2020-12-16 16:41:07
 */
@Service
public class IceBackApplyReportServiceImpl extends ServiceImpl<IceBackApplyReportDao, IceBackApplyReport> implements IceBackApplyReportService {
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private JedisClient jedis;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public IPage<IceBackApplyReport> findByPage(IceBackApplyReportMsg reportMsg) {
        LambdaQueryWrapper<IceBackApplyReport> wrapper = this.fillWrapper(reportMsg);
        return this.page(reportMsg, wrapper);
    }

    @Override
    public LambdaQueryWrapper<IceBackApplyReport> fillWrapper(IceBackApplyReportMsg reportMsg) {
        LambdaQueryWrapper<IceBackApplyReport> wrapper = Wrappers.<IceBackApplyReport>lambdaQuery();
        if(reportMsg.getGroupDeptId() != null){
            wrapper.eq(IceBackApplyReport::getGroupDeptId,reportMsg.getGroupDeptId());
        }
        if(reportMsg.getServiceDeptId() != null){
            wrapper.eq(IceBackApplyReport::getServiceDeptId,reportMsg.getServiceDeptId());
        }
        if(reportMsg.getRegionDeptId() != null){
            wrapper.eq(IceBackApplyReport::getRegionDeptId,reportMsg.getRegionDeptId());
        }
        if(reportMsg.getBusinessDeptId() != null){
            wrapper.eq(IceBackApplyReport::getBusinessDeptId,reportMsg.getBusinessDeptId());
        }
        if(reportMsg.getHeadquartersDeptId() != null){
            wrapper.eq(IceBackApplyReport::getHeadquartersDeptId,reportMsg.getHeadquartersDeptId());
        }
        if(StringUtils.isNotEmpty(reportMsg.getApplyNumber())){
            wrapper.eq(IceBackApplyReport::getApplyNumber,reportMsg.getApplyNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getDealerName())){
            wrapper.like(IceBackApplyReport::getDealerName,reportMsg.getDealerName());
        }
        if(StringUtils.isNotEmpty(reportMsg.getDealerNumber())){
            wrapper.eq(IceBackApplyReport::getDealerNumber,reportMsg.getDealerNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getBackCustomerName())){
            wrapper.like(IceBackApplyReport::getCustomerName,reportMsg.getBackCustomerName());
        }
        if(StringUtils.isNotEmpty(reportMsg.getBackCustomerNumber())){
            wrapper.eq(IceBackApplyReport::getCustomerNumber,reportMsg.getBackCustomerNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getStartTime())){
            wrapper.ge(IceBackApplyReport::getCreatedTime,reportMsg.getStartTime());
        }
        if(StringUtils.isNotEmpty(reportMsg.getEndTime())){
            wrapper.le(IceBackApplyReport::getCreatedTime,reportMsg.getEndTime());
        }
        if(StringUtils.isNotEmpty(reportMsg.getAssetId())){
            wrapper.eq(IceBackApplyReport::getAssetId,reportMsg.getAssetId());
        }
        if(reportMsg.getExamineStatus() != null){
            wrapper.eq(IceBackApplyReport::getExamineStatus,reportMsg.getExamineStatus());
        }
        wrapper.orderByDesc(IceBackApplyReport::getCreatedTime);
        return wrapper;
    }

    @Override
    public CommonResponse<IceBackApplyReport> sendExportMsg(IceBackApplyReportMsg reportMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_EXCEPTION_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
        }
        LambdaQueryWrapper<IceBackApplyReport> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(this.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "冰柜退还报表-导出"));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            reportMsg.setRecordsId(recordsId);
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceBackApplyReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<IceBackApplyReport> wrapper) {
         return this.count(wrapper);
    }
}