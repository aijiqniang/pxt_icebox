package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
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
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxExamineExceptionReportMsg;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.ExportRecordsDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExamineExceptionReportDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxPutReportDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.service.IceBoxExamineExceptionReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.visit.client.FeignExportRecordsClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class IceBoxExamineExceptionReportServiceImpl extends ServiceImpl<IceBoxExamineExceptionReportDao, IceBoxExamineExceptionReport> implements IceBoxExamineExceptionReportService {

    @Autowired
    private IceBoxExamineExceptionReportDao iceBoxExamineExceptionReportDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private JedisClient jedis;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;

    @Override
    public IPage<IceBoxExamineExceptionReport> findByPage(IceBoxExamineExceptionReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = fillWrapper(reportMsg);
        IPage<IceBoxExamineExceptionReport> page = iceBoxExamineExceptionReportDao.selectPage(reportMsg, wrapper);
        return page;
    }

    @Override
    public CommonResponse<IceBoxExamineExceptionReport> sendExportMsg(IceBoxExamineExceptionReportMsg reportMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_EXCEPTION_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
//        if (null != jedis.get(key)) {
//            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
//        }
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(iceBoxExamineExceptionReportDao.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "冰柜异常报备信息-导出"));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            reportMsg.setOperateType(OperateTypeEnum.SELECT.getType());
            reportMsg.setRecordsId(recordsId);
            reportMsg.setOperateName(userManageVo.getSessionUserInfoVo().getRealname());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxExceptionReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper) {
        return iceBoxExamineExceptionReportDao.selectByExportCount(wrapper);
    }

    private LambdaQueryWrapper<IceBoxExamineExceptionReport> fillWrapper(IceBoxExamineExceptionReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = Wrappers.<IceBoxExamineExceptionReport>lambdaQuery();
        if(reportMsg.getGroupDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getGroupDeptId,reportMsg.getGroupDeptId());
        }
        if(reportMsg.getServiceDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getServiceDeptId,reportMsg.getServiceDeptId());
        }
        if(reportMsg.getRegionDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getRegionDeptId,reportMsg.getRegionDeptId());
        }
        if(reportMsg.getBusinessDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getBusinessDeptId,reportMsg.getBusinessDeptId());
        }
        if(reportMsg.getHeadquartersDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getHeadquartersDeptId,reportMsg.getHeadquartersDeptId());
        }
        if(StringUtils.isNotEmpty(reportMsg.getExamineNumber())){
            wrapper.like(IceBoxExamineExceptionReport::getExamineNumber,reportMsg.getExamineNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSupplierName())){
            wrapper.like(IceBoxExamineExceptionReport::getSupplierName,reportMsg.getSupplierName());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSupplierNumber())){
            wrapper.like(IceBoxExamineExceptionReport::getSupplierNumber,reportMsg.getSupplierNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSubmitterName())){
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.findUserIdsByUserName(reportMsg.getSubmitterName()));
            if(CollectionUtil.isNotEmpty(userIds)){
                wrapper.in(IceBoxExamineExceptionReport::getSubmitterId,userIds);
            }else {
                wrapper.eq(IceBoxExamineExceptionReport::getSubmitterId,"");
            }

        }
        if(reportMsg.getSubmitTime() != null){
            wrapper.ge(IceBoxExamineExceptionReport::getSubmitTime,reportMsg.getSubmitTime());
        }
        if(reportMsg.getSubmitEndTime() != null){
            wrapper.le(IceBoxExamineExceptionReport::getSubmitTime,reportMsg.getSubmitEndTime());
        }
        if(reportMsg.getToOaTime() != null){
            wrapper.ge(IceBoxExamineExceptionReport::getToOaTime,reportMsg.getToOaTime());
        }
        if(reportMsg.getToOaEndTime() != null){
            wrapper.le(IceBoxExamineExceptionReport::getToOaTime,reportMsg.getToOaEndTime());
        }
        if(reportMsg.getPutCustomerName() != null){
            wrapper.like(IceBoxExamineExceptionReport::getPutCustomerName,reportMsg.getPutCustomerName());
        }
        if(reportMsg.getPutCustomerNumber() != null){
            wrapper.like(IceBoxExamineExceptionReport::getPutCustomerNumber,reportMsg.getPutCustomerNumber());
        }
        if(reportMsg.getPutCustomerType() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getPutCustomerType,reportMsg.getPutCustomerType());
        }
        if(StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            wrapper.eq(IceBoxExamineExceptionReport::getIceBoxAssetId,reportMsg.getIceBoxAssetId());
        }
        if(reportMsg.getStatus() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getStatus,reportMsg.getStatus());
        }
        if(StringUtils.isNotEmpty(reportMsg.getToOaNumber())){
            wrapper.eq(IceBoxExamineExceptionReport::getToOaNumber,reportMsg.getToOaNumber());
        }
        return wrapper;
    }
}

