package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.commondb.config.annotation.RoutingDataSource;
import com.szeastroc.commondb.config.mybatis.Datasources;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SessionStoreInfoVo;
import com.szeastroc.customer.common.vo.SimpleStoreVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.config.XcxConfig;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.BackType;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.ServiceType;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.oldprocess.dao.WechatTransferOrderDao;
import com.szeastroc.icebox.oldprocess.entity.WechatTransferOrder;
import com.szeastroc.transfer.client.FeignTransferClient;
import com.szeastroc.transfer.common.enums.ResourceTypeEnum;
import com.szeastroc.transfer.common.enums.WechatPayTypeEnum;
import com.szeastroc.transfer.common.request.TransferRequest;
import com.szeastroc.transfer.common.response.TransferReponse;
import com.szeastroc.user.client.FeignCacheClient;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.vo.DeptNameRequest;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import com.szeastroc.user.common.vo.SessionUserInfoVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import com.szeastroc.visit.client.FeignOutBacklogClient;
import com.szeastroc.visit.client.FeignOutExamineClient;
import com.szeastroc.visit.common.NoticeBacklogRequestVo;
import com.szeastroc.visit.common.SessionExamineCreateVo;
import com.szeastroc.visit.common.SessionExamineVo;
import com.szeastroc.visit.common.SessionIceBoxRefundModel;
import com.szeastroc.visit.common.enums.NoticeTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBackOrderServiceImpl extends ServiceImpl<IceBackOrderDao, IceBackOrder> implements IceBackOrderService {

    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final IcePutApplyDao icePutApplyDao;
    private final IcePutOrderDao icePutOrderDao;
    private final IcePutPactRecordDao icePutPactRecordDao;
    private final IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    private final FeignOutBacklogClient feignOutBacklogClient;
    private final IceBackApplyRelateBoxDao iceBackApplyRelateBoxDao;
    private final IceBackOrderDao iceBackOrderDao;
    private final IceBackApplyDao iceBackApplyDao;
    private final FeignStoreClient feignStoreClient;
    private final FeignDeptClient feignDeptClient;
    private final FeignUserClient feignUserClient;
    private final FeignOutExamineClient feignOutExamineClient;
    private final WechatTransferOrderDao wechatTransferOrderDao;
    private final XcxConfig xcxConfig;
    private final FeignTransferClient feignTransferClient;
    private final FeignSupplierClient feignSupplierClient;
    private final FeignCacheClient feignCacheClient;
    private final IceModelDao iceModelDao;
    private IceTransferRecordDao iceTransferRecordDao;


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public void takeBackOrder(Integer iceBoxId) {
        // 校验
        validateTakeBack(iceBoxId);

        // TODO 由崔梦阳实现退还逻辑

        // 创建通知
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMdd");
        String blockName = "冰柜退押确认";
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        String putStoreNumber = iceBox.getPutStoreNumber();

        Map<String, SessionStoreInfoVo> map = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo(Collections.singletonList(putStoreNumber)));

        SessionStoreInfoVo sessionStoreInfoVo = map.get(putStoreNumber);

        Integer userId = sessionStoreInfoVo.getUserId();

        String assetId = iceBoxExtend.getAssetId();
        String relateCode = prefix + "_" + assetId;
        NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(blockName)
                .noticeTypeEnum(NoticeTypeEnum.ICEBOX_REFUND_CONFIRM)
                .relateCode(relateCode)
                .sendUserId(5941)
                .build();
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo) {

        // 退还编号
        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);
        Integer iceBoxId = simpleIceBoxDetailVo.getId();

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
                .eq(IcePutOrder::getChestId, iceBoxId));


        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        Integer backType = simpleIceBoxDetailVo.getBackType();

        IceBackOrder iceBackOrder = IceBackOrder.builder()
                .boxId(iceBoxId)
                .amount(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO)
                .applyNumber(applyNumber)
                .openid(icePutOrder.getOpenid())
                .putOrderId(icePutOrder.getId())
                .partnerTradeNo(icePutOrder.getOrderNum())
                .build();


        IceBackApplyRelateBox iceBackApplyRelateBox = IceBackApplyRelateBox.builder()
                .applyNumber(applyNumber)
                .backSupplierId(simpleIceBoxDetailVo.getSupplierId())
                .backType(backType)
                .freeType(icePutApplyRelateBox.getFreeType())
                .boxId(iceBoxId)
                .modelId(simpleIceBoxDetailVo.getChestModelId())
                .build();


        // 创建审批流

        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(simpleIceBoxDetailVo.getUserId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptId(simpleUserInfoVo.getSimpleDeptInfoVos().get(0).getId()));
//        List<Integer> userIds = new ArrayList<Integer>();
//        获取上级部门领导
//        SessionUserInfoVo userInfoVo1 = sessionUserInfoMap.get(1);
//        SessionUserInfoVo userInfoVo2 = sessionUserInfoMap.get(2);
//        SessionUserInfoVo userInfoVo3 = sessionUserInfoMap.get(2);
//        if (userInfoVo1 == null || userInfoVo2 == null || userInfoVo3 == null) {
//            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
//        }
//        userIds.add(userInfoVo1.getId());
//        userIds.add(userInfoVo2.getId());
//        userIds.add(userInfoVo3.getId());

        List<Integer> userIds = Arrays.asList(5941, 2103);
        SessionExamineVo sessionExamineVo = new SessionExamineVo();
        SessionIceBoxRefundModel sessionIceBoxRefundModel = new SessionIceBoxRefundModel();

        BeanUtils.copyProperties(simpleIceBoxDetailVo, sessionIceBoxRefundModel);

        SessionExamineCreateVo sessionExamineCreateVo = SessionExamineCreateVo.builder()
                .code(applyNumber)
                .relateCode(applyNumber)
                .createBy(simpleIceBoxDetailVo.getUserId())
                .userIds(userIds)
                .build();

        sessionExamineVo.setSessionExamineCreateVo(sessionExamineCreateVo);
        sessionExamineVo.setSessionIceBoxRefundModel(sessionIceBoxRefundModel);

        feignOutExamineClient.iceBoxRefund(sessionExamineVo);


        IceBackApply iceBackApply = IceBackApply.builder()
                .applyNumber(applyNumber)
                .backStoreNumber(simpleIceBoxDetailVo.getPutStoreNumber())
                .userId(simpleIceBoxDetailVo.getUserId())
                .createdBy(simpleIceBoxDetailVo.getUserId())
                .build();


        iceBackOrderDao.insert(iceBackOrder);

        iceBackApplyRelateBoxDao.insert(iceBackApplyRelateBox);

        iceBackApplyDao.insert(iceBackApply);


    }

    @Override
    public void doTransfer(String applyNumber) {

        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber, applyNumber));

        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));


        IceBackOrder iceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery().eq(IceBackOrder::getApplyNumber, applyNumber));


        Integer iceBoxId = iceBackApplyRelateBox.getBoxId();
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));


        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxId));


        IceTransferRecord iceTransferRecord = IceTransferRecord.builder()
                .applyNumber(applyNumber)
                .serviceType(ServiceType.IS_RETURN.getType())
                .boxId(iceBoxId)
                .supplierId(iceBackApplyRelateBox.getBackSupplierId())
                .storeNumber(iceBackApply.getBackStoreNumber())
                .transferMoney(iceBackOrder.getAmount())
                .applyUserId(iceBackApply.getUserId())
                .build();


        // 插入交易记录
        iceTransferRecordDao.insert(iceTransferRecord);

        // 更新冰柜状态
        iceBoxExtend.setLastPutTime(new Date());
        iceBoxExtend.setLastPutId(iceTransferRecord.getId());
        iceBoxExtend.setLastApplyNumber(applyNumber);
        iceBoxExtendDao.updateById(iceBoxExtend);

//         免押时, 不校验订单, 直接跳过
        if (FreePayTypeEnum.IS_FREE.getType() == icePutApplyRelateBox.getFreeType()) {
            return;
        }

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId));

        WechatTransferOrder wechatTransferOrder = new WechatTransferOrder(String.valueOf(icePutOrder.getId()), iceBoxId,
                icePutPactRecord.getId(), icePutOrder.getId(), icePutOrder.getOpenid(), icePutOrder.getPayMoney());

        log.info("wechatTransferOrder存入数据库 -> [{}]", JSON.toJSONString(wechatTransferOrder));
        wechatTransferOrderDao.insert(wechatTransferOrder);

        /**
         * 调用转账服务
         */
        TransferRequest transferRequest = TransferRequest.builder()
                .resourceType(ResourceTypeEnum.FROM_ICEBOX.getType())
                .resourceKey(String.valueOf(icePutOrder.getId()))
                .wxappid(xcxConfig.getAppid())
                .openid(icePutOrder.getOpenid())
//                .paymentAmount(orderInfo.getPayMoney().multiply(new BigDecimal(100)))
                .paymentAmount(icePutOrder.getPayMoney())
                .wechatPayType(WechatPayTypeEnum.FOR_TRANSFER.getType())
                .mchType(xcxConfig.getMchType())
                .build();

        TransferReponse transferReponse = FeignResponseUtil.getFeignData(feignTransferClient.transfer(transferRequest));

        // 修改冰柜状态
    }

    @RoutingDataSource(value = Datasources.SLAVE_DB)
    @Override
    public IPage findPage(IceBoxPage iceBoxPage) {
        Integer deptId = iceBoxPage.getDeptId(); // 营销区域id
        if (deptId != null) {
            DeptNameRequest request = new DeptNameRequest();
            request.setParentIds(deptId.toString());
            // 查询出当前部门下面的服务处
            List<SessionDeptInfoVo> deptInfoVos = FeignResponseUtil.getFeignData(feignDeptClient.findDeptInfoListByParentId(request));
            List<Integer> deptIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(deptInfoVos)) {
                deptInfoVos.forEach(i -> {
                    deptIds.add(i.getId());
                });
            }
            deptIds.add(deptId);
            iceBoxPage.setDeptIds(deptIds);
        }
        // 当所在对象编号或者所在对象名称不为空时,所在对象字段为必填
        if ((StringUtils.isNotBlank(iceBoxPage.getBelongObjNumber()) || StringUtils.isNotBlank(iceBoxPage.getBelongObjName()))
                && iceBoxPage.getBelongObj() == null) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "请选择所在对象类型");
        }
        Set<Integer> supplierIdList = new HashSet<>(); // 拥有者的经销商
        Set<String> putStoreNumberList = new HashSet<>(); // 投放的门店number

        // 所在对象  (put_status  投放状态 0: 未投放 1:已锁定(被业务员申请) 2:投放中 3:已投放; 当经销商时为 0-未投放;当门店时为非未投放状态;)
        String belongObjNumber = iceBoxPage.getBelongObjNumber();
        String belongObjName = iceBoxPage.getBelongObjName();
        String limit = " limit 30";
        // 所在对象为 经销商
        if (iceBoxPage.getBelongObj() != null && PutStatus.NO_PUT.getStatus() == iceBoxPage.getBelongObj()) {
            // supplier_type 客户类型：1-经销商，2-分销商，3-邮差，4-批发商
            // status 状态：0-禁用，1-启用
            if (StringUtils.isNotBlank(belongObjNumber)) { // 用 number 去查
                List<SubordinateInfoVo> infoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.getByNameOrNumber(null, belongObjNumber, 1, 1, limit));
                Optional.ofNullable(infoVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        supplierIdList.add(i.getId());
                    });
                });
            }
            if (StringUtils.isNotBlank(belongObjName)) { // 用 name 去查
                List<SubordinateInfoVo> infoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.getByNameOrNumber(belongObjName, null, 1, 1, limit));
                Optional.ofNullable(infoVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        supplierIdList.add(i.getId());
                    });
                });
            }
            if (CollectionUtils.isEmpty(supplierIdList)) {
                return null;
            }
        }
        // 所在对象为 门店
        if (iceBoxPage.getBelongObj() != null && PutStatus.NO_PUT.getStatus() != iceBoxPage.getBelongObj()) {
            if (StringUtils.isNotBlank(belongObjNumber)) { // 用 number 去查
                List<SimpleStoreVo> storeVoList = FeignResponseUtil.getFeignData(feignStoreClient.getByNameOrNumber(belongObjNumber, null, 1, null, limit));
                Optional.ofNullable(storeVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        putStoreNumberList.add(i.getStoreNumber());
                    });
                });
            }
            if (StringUtils.isNotBlank(belongObjName)) { // 用 name 去查
                List<SimpleStoreVo> storeVoList = FeignResponseUtil.getFeignData(feignStoreClient.getByNameOrNumber(null, belongObjName, 1, null, limit));
                Optional.ofNullable(storeVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        putStoreNumberList.add(i.getStoreNumber());
                    });
                });
            }
            if (CollectionUtils.isEmpty(putStoreNumberList)) {
                return null;
            }
        }

        List<IceBox> iceBoxList = iceBoxDao.findPage(iceBoxPage);
        if (CollectionUtils.isEmpty(iceBoxList)) {
            return null;
        }
        List<Integer> deptIds = iceBoxList.stream().map(IceBox::getDeptId).collect(Collectors.toList());
        // 营销区域对应得部门  服务处->大区->事业部
        Map<Integer, String> deptMap = null;
        if (CollectionUtils.isNotEmpty(deptIds)) {
            deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(deptIds));
        }
        // 设备型号
        List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery()
                .in(IceModel::getId, iceBoxList.stream().map(IceBox::getModelId).collect(Collectors.toSet())));
        Map<Integer, IceModel> modelMap = new HashMap<>();
        Optional.ofNullable(iceModels).ifPresent(list -> {
            list.forEach(i -> {
                modelMap.put(i.getId(), i);
            });
        });
        // 经销商 集合
        List<Integer> suppIds = iceBoxList.stream().filter(i -> i.getPutStatus().equals(PutStatus.NO_PUT.getStatus())).map(IceBox::getSupplierId).collect(Collectors.toList());
        Map<Integer, SubordinateInfoVo> suppMap = null;
        if (CollectionUtils.isNotEmpty(suppIds)) {
            suppMap = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(suppIds));
        }
        // 门店 集合
        Map<String, SimpleStoreVo> storeMap = null;
        List<String> storeNumbers = iceBoxList.stream().filter(i -> !i.getPutStatus().equals(PutStatus.NO_PUT.getStatus())).map(IceBox::getPutStoreNumber).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(storeNumbers)) {
            storeMap = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumberList(storeNumbers));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (IceBox iceBox : iceBoxList) {
            // t_ice_box 的id 和 t_ice_box_extend 的id是一一对应的
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("statusStr", IceBoxEnums.StatusEnum.getDesc(iceBox.getStatus())); // 设备状态
            String deptStr = null;
            if (deptMap != null) {
                deptStr = deptMap.get(iceBox.getDeptId()); // 营销区域
            }
            map.put("deptStr", deptStr); // 营销区域
            map.put("bluetoothId", iceBoxExtend.getBluetoothId()); // 设备编号 --蓝牙设备id
            map.put("chestName", iceBox.getChestName()); // 设备名称
            map.put("brandName", iceBox.getBrandName()); // 品牌
            IceModel iceModel = modelMap.get(iceBox.getId());
            map.put("chestModel", iceModel.getChestModel()); // 设备型号
            map.put("chestNorm", iceBox.getChestNorm()); // 规格
            map.put("lastPutTime", iceBoxExtend.getLastPutTime()); // 最近投放日期
            map.put("lastExamineTime", iceBoxExtend.getLastExamineTime()); // 最近巡检日期
            String lastApplyNumber = iceBoxExtend.getLastApplyNumber(); // 最近一次申请编号
            IcePutApplyRelateBox icePutApplyRelateBox = null;
            if (StringUtils.isNotBlank(lastApplyNumber)) {
                icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                        .eq(IcePutApplyRelateBox::getApplyNumber, lastApplyNumber).last(" limit 1"));
            }
            map.put("freeTypeStr", icePutApplyRelateBox == null ? null : FreePayTypeEnum.getDesc(icePutApplyRelateBox.getFreeType())); // 押金收取
            map.put("belongObjStr", iceBox.getPutStatus().equals(0) ? "经销商" : "门店"); // 所在客户类型
            String name = null;
            String number = null;
            if (PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus()) && suppMap != null) { // 经销商
                SubordinateInfoVo infoVo = suppMap.get(iceBox.getSupplierId());
                name = infoVo.getName();
                number = infoVo.getNumber();
            }
            if (!PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus()) && storeMap != null) { // 门店
                SimpleStoreVo storeVo = storeMap.get(iceBox.getPutStoreNumber());
                name = storeVo.getStoreName();
                number = storeVo.getStoreNumber();
            }
            map.put("brandName", number); // 客户编号
            map.put("brandName", name); // 客户名称
//            map.put("remark","暂无备注"); // 备注

            list.add(map);
        }

        return new Page(iceBoxPage.getCurrent(), iceBoxPage.getSize(), iceBoxPage.getTotal()).setRecords(list);
    }


    /**
     * takeBackIceChest注入对象及校验
     *
     * @param iceBoxId
     * @throws ImproperOptionException
     */
    private void validateTakeBack(Integer iceBoxId) throws ImproperOptionException, NormalOptionException {

        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);

        // 校验: 冰柜表中数据
        if (Objects.isNull(iceBox) || Objects.isNull(iceBoxExtend)) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        // 校验: 投放表中数据
        if (Objects.isNull(icePutApply)) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxId));

        // 校验: 电子协议
        if (icePutPactRecord == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD + ": 未找到对应的电子协议");
        }

        // 校验退还到期时间
        if (icePutPactRecord.getPutExpireTime().getTime() > new Date().getTime()) {
            throw new NormalOptionException(ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getCode(), ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getMessage());
        }

        // 免押时, 不校验订单, 直接跳过
        if (FreePayTypeEnum.IS_FREE.getType() == icePutApplyRelateBox.getFreeType()) {
            return;
        }

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId));
        /**
         * 校验: 订单号
         */
        if (Objects.isNull(icePutOrder)) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        if (!icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": 订单未完成");
        }
    }
}

