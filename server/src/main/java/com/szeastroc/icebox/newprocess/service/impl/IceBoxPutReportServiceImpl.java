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
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.ExportRecordsDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxPutReportDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.visit.client.FeignExportRecordsClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.List;
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
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;

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
//        if (null != jedis.get(key)) {
//            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
//        }
        LambdaQueryWrapper<IceBoxPutReport> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(iceBoxPutReportDao.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "冰柜投放信息-导出"));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            reportMsg.setOperateType(OperateTypeEnum.SELECT.getType());
            reportMsg.setRecordsId(recordsId);
            reportMsg.setOperateName(userManageVo.getSessionUserInfoVo().getRealname());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<IceBoxPutReport> wrapper) {
        return iceBoxPutReportDao.selectByExportCount(wrapper);
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
            wrapper.like(IceBoxPutReport::getSupplierName,reportMsg.getSupplierName());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSupplierNumber())){
            wrapper.like(IceBoxPutReport::getSupplierNumber,reportMsg.getSupplierNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSubmitterName())){
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.findUserIdsByUserName(reportMsg.getSubmitterName()));
            if(CollectionUtil.isNotEmpty(userIds)){
                wrapper.in(IceBoxPutReport::getSubmitterId,userIds);
            }else {
                wrapper.eq(IceBoxPutReport::getSubmitterId,"");
            }

        }
        if(reportMsg.getSubmitTime() != null){
            wrapper.ge(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitTime());
        }
        if(reportMsg.getSubmitEndTime() != null){
            wrapper.le(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitEndTime());
        }
        if(reportMsg.getPutCustomerName() != null){
            wrapper.like(IceBoxPutReport::getPutCustomerName,reportMsg.getPutCustomerName());
        }
        if(reportMsg.getPutCustomerNumber() != null){
            wrapper.like(IceBoxPutReport::getPutCustomerNumber,reportMsg.getPutCustomerNumber());
        }
        if(reportMsg.getPutCustomerType() != null){
            wrapper.eq(IceBoxPutReport::getPutCustomerType,reportMsg.getPutCustomerType());
        }
        if(StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            wrapper.eq(IceBoxPutReport::getIceBoxAssetId,reportMsg.getIceBoxAssetId());
        }
        if(reportMsg.getPutStatus() != null){
            if(PutStatus.DO_PUT.getStatus().equals(reportMsg.getPutStatus())){
                wrapper.and(x -> x.eq(IceBoxPutReport::getPutStatus,PutStatus.LOCK_PUT.getStatus()).or().eq(IceBoxPutReport::getPutStatus,PutStatus.DO_PUT.getStatus()));
            }else {
                wrapper.eq(IceBoxPutReport::getPutStatus,reportMsg.getPutStatus());
            }
        }
        return wrapper;
    }
}

