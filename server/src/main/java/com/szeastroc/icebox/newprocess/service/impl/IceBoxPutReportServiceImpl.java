package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.ExportRecordsDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxPutReportDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class IceBoxPutReportServiceImpl extends ServiceImpl<IceBoxPutReportDao, IceBoxPutReport> implements IceBoxPutReportService {

    @Autowired
    private IceBoxPutReportDao iceBoxPutReportDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private ExportRecordsDao exportRecordsDao;
    @Autowired
    private JedisClient jedis;

    @Override
    public IPage<IceBoxPutReport> findByPage(IceBoxPutReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxPutReport> wrapper = fillWrapper(reportMsg);
        IPage<IceBoxPutReport> page = iceBoxPutReportDao.selectPage(reportMsg, wrapper);
        return page;
    }

    @Override
    public CommonResponse<IceBoxPutReport> sendExportMsg(IceBoxPutReportMsg reportMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_PUT_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
        }
        LambdaQueryWrapper<IceBoxPutReport> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(exportRecordsDao.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务编号
        String serialNum = String.format("iceBoxPut%s", System.currentTimeMillis() / 1000);
        exportRecordsDao.insertExportRecords(serialNum, "冰柜投放信息-导出", userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), ExportRecordTypeEnum.PROCESSING.type, new Date());

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            reportMsg.setOperateType(OperateTypeEnum.SELECT.getType());
            reportMsg.setSerialNum(serialNum);
            reportMsg.setOperateName(userManageVo.getSessionUserInfoVo().getRealname());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return null;
    }

    @Getter
    @AllArgsConstructor
    private enum ExportRecordTypeEnum {
        PROCESSING(0, "处理中"),
        COMPLETED(1, "已完成");

        private Integer type;
        private String desc;
    }

    private LambdaQueryWrapper<IceBoxPutReport> fillWrapper(IceBoxPutReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxPutReport> wrapper = Wrappers.<IceBoxPutReport>lambdaQuery();
        if(reportMsg.getGroupDeptId() != null){
            wrapper.eq(IceBoxPutReport::getGroupDeptId,reportMsg.getGroupDeptId());
        }
        if(reportMsg.getServiceDeptId() != null){
            wrapper.eq(IceBoxPutReport::getServiceDeptId,reportMsg.getServiceDeptId());
        }
        if(reportMsg.getRegionDeptId() != null){
            wrapper.eq(IceBoxPutReport::getRegionDeptId,reportMsg.getRegionDeptId());
        }
        if(reportMsg.getBusinessDeptId() != null){
            wrapper.eq(IceBoxPutReport::getBusinessDeptId,reportMsg.getBusinessDeptId());
        }
        if(reportMsg.getHeadquartersDeptId() != null){
            wrapper.eq(IceBoxPutReport::getHeadquartersDeptId,reportMsg.getHeadquartersDeptId());
        }
        if(StringUtils.isNotEmpty(reportMsg.getApplyNumber())){
            wrapper.eq(IceBoxPutReport::getApplyNumber,reportMsg.getApplyNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSupplierName())){
            wrapper.and(x -> x.like(IceBoxPutReport::getSupplierName,reportMsg.getSupplierName()).or().like(IceBoxPutReport::getSupplierNumber,reportMsg.getSupplierNumber()));
        }
        if(reportMsg.getSubmitterId() != null){
            wrapper.eq(IceBoxPutReport::getSubmitterId,reportMsg.getSubmitterId());
        }
        if(reportMsg.getSubmitTime() != null){
            wrapper.ge(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitTime());
        }
        if(reportMsg.getSubmitEndTime() != null){
            wrapper.le(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitEndTime());
        }
        if(reportMsg.getPutCustomerName() != null){
            wrapper.and(x -> x.like(IceBoxPutReport::getPutCustomerName,reportMsg.getPutCustomerName()).or().like(IceBoxPutReport::getPutCustomerNumber,reportMsg.getPutCustomerNumber()));
        }
        if(reportMsg.getPutCustomerType() != null){
            wrapper.eq(IceBoxPutReport::getPutCustomerType,reportMsg.getPutCustomerType());
        }
        if(StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            wrapper.eq(IceBoxPutReport::getIceBoxAssetId,reportMsg.getIceBoxAssetId());
        }
        return wrapper;
    }
}

