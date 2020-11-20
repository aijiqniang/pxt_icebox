package com.szeastroc.icebox.newprocess.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxTransferHistoryDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxTransferHistory;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.ExamineStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import com.szeastroc.icebox.newprocess.vo.IceBoxTransferHistoryPageVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxTransferHistoryVo;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import com.szeastroc.icebox.rabbitMQ.DataPack;
import com.szeastroc.icebox.rabbitMQ.DirectProducer;
import com.szeastroc.icebox.rabbitMQ.MethodNameOfMQ;
import com.szeastroc.icebox.util.NewExcelUtil;
import com.szeastroc.user.client.FeignCacheClient;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.client.FeignExportRecordsClient;
import com.szeastroc.visit.common.SessionExamineVo;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IceBoxTransferHistoryServiceImpl extends ServiceImpl<IceBoxTransferHistoryDao, IceBoxTransferHistory> implements IceBoxTransferHistoryService {


    @Autowired
    private IceBoxTransferHistoryDao iceBoxTransferHistoryDao;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private FeignExamineClient feignExamineClient;
    @Autowired
    private FeignDeptClient feignDeptClient;
    @Autowired
    private FeignCacheClient feignCacheClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private DirectProducer directProducer;


    @Override
    public IPage<IceBoxTransferHistoryPageVo> report(IceTransferRecordPage iceTransferRecordPage) {

        LambdaQueryWrapper<IceBoxTransferHistory> wrapper = buildWrapper(iceTransferRecordPage);
        IPage<IceBoxTransferHistory> iPage = iceBoxTransferHistoryDao.selectPage(iceTransferRecordPage, wrapper);
        IPage<IceBoxTransferHistoryPageVo> page = new Page<>();
        page = iPage.convert(this::buildPageVo);
        return page;
    }

    @Override
    public void reportExport(IceTransferRecordPage iceTransferRecordPage) {

        // 从session 中获取用户信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        Integer userId = userManageVo.getSessionUserInfoVo().getId();
        String userName = userManageVo.getSessionUserInfoVo().getRealname();
        // 控制导出的请求频率
        String key = "ice_transfer_export_excel_" + userId;
        String value = jedisClient.get(key);
//        if (StringUtils.isNotBlank(value)) {
//            throw new NormalOptionException(Constants.API_CODE_FAIL, "请到“首页-下载任务”中查看导出结果，请勿频繁操作(间隔3分钟)...");
//        }
        jedisClient.setnx(key, userId.toString(), 180);
        // 塞入数据到下载列表中  exportRecordId
        Integer exportRecordId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userId, userName, JSON.toJSONString(iceTransferRecordPage), "冰柜转移明细导出"));
        iceTransferRecordPage.setExportRecordId(exportRecordId);
        // 塞入部门集合
        DataPack dataPack = new DataPack(); // 数据包
        dataPack.setMethodName(MethodNameOfMQ.EXPORT_ICE_TRANSFER);
        dataPack.setObj(iceTransferRecordPage);
        directProducer.sendMsg(MqConstant.directRoutingKey, dataPack);
    }

    @Override
    public List<IceBoxTransferHistory> findByIceBoxId(Integer iceBoxId) {

        return null;
    }

    @Override
    public void exportTransferHistory(IceTransferRecordPage iceTransferRecordPage) {
        try {
            LambdaQueryWrapper<IceBoxTransferHistory> wrapper = buildWrapper(iceTransferRecordPage);
            List<IceBoxTransferHistory> iceBoxTransferHistoryList = iceBoxTransferHistoryDao.selectList(wrapper);
            List<IceBoxTransferHistoryPageVo> iceBoxTransferHistoryPageVos = new ArrayList<IceBoxTransferHistoryPageVo>();
            iceBoxTransferHistoryList.forEach(iceBoxTransferHistory -> iceBoxTransferHistoryPageVos.add(buildPageVo(iceBoxTransferHistory)));
            String fileName = "冰柜转移明细表";
            String titleName = "冰柜转移明细表";
            NewExcelUtil<IceBoxTransferHistoryPageVo> newExcelUtil = new NewExcelUtil<>();
            newExcelUtil.asyncExportExcelOther(fileName, titleName, iceBoxTransferHistoryPageVos, iceTransferRecordPage.getExportRecordId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<IceBoxTransferHistoryVo> findListBySupplierId(Integer supplierId) {
        LambdaQueryWrapper<IceBoxTransferHistory> wrappers = Wrappers.lambdaQuery();
        wrappers.eq(IceBoxTransferHistory::getOldSupplierId, supplierId);
        List<IceBoxTransferHistory> historyList = iceBoxTransferHistoryDao.selectList(wrappers);
        List<IceBoxTransferHistoryVo> historyVoList = new ArrayList<>();
        if (CollectionUtil.isEmpty(historyList)) {
            return historyVoList;
        }
        Map<String, List<IceBoxTransferHistory>> historyMap = historyList.stream().collect(Collectors.groupingBy(IceBoxTransferHistory::getTransferNumber));
        for (String transferNumber : historyMap.keySet()) {
            IceBoxTransferHistoryVo historyVo = new IceBoxTransferHistoryVo();
            List<IceBoxTransferHistory> list = historyMap.get(transferNumber);
            if (CollectionUtil.isEmpty(list)) {
                continue;
            }
            IceBoxTransferHistory history = list.get(0);
            BeanUtil.copyProperties(history, historyVo);
            List<Integer> iceBoxIds = list.stream().map(x -> x.getIceBoxId()).collect(Collectors.toList());
            List<IceBox> iceBoxList = iceBoxDao.selectBatchIds(iceBoxIds);
            historyVo.setIceBoxs(iceBoxList);
            if (history.getIsCheck().equals(1)) {
                List<SessionExamineVo.VisitExamineNodeVo> examineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(transferNumber));
                historyVo.setExamineNodeVoList(examineNodeVos);
            }
            historyVoList.add(historyVo);
        }
        return historyVoList;
    }


    private LambdaQueryWrapper<IceBoxTransferHistory> buildWrapper(IceTransferRecordPage iceTransferRecordPage) {
        LambdaQueryWrapper<IceBoxTransferHistory> wrapper = Wrappers.lambdaQuery();
        Integer deptId = iceTransferRecordPage.getDeptId();
        String assetId = iceTransferRecordPage.getAssetId();

        if (null != deptId) {
            SessionDeptInfoVo sessionDeptInfoVo = FeignResponseUtil.getFeignData(feignCacheClient.getForDeptInfoVo(deptId));
            Integer deptType = sessionDeptInfoVo.getDeptType();

            if (DeptTypeEnum.SERVICE.getType().equals(deptType)) {
                wrapper.eq(IceBoxTransferHistory::getServiceDeptId, deptId);
            }
            if (DeptTypeEnum.LARGE_AREA.getType().equals(deptType)) {
                wrapper.eq(IceBoxTransferHistory::getRegionDeptId, deptId);
            }
            if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(deptType)) {
                wrapper.eq(IceBoxTransferHistory::getBusinessDeptId, deptId);
            }
            if (DeptTypeEnum.THIS_PART.getType().equals(deptType)) {
                wrapper.eq(IceBoxTransferHistory::getHeadquartersDeptId, deptId);
            }
            if (DeptTypeEnum.GROUP.getType().equals(deptType)) {
                wrapper.eq(IceBoxTransferHistory::getGroupDeptId, deptId);
            }

        }

        if (StringUtils.isNotBlank(assetId)) {
            wrapper.eq(IceBoxTransferHistory::getAssetId, assetId);
        }
        String oldSupplierName = iceTransferRecordPage.getOldSupplierName();
        String oldSupplierNumber = iceTransferRecordPage.getOldSupplierNumber();
        String newSupplierName = iceTransferRecordPage.getNewSupplierName();
        String newSupplierNumber = iceTransferRecordPage.getNewSupplierNumber();

        if (StringUtils.isNotBlank(oldSupplierName)) {
            wrapper.like(IceBoxTransferHistory::getOldSupplierName, oldSupplierName);
        }

        if (StringUtils.isNotBlank(newSupplierName)) {
            wrapper.like(IceBoxTransferHistory::getNewSupplierName, newSupplierName);
        }

        if (StringUtils.isNotBlank(oldSupplierNumber)) {
            wrapper.like(IceBoxTransferHistory::getOldSupplierNumber, oldSupplierNumber);
        }
        if (StringUtils.isNotBlank(newSupplierNumber)) {
            wrapper.like(IceBoxTransferHistory::getNewSupplierNumber, newSupplierNumber);
        }

        Date startTime = iceTransferRecordPage.getStartTime();
        Date endTime = iceTransferRecordPage.getEndTime();


        if (null != startTime) {
            wrapper.ge(IceBoxTransferHistory::getCreateTime, startTime);
        }

        if (null != endTime) {
            wrapper.le(IceBoxTransferHistory::getCreateTime, endTime);
        }

        String createBy = iceTransferRecordPage.getCreateByName();

        if (StringUtils.isNotBlank(createBy)) {
            wrapper.like(IceBoxTransferHistory::getCreateByName, createBy);
        }

        Integer examineStatus = iceTransferRecordPage.getExamineStatus();

        if (null != examineStatus) {
            wrapper.eq(IceBoxTransferHistory::getExamineStatus, examineStatus);
        }
        return wrapper;
    }

    private IceBoxTransferHistoryPageVo buildPageVo(IceBoxTransferHistory iceBoxTransferHistory) {
        Integer iceBoxId = iceBoxTransferHistory.getIceBoxId();
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        IceBoxTransferHistoryPageVo iceBoxTransferHistoryPageVo = new IceBoxTransferHistoryPageVo();
        iceBoxTransferHistoryPageVo.setAssetId(iceBox.getAssetId());
        iceBoxTransferHistoryPageVo.setModelName(iceBox.getModelName());
        iceBoxTransferHistoryPageVo.setDepositMoney(iceBox.getDepositMoney());
        iceBoxTransferHistoryPageVo.setExamineStatusStr(ExamineStatus.convertVo(iceBoxTransferHistory.getExamineStatus()));

        Integer oldMarketAreaId = iceBoxTransferHistory.getOldMarketAreaId();
        Integer newMarketAreaId = iceBoxTransferHistory.getNewMarketAreaId();

        Map<Integer, SessionDeptInfoVo> oldDeptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(oldMarketAreaId));
        Map<Integer, SessionDeptInfoVo> newDeptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(newMarketAreaId));
        iceBoxTransferHistoryPageVo.setOldServiceDept(oldDeptMap.get(2).getName());
        iceBoxTransferHistoryPageVo.setOldRegionDept(oldDeptMap.get(3).getName());
        iceBoxTransferHistoryPageVo.setOldBusinessDept(oldDeptMap.get(4).getName());

        iceBoxTransferHistoryPageVo.setNewServiceDept(newDeptMap.get(2).getName());
        iceBoxTransferHistoryPageVo.setNewRegionDept(newDeptMap.get(3).getName());
        iceBoxTransferHistoryPageVo.setNewBusinessDept(newDeptMap.get(4).getName());

        Integer oldSupplierId = iceBoxTransferHistory.getOldSupplierId();
        Integer newSupplierId = iceBoxTransferHistory.getNewSupplierId();

        List<Integer> list = new ArrayList<>();

        list.add(oldSupplierId);
        list.add(newSupplierId);
        Map<Integer, SubordinateInfoVo> subordinateInfoVoMap = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(list));

        iceBoxTransferHistoryPageVo.setOldSupplierName(subordinateInfoVoMap.get(oldSupplierId).getName());
        iceBoxTransferHistoryPageVo.setOldSupplierNumber(subordinateInfoVoMap.get(oldSupplierId).getNumber());

        iceBoxTransferHistoryPageVo.setNewSupplierName(subordinateInfoVoMap.get(newSupplierId).getName());
        iceBoxTransferHistoryPageVo.setNewSupplierNumber(subordinateInfoVoMap.get(newSupplierId).getNumber());

        iceBoxTransferHistoryPageVo.setCreateByName(iceBoxTransferHistory.getCreateByName());

        iceBoxTransferHistoryPageVo.setCreateTimeStr(iceBoxTransferHistory.getCreateTime() != null ? new DateTime(iceBoxTransferHistory.getCreateTime()).toString("yyyy-MM-dd HH:mm:ss") : "");
        iceBoxTransferHistoryPageVo.setReviewer(iceBoxTransferHistory.getReviewerName());
        iceBoxTransferHistoryPageVo.setReviewTimeStr(iceBoxTransferHistory.getReviewerTime() != null ? new DateTime(iceBoxTransferHistory.getReviewerTime()).toString("yyyy-MM-dd HH:mm:ss") : "");
        return iceBoxTransferHistoryPageVo;
    }

}
