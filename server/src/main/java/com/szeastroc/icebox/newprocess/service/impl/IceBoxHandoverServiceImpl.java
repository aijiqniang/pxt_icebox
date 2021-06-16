package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.*;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.UserInfoVo;
import com.szeastroc.common.entity.visit.NoticeBacklogRequestVo;
import com.szeastroc.common.entity.visit.enums.NoticeTypeEnum;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignStoreRelateMemberClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.feign.visit.FeignOutBacklogClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.ExamineStatus;
import com.szeastroc.icebox.newprocess.enums.HandOverEnum;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxHandoverService;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxHandoverPage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxHandoverServiceImpl extends ServiceImpl<IceBoxHandoverDao, IceBoxHandover>
implements IceBoxHandoverService{

    private final IceBoxHandoverDao iceBoxHandoverDao;
    private final FeignStoreClient feignStoreClient;
    private final FeignSupplierClient feignSupplierClient;
    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtenddao;
    private final FeignStoreRelateMemberClient feignStoreRelateMemberClient;
    private final IcePutOrderDao icePutOrderDao;
    private final IceBackApplyRelateBoxDao iceBackApplyRelateBoxDao;
    private final IceBackApplyDao iceBackApplyDao;
    private final ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    private final FeignOutBacklogClient feignOutBacklogClient;
    private final FeignUserClient feignUserClient;
    private final FeignExportRecordsClient feignExportRecordsClient;
    private final JedisClient jedis;
    private final RabbitTemplate rabbitTemplate;
    private final IceModelDao iceModelDao;
    private final IceExamineDao iceExamineDao;

    @Override
    public Map<String, List<Map<String,Object>>> findByUserid(Integer userId,Integer receiveUserId, String storeName) {
        Map<String, List<Map<String,Object>>> map = new HashMap<>(16);

        /**
         * 门店信息
         */
        StoreInfoDtoVo requestVo = new StoreInfoDtoVo();
        if(StringUtils.isNotEmpty(storeName)){
            requestVo.setStoreName(storeName);
        }
        requestVo.setMainSaleManId(userId);
        List<StoreInfoDtoVo> storeInfoDtoVos = new ArrayList<>();
        Object data = FeignResponseUtil.getFeignData(feignStoreClient.getStoreInfo(requestVo));
        if (data instanceof ArrayList<?>) {
            for (Object o : (List<?>) data) {
                storeInfoDtoVos.add(JSON.parseObject(JSON.toJSONString(o),StoreInfoDtoVo.class));
            }
        }

        if(storeInfoDtoVos.size()>0){
            List<StoreInfoDtoVo> ownStores = storeInfoDtoVos.stream().filter(storeInfoDtoVo -> userId.equals(storeInfoDtoVo.getMainSaleManId())).collect(Collectors.toList());
            if(ownStores.size()>0){
                for(StoreInfoDtoVo storeInfoDtoVo : ownStores){
                    MemberInfoVo memberInfoVo = FeignResponseUtil.getFeignData(feignStoreRelateMemberClient.getShopKeeper(storeInfoDtoVo.getStoreNumber()));
                    List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, storeInfoDtoVo.getStoreNumber()));
                    if(iceBoxes.size()>0){
                        List<Map<String,Object>> secondList = new ArrayList();
                        for (IceBox iceBox : iceBoxes){

                            Map thirdMap = new HashMap(8);



                            /**
                             * 传了接收人 是在通知那里进来的
                             */
                            if(receiveUserId != null && receiveUserId > 0){
                                IceBoxHandover iceBoxHandover1 = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getIceBoxId, iceBox.getId()).eq(IceBoxHandover::getSendUserId, userId).orderByDesc(IceBoxHandover::getId).eq(IceBoxHandover::getReceiveUserId,receiveUserId).eq(IceBoxHandover::getHandoverStatus,1).last("limit 1"));
                                if(iceBoxHandover1 == null){
                                    continue;
                                }else{
                                    thirdMap.put("handoverStatus",iceBoxHandover1.getHandoverStatus());
                                    thirdMap.put("handoverId",iceBoxHandover1.getId());
                                }
                            }else{
                                /**
                                 * 1交接中 2已交接 3已驳回  0就是未交接
                                 */
                                thirdMap.put("handoverStatus",0);
                                IceBoxHandover iceBoxHandover = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getIceBoxId, iceBox.getId()).eq(IceBoxHandover::getSendUserId, userId).orderByDesc(IceBoxHandover::getId).eq(IceBoxHandover::getHandoverStatus,1).last("limit 1"));
                                if(iceBoxHandover != null){
                                    thirdMap.put("handoverStatus",iceBoxHandover.getHandoverStatus());
                                    thirdMap.put("handoverId",iceBoxHandover.getId());
                                }
                            }

                            thirdMap.put("shopKeeper",memberInfoVo.getName());
                            thirdMap.put("shopKeeperMobile",memberInfoVo.getMobile());
                            thirdMap.put("address",storeInfoDtoVo.getAddress());
                            thirdMap.put("storeName",storeInfoDtoVo.getStoreName());
                            thirdMap.put("iceBoxId",iceBox.getId());
                            thirdMap.put("iceBoxAssetId", iceBox.getAssetId());
                            thirdMap.put("modelId",iceBox.getModelId());
                            thirdMap.put("modelName",iceBox.getModelName());
                            thirdMap.put("depositMoney", 0);
                            thirdMap.put("putStatus",iceBox.getPutStatus());
                            thirdMap.put("iceBoxStatus",iceBox.getPutStatus());
                            thirdMap.put("freeType",FreePayTypeEnum.UN_FREE.getType());
                            IceBoxExtend iceBoxExtend = iceBoxExtenddao.selectById(iceBox.getId());
                            if(StringUtils.isNotEmpty(iceBoxExtend.getLastApplyNumber())){
                                ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxExtend.getLastApplyNumber()).last("limit 1"));
                                if(applyRelatePutStoreModel != null ){
                                    thirdMap.put("freeType",applyRelatePutStoreModel.getFreeType());
                                    /**
                                     * 不免押去找押金
                                     */
                                    if(FreePayTypeEnum.UN_FREE.getType().equals(applyRelatePutStoreModel.getFreeType())){
                                        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getChestId, iceBox.getId()).eq(IcePutOrder::getApplyNumber, iceBoxExtend.getLastApplyNumber()).last("limit 1"));
                                        if(icePutOrder != null && icePutOrder.getPayMoney() != null){
                                            thirdMap.put("depositMoney", icePutOrder.getPayMoney());
                                        }
                                    }
                                }
                            }
                            if(iceBox.getSupplierId()>0){
                                SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(iceBox.getSupplierId()));
                                thirdMap.put("supplierName",supplierInfo.getName());
                            }
                            thirdMap.put("supplierId", iceBox.getSupplierId());


                            /**
                             * 是否存在退押中状态(0:未审核 1:审核中 2:通过 3:驳回)
                             */
                            thirdMap.put("backStatus",2);
                            IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, iceBox.getId()).orderByDesc(IceBackApplyRelateBox::getId).last("limit 1"));
                            if(iceBackApplyRelateBox != null){
                                IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, iceBackApplyRelateBox.getApplyNumber()));
                                if(iceBackApply != null){
                                    thirdMap.put("backStatus",iceBackApply.getExamineStatus());
                                }
                            }


                            secondList.add(thirdMap);
                        }
                    map.put(storeInfoDtoVo.getStoreNumber(),secondList);
                    }

                }
            }
        }
        /**
         * 邮差 等
         */
        List<SupplierInfo> supplierInfoList = FeignResponseUtil.getFeignData(feignSupplierClient.findByMainSaleManIdAndName(userId,storeName));
        if(supplierInfoList.size()>0){
            for(SupplierInfo subordinateInfoVo : supplierInfoList){
                if(userId.equals(subordinateInfoVo.getMainSalesmanId())){
                    List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, subordinateInfoVo.getNumber()));
                    if(iceBoxes.size()>0){
                        List<Map<String,Object>> secondList = new ArrayList();
                        for (IceBox iceBox : iceBoxes){

                            Map thirdMap = new HashMap(8);
                            /**
                             * 传了接收人 是在通知那里进来的
                             */
                            if(receiveUserId != null && receiveUserId > 0){
                                IceBoxHandover iceBoxHandover1 = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getIceBoxId, iceBox.getId()).eq(IceBoxHandover::getSendUserId, userId).orderByDesc(IceBoxHandover::getId).eq(IceBoxHandover::getReceiveUserId,receiveUserId).eq(IceBoxHandover::getHandoverStatus,1).last("limit 1"));
                                if(iceBoxHandover1 == null){
                                    continue;
                                }else{
                                    thirdMap.put("handoverStatus",iceBoxHandover1.getHandoverStatus());
                                    thirdMap.put("handoverId",iceBoxHandover1.getId());
                                }
                            }else{
                                /**
                                 * 1交接中 2已交接 3已驳回  0就是未交接
                                 */
                                thirdMap.put("handoverStatus",0);
                                IceBoxHandover iceBoxHandover = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getIceBoxId, iceBox.getId()).eq(IceBoxHandover::getSendUserId, userId).orderByDesc(IceBoxHandover::getId).eq(IceBoxHandover::getHandoverStatus,1).last("limit 1"));
                                if(iceBoxHandover != null){
                                    thirdMap.put("handoverStatus",iceBoxHandover.getHandoverStatus());
                                    thirdMap.put("handoverId",iceBoxHandover.getId());
                                }
                            }
                            thirdMap.put("shopKeeper",subordinateInfoVo.getLinkMan());
                            thirdMap.put("shopKeeperMobile",subordinateInfoVo.getLinkManMobile());
                            thirdMap.put("address",subordinateInfoVo.getAddress());
                            thirdMap.put("storeName",subordinateInfoVo.getName());
                            thirdMap.put("iceBoxId",iceBox.getId());
                            thirdMap.put("iceBoxAssetId", iceBox.getAssetId());
                            thirdMap.put("modelId",iceBox.getModelId());
                            thirdMap.put("modelName",iceBox.getModelName());
                            thirdMap.put("depositMoney", 0);
                            thirdMap.put("putStatus",iceBox.getPutStatus());
                            thirdMap.put("iceBoxStatus",iceBox.getPutStatus());
                            thirdMap.put("freeType",FreePayTypeEnum.UN_FREE.getType());
                            IceBoxExtend iceBoxExtend = iceBoxExtenddao.selectById(iceBox.getId());
                            if(StringUtils.isNotEmpty(iceBoxExtend.getLastApplyNumber())){
                                ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxExtend.getLastApplyNumber()).last("limit 1"));
                                if(applyRelatePutStoreModel != null ){
                                    thirdMap.put("freeType",applyRelatePutStoreModel.getFreeType());
                                    /**
                                     * 不免押去找押金
                                     */
                                    if(FreePayTypeEnum.UN_FREE.getType().equals(applyRelatePutStoreModel.getFreeType())){
                                        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getChestId, iceBox.getId()).eq(IcePutOrder::getApplyNumber, iceBoxExtend.getLastApplyNumber()).last("limit 1"));
                                        if(icePutOrder != null && icePutOrder.getPayMoney() != null){
                                            thirdMap.put("depositMoney", icePutOrder.getPayMoney());
                                        }
                                    }
                                }

                            }
                            if(iceBox.getSupplierId()>0){
                                SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(iceBox.getSupplierId()));
                                thirdMap.put("supplierName",supplierInfo.getName());
                            }
                            thirdMap.put("supplierId", iceBox.getSupplierId());

                            /**
                             * 是否存在退押中状态(0:未审核 1:审核中 2:通过 3:驳回)
                             */
                            thirdMap.put("backStatus",2);
                            IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, iceBox.getId()).orderByDesc(IceBackApplyRelateBox::getId).last("limit 1"));
                            if(iceBackApplyRelateBox != null){
                                IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, iceBackApplyRelateBox.getApplyNumber()));
                                if(iceBackApply != null){
                                    thirdMap.put("backStatus",iceBackApply.getExamineStatus());
                                }
                            }
                            secondList.add(thirdMap);
                        }
                        map.put(subordinateInfoVo.getNumber(),secondList);
                    }
                }
            }
        }

        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void sendHandOverRequest(List<IceBoxHandover> iceBoxHandovers) {
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMddHHmmss");
        String relateCode = "";
        for(IceBoxHandover iceBoxHandover : iceBoxHandovers){
            IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, iceBoxHandover.getIceBoxId()).orderByDesc(IceBackApplyRelateBox::getId).last("limit 1"));
            if(iceBackApplyRelateBox != null){
                IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, iceBackApplyRelateBox.getApplyNumber()).last("limit 1"));
                if(iceBackApply != null  ){
                    if(iceBackApply.getExamineStatus().equals(ExamineStatus.DEFAULT_EXAMINE.getStatus()) || iceBackApply.getExamineStatus().equals(ExamineStatus.DOING_EXAMINE.getStatus()) ){
                        throw new ImproperOptionException("有进行中的审批流程，不允许交接");
                    }
                }
            }
            IceBoxHandover handover = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getStatus, 1).eq(IceBoxHandover::getIceBoxId, iceBoxHandover.getIceBoxId()).eq(IceBoxHandover::getSendUserId, iceBoxHandover.getSendUserId()).eq(IceBoxHandover::getReceiveUserId, iceBoxHandover.getReceiveUserId()).last("limit 1"));
            if(handover != null){
                throw new ImproperOptionException("该冰柜正在交接中,请勿重复申请");
            }
            if(iceBoxHandover.getStoreNumber().startsWith("C0")){
                StoreInfoDtoVo storeInfo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBoxHandover.getStoreNumber()));
                if(storeInfo != null){
                    iceBoxHandover.setHeadquartersDeptId(storeInfo.getHeadquartersDeptId());
                    iceBoxHandover.setHeadquartersDeptName(storeInfo.getHeadquartersDeptName());
                    iceBoxHandover.setBusinessDeptId(storeInfo.getBusinessDeptId());
                    iceBoxHandover.setBusinessDeptName(storeInfo.getBusinessDeptName());
                    iceBoxHandover.setRegionDeptId(storeInfo.getRegionDeptId());
                    iceBoxHandover.setRegionDeptName(storeInfo.getRegionDeptName());
                    iceBoxHandover.setServiceDeptId(storeInfo.getServiceDeptId());
                    iceBoxHandover.setServiceDeptName(storeInfo.getServiceDeptName());
                    iceBoxHandover.setGroupDeptId(storeInfo.getGroupDeptId());
                    iceBoxHandover.setGroupDeptName(storeInfo.getGroupDeptName());
                }
            }else {
                SupplierInfoSessionVo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBoxHandover.getStoreNumber()));
                if(supplierInfo != null){
                    iceBoxHandover.setHeadquartersDeptId(supplierInfo.getHeadquartersDeptId());
                    iceBoxHandover.setHeadquartersDeptName(supplierInfo.getHeadquartersDeptName());
                    iceBoxHandover.setBusinessDeptId(supplierInfo.getBusinessDeptId());
                    iceBoxHandover.setBusinessDeptName(supplierInfo.getBusinessDeptName());
                    iceBoxHandover.setRegionDeptId(supplierInfo.getRegionDeptId());
                    iceBoxHandover.setRegionDeptName(supplierInfo.getRegionDeptName());
                    iceBoxHandover.setServiceDeptId(supplierInfo.getServiceDeptId());
                    iceBoxHandover.setServiceDeptName(supplierInfo.getServiceDeptName());
                    iceBoxHandover.setGroupDeptId(supplierInfo.getGroupDeptId());
                    iceBoxHandover.setGroupDeptName(supplierInfo.getGroupDeptName());
                }
            }
            UserInfoVo sendUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(iceBoxHandover.getSendUserId()));
            if(sendUserInfoVo != null && StringUtils.isNotEmpty(sendUserInfoVo.getRealname())){
                iceBoxHandover.setSendUserName(sendUserInfoVo.getRealname());
                iceBoxHandover.setSendUserOfficeName(sendUserInfoVo.getOfficeName());
            }

            UserInfoVo receiveUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(iceBoxHandover.getReceiveUserId()));
            if(receiveUserInfoVo != null && StringUtils.isNotEmpty(receiveUserInfoVo.getRealname())){
                iceBoxHandover.setReceiveUserName(receiveUserInfoVo.getRealname());
                iceBoxHandover.setReceiveUserOfficeName(receiveUserInfoVo.getOfficeName());
            }
            relateCode = iceBoxHandovers.get(0).getSendUserId()+"_"+iceBoxHandovers.get(0).getReceiveUserId()+"_"+prefix;
            iceBoxHandover.setStatus(1);
            iceBoxHandover.setHandoverStatus(1);
            iceBoxHandover.setCreateTime(new Date());
            iceBoxHandover.setHandoverStatus(HandOverEnum.DO_HANDOVER.getType());
            iceBoxHandover.setRelateCode(relateCode);
            iceBoxHandoverDao.insert(iceBoxHandover);
        }

        NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(NoticeTypeEnum.ICEBOX_HANDOVER_CONFIRM.getDesc()+":"+iceBoxHandovers.get(0).getSendUserName()+"==>"+iceBoxHandovers.get(0).getReceiveUserName())
                .noticeTypeEnum(NoticeTypeEnum.ICEBOX_HANDOVER_CONFIRM)
                .relateCode(relateCode)
                .sendUserId(iceBoxHandovers.get(0).getReceiveUserId())
                .build();
        // 创建通知
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void passHandOverRequest(List<Integer> ids) {
        for(Integer id : ids){
            IceBoxHandover iceBoxHandover = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getId,id).eq(IceBoxHandover::getStatus,1).eq(IceBoxHandover::getStatus,1).last("limit 1"));

            if(iceBoxHandover != null){
                iceBoxHandover.setHandoverStatus(HandOverEnum.PASS_HANDOVER.getType());
                iceBoxHandover.setHandoverTime(new Date());
                iceBoxHandoverDao.updateById(iceBoxHandover);

                IceBox iceBox = iceBoxDao.selectById(iceBoxHandover.getIceBoxId());
                iceBox.setResponseManId(iceBoxHandover.getReceiveUserId());
                iceBox.setResponseMan(iceBoxHandover.getReceiveUserName());
                iceBoxDao.updateById(iceBox);
                /*if(StringUtils.isNotEmpty(iceBoxHandover.getStoreNumber())){
                    if(iceBoxHandover.getStoreNumber().contains("C0")){
                        //门店
                        feignStoreClient.updateStoreMainSaleMan(iceBoxHandover.getStoreNumber(),iceBoxHandover.getReceiveUserId());
                    }else{
                        //邮差经销商等
                        feignSupplierClient.updateMainSaleMan(iceBoxHandover.getStoreNumber(),iceBoxHandover.getReceiveUserId());
                    }
                }*/
            }

        }
    }

    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    @Override
    public void rejectHandOverRequest(List<Integer> ids) {
        for(Integer id : ids){
            IceBoxHandover iceBoxHandover = iceBoxHandoverDao.selectById(id);
            if(iceBoxHandover != null){
                iceBoxHandover.setHandoverStatus(HandOverEnum.REJECT_HANOVER.getType());
                iceBoxHandover.setStatus(2);
                iceBoxHandoverDao.updateById(iceBoxHandover);
            }
        }
    }

    @Override
    public IPage<IceBoxHandover> findByPage(IceBoxHandoverPage iceBoxHandoverPage) {
        LambdaQueryWrapper<IceBoxHandover> wrapper = fillWrapper(iceBoxHandoverPage);
        IPage iPage = iceBoxHandoverDao.selectPage(iceBoxHandoverPage, wrapper);
        return iPage;
    }

    @Override
    public CommonResponse exportIceHandover(IceBoxHandoverPage iceBoxHandoverPage) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_HANDOVER_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
        }
        LambdaQueryWrapper<IceBoxHandover> wrapper = fillWrapper(iceBoxHandoverPage);
        IPage iPage = iceBoxHandoverDao.selectPage(iceBoxHandoverPage, wrapper);
        if (iPage != null && iPage.getRecords().size() == 0) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(iceBoxHandoverPage), "冰柜交接信息-导出"));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            iceBoxHandoverPage.setOperateType(OperateTypeEnum.SELECT.getType());
            iceBoxHandoverPage.setRecordsId(recordsId);
            iceBoxHandoverPage.setOperateName(userManageVo.getSessionUserInfoVo().getRealname());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICE_BOX_HANDOVER_QUEUE, iceBoxHandoverPage);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 180, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }

    @Override
    public Map<String, List<Map<String, Object>>> findByUseridNew(Integer sendUserId, Integer receiveUserId, String storeName,String relateCode) {
        Map<String, List<Map<String,Object>>> map = new HashMap<>(16);
        Map<String, List<Map<String,Object>>> likeMap = new HashMap<>(16);
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getResponseManId, sendUserId).isNotNull(IceBox::getPutStoreNumber).eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()));
        if(iceBoxes.size() == 0){
            return map;
        }
        Map<String, List<IceBox>> collect = iceBoxes.stream().collect(Collectors.groupingBy(iceBox -> iceBox.getPutStoreNumber()));
        for (String storeNumber : collect.keySet()){
            List<IceBox> list = collect.get(storeNumber);
            MemberInfoVo memberInfoVo = null;
            StoreInfoDtoVo storeInfoDtoVo = null;
            SubordinateInfoVo subordinateInfoVo = null;
            if(StringUtils.isNotEmpty(storeNumber)){
                if(storeNumber.startsWith("C0")){
                    memberInfoVo = FeignResponseUtil.getFeignData(feignStoreRelateMemberClient.getShopKeeper(storeNumber));
                    storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(storeNumber));
                }else{
                    subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(storeNumber));
                }
            }
            if(list.size() > 0){
                List<Map<String,Object>> secondList = new ArrayList<>();
                for(IceBox iceBox : list){

                    Map<String,Object> thirdMap = new HashMap(8);
                    thirdMap.put("iceBoxId",iceBox.getId());
                    thirdMap.put("iceBoxAssetId", iceBox.getAssetId());
                    thirdMap.put("modelId",iceBox.getModelId());
                    IceModel iceModel = iceModelDao.selectById(iceBox.getModelId());
                    if(iceModel != null){
                        thirdMap.put("modelName",iceModel.getChestModel());
                    }
                    thirdMap.put("depositMoney", iceBox.getDepositMoney());
                    thirdMap.put("putStatus",iceBox.getPutStatus());
                    thirdMap.put("iceBoxStatus",iceBox.getPutStatus());
                    IceBoxExtend iceBoxExtend = iceBoxExtenddao.selectById(iceBox.getId());
                    if(iceBoxExtend != null && StringUtils.isNotEmpty(iceBoxExtend.getLastApplyNumber())){
                        ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxExtend.getLastApplyNumber()).last("limit 1"));
                        if(applyRelatePutStoreModel != null){
                            thirdMap.put("freeType",applyRelatePutStoreModel.getFreeType());
                        }

                    }
                    if(iceBox.getSupplierId()>0){
                        SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(iceBox.getSupplierId()));
                        thirdMap.put("supplierName",supplierInfo.getName());
                    }
                    thirdMap.put("supplierId", iceBox.getSupplierId());

                    /**
                     * 是否存在退押中状态(0:未审核 1:审核中 2:通过 3:驳回)
                     */
                    thirdMap.put("backStatus",2);
                    IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, iceBox.getId()).orderByDesc(IceBackApplyRelateBox::getId).last("limit 1"));
                    if(iceBackApplyRelateBox != null){
                        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, iceBackApplyRelateBox.getApplyNumber()));
                        if(iceBackApply != null){
                            thirdMap.put("backStatus",iceBackApply.getExamineStatus());
                        }
                    }
                    /**
                     * 传了接收人 是在通知那里进来的
                     */
                    if(receiveUserId != null && receiveUserId > 0){
                        IceBoxHandover iceBoxHandover1 = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getRelateCode,relateCode).eq(IceBoxHandover::getIceBoxId, iceBox.getId()).eq(IceBoxHandover::getSendUserId, sendUserId).orderByDesc(IceBoxHandover::getId).eq(IceBoxHandover::getReceiveUserId,receiveUserId).eq(IceBoxHandover::getHandoverStatus,1).last("limit 1"));
                        if(iceBoxHandover1 == null){
                            continue;
                        }else{
                            thirdMap.put("handoverStatus",iceBoxHandover1.getHandoverStatus());
                            thirdMap.put("handoverId",iceBoxHandover1.getId());
                        }
                    }else{
                        /**
                         * 1交接中 2已交接 3已驳回  0就是未交接
                         */
                        thirdMap.put("handoverStatus",0);
                        IceBoxHandover iceBoxHandover = iceBoxHandoverDao.selectOne(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getIceBoxId, iceBox.getId()).eq(IceBoxHandover::getSendUserId, sendUserId).orderByDesc(IceBoxHandover::getId).eq(IceBoxHandover::getHandoverStatus,1).last("limit 1"));
                        if(iceBoxHandover != null){
                            thirdMap.put("handoverStatus",iceBoxHandover.getHandoverStatus());
                            thirdMap.put("handoverId",iceBoxHandover.getId());
                        }
                    }
                    if(StringUtils.isNotEmpty(iceBox.getPutStoreNumber())){
                        if(iceBox.getPutStoreNumber().startsWith("C0")){
                            thirdMap.put("shopKeeper",memberInfoVo.getName());
                            thirdMap.put("shopKeeperMobile",memberInfoVo.getMobile());
                            thirdMap.put("address",storeInfoDtoVo.getAddress());
                            thirdMap.put("storeName",storeInfoDtoVo.getStoreName());
                        }else {
                            thirdMap.put("shopKeeper",subordinateInfoVo.getLinkman());
                            thirdMap.put("shopKeeperMobile",subordinateInfoVo.getLinkmanMobile());
                            thirdMap.put("address",subordinateInfoVo.getAddress());
                            thirdMap.put("storeName",subordinateInfoVo.getName());
                        }
                    }
                    secondList.add(thirdMap);
                }
                map.put(storeNumber,secondList);
            }
        }
        if(StringUtils.isNotEmpty(storeName)){
            List<String> storeNumbers = FeignResponseUtil.getFeignData(feignStoreClient.findStoesByRelateUser(sendUserId));

            StoreInfoDtoVo requestVo = new StoreInfoDtoVo();
            requestVo.setStoreName(storeName);
            List<StoreInfoDtoVo> feignData = FeignResponseUtil.getFeignData(feignStoreClient.findStoreInfoVoByNameAndNumber(storeName));
            if(feignData.size()>0){
                for (StoreInfoDtoVo storeVo : feignData){
                    if(storeNumbers.contains(storeVo.getStoreNumber()) ){
                        likeMap.put(storeVo.getStoreNumber(),map.get(storeVo.getStoreNumber()));
                    }
                }
            }
            List<SimpleSupplierInfoVo> feignData1 = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierInfoVoByNameOrNumber(storeName));
            if(feignData1.size()>0){
                for (SimpleSupplierInfoVo storeVo : feignData1){
                    if(storeNumbers.contains(storeVo.getNumber()) ){
                        likeMap.put(storeVo.getNumber(),map.get(storeVo.getNumber()));
                    }
                }
            }

            return likeMap;
        }
        return map;
    }

    @Override
    public void updateResponseMan(List<Integer> iceboxIds) {
        if(iceboxIds.size()>0){
            for(Integer id : iceboxIds){
                IceBox iceBox = iceBoxDao.selectById(id);
                if(iceBox != null && StringUtils.isNotEmpty(iceBox.getPutStoreNumber()) && PutStatus.FINISH_PUT.getStatus().equals(iceBox.getPutStatus())){
                    if(iceBox.getPutStoreNumber().startsWith("C0")){
                        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                        if(storeInfoDtoVo != null){
                            if(storeInfoDtoVo.getMainSaleManId() != null && storeInfoDtoVo.getMainSaleManId() > 0){
                                //有主业务员
                                iceBox.setResponseManId(storeInfoDtoVo.getMainSaleManId());
                                UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(storeInfoDtoVo.getMainSaleManId()));
                                if(userInfoVo != null){
                                    iceBox.setResponseMan(userInfoVo.getRealname());
                                    iceBoxDao.updateById(iceBox);
                                }
                            }else{
                                //无主业务员
                                IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getIceBoxId, iceBox.getId()).eq(IceExamine::getStoreNumber, iceBox.getPutStoreNumber()).orderByAsc(IceExamine::getId).last("limit 1"));
                                if(iceExamine != null && iceExamine.getCreateBy() != null){
                                    iceBox.setResponseManId(storeInfoDtoVo.getMainSaleManId());
                                    UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(iceExamine.getCreateBy()));
                                    if(userInfoVo != null){
                                        iceBox.setResponseMan(userInfoVo.getRealname());
                                    }
                                    iceBoxDao.updateById(iceBox);
                                }

                            }
                        }

                    }else{
                        SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBox.getPutStoreNumber()));
                        if(supplierInfoSessionVo != null){
                            if(supplierInfoSessionVo.getMainSalesmanId() != null && supplierInfoSessionVo.getMainSalesmanId() > 0){
                                //有主业务员
                                iceBox.setResponseManId(supplierInfoSessionVo.getMainSalesmanId());
                                UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(supplierInfoSessionVo.getMainSalesmanId()));
                                if(userInfoVo != null){
                                    iceBox.setResponseMan(userInfoVo.getRealname());
                                }
                                iceBoxDao.updateById(iceBox);
                            }else {
                                //无主业务员
                                IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getIceBoxId, iceBox.getId()).eq(IceExamine::getStoreNumber, iceBox.getPutStoreNumber()).orderByAsc(IceExamine::getId).last("limit 1"));
                                if(iceExamine != null && iceExamine.getCreateBy() != null){
                                    iceBox.setResponseManId(supplierInfoSessionVo.getMainSalesmanId());
                                    UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(iceExamine.getCreateBy()));
                                    if(userInfoVo != null){
                                        iceBox.setResponseMan(userInfoVo.getRealname());
                                    }
                                    iceBoxDao.updateById(iceBox);
                                }
                            }
                        }

                    }
                }
            }
        }else {
            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()));
            for(IceBox iceBox : iceBoxes){
                if(iceBox != null && StringUtils.isNotEmpty(iceBox.getPutStoreNumber()) && PutStatus.FINISH_PUT.getStatus().equals(iceBox.getPutStatus())){
                    if(iceBox.getPutStoreNumber().startsWith("C0")){
                        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                        if(storeInfoDtoVo != null){
                            if(storeInfoDtoVo.getMainSaleManId() != null && storeInfoDtoVo.getMainSaleManId() > 0){
                                //有主业务员
                                iceBox.setResponseManId(storeInfoDtoVo.getMainSaleManId());
                                UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(storeInfoDtoVo.getMainSaleManId()));
                                if(userInfoVo != null){
                                    iceBox.setResponseMan(userInfoVo.getRealname());
                                }
                                iceBoxDao.updateById(iceBox);
                            }else{
                                //无主业务员
                                IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getIceBoxId, iceBox.getId()).eq(IceExamine::getStoreNumber, iceBox.getPutStoreNumber()).orderByAsc(IceExamine::getId).last("limit 1"));
                                if(iceExamine != null && iceExamine.getCreateBy() != null){
                                    iceBox.setResponseManId(storeInfoDtoVo.getMainSaleManId());
                                    UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(iceExamine.getCreateBy()));
                                    if(userInfoVo != null){
                                        iceBox.setResponseMan(userInfoVo.getRealname());
                                    }
                                    iceBoxDao.updateById(iceBox);
                                }

                            }
                        }

                    }else{
                        SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBox.getPutStoreNumber()));
                        if(supplierInfoSessionVo != null){
                            if(supplierInfoSessionVo.getMainSalesmanId() != null && supplierInfoSessionVo.getMainSalesmanId() > 0){
                                //有主业务员
                                iceBox.setResponseManId(supplierInfoSessionVo.getMainSalesmanId());
                                UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(supplierInfoSessionVo.getMainSalesmanId()));
                                if(userInfoVo != null){
                                    iceBox.setResponseMan(userInfoVo.getRealname());
                                }
                                iceBoxDao.updateById(iceBox);

                            }else {
                                //无主业务员
                                IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getIceBoxId, iceBox.getId()).eq(IceExamine::getStoreNumber, iceBox.getPutStoreNumber()).orderByAsc(IceExamine::getId).last("limit 1"));
                                if(iceExamine != null && iceExamine.getCreateBy() != null){
                                    iceBox.setResponseManId(supplierInfoSessionVo.getMainSalesmanId());
                                    UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(iceExamine.getCreateBy()));
                                    if(userInfoVo != null){
                                        iceBox.setResponseMan(userInfoVo.getRealname());
                                    }
                                    iceBoxDao.updateById(iceBox);
                                }
                            }
                        }

                    }
                }
            }

            }
    }

    private LambdaQueryWrapper<IceBoxHandover> fillWrapper(IceBoxHandoverPage iceBoxHandoverPage) {
        LambdaQueryWrapper<IceBoxHandover> wrapper = Wrappers.<IceBoxHandover>lambdaQuery();
        if(iceBoxHandoverPage.getGroupDeptId() != null && iceBoxHandoverPage.getGroupDeptId() > 0){
            wrapper.eq(IceBoxHandover::getGroupDeptId,iceBoxHandoverPage.getGroupDeptId());
        }
        if(iceBoxHandoverPage.getServiceDeptId() != null && iceBoxHandoverPage.getServiceDeptId() > 0){
            wrapper.eq(IceBoxHandover::getServiceDeptId,iceBoxHandoverPage.getServiceDeptId());
        }
        if(iceBoxHandoverPage.getRegionDeptId() != null && iceBoxHandoverPage.getRegionDeptId() > 0){
            wrapper.eq(IceBoxHandover::getRegionDeptId,iceBoxHandoverPage.getRegionDeptId());
        }
        if(iceBoxHandoverPage.getBusinessDeptId() != null && iceBoxHandoverPage.getBusinessDeptId() >0){
            wrapper.eq(IceBoxHandover::getBusinessDeptId,iceBoxHandoverPage.getBusinessDeptId());
        }
        if(iceBoxHandoverPage.getHeadquartersDeptId() != null && iceBoxHandoverPage.getHeadquartersDeptId() > 0){
            wrapper.eq(IceBoxHandover::getHeadquartersDeptId,iceBoxHandoverPage.getHeadquartersDeptId());
        }
        if(StringUtils.isNotEmpty(iceBoxHandoverPage.getIceBoxAssetid())){
            wrapper.eq(IceBoxHandover::getIceBoxAssetid,iceBoxHandoverPage.getIceBoxAssetid());
        }
        if(StringUtils.isNotEmpty(iceBoxHandoverPage.getSendUserName())){
            wrapper.like(IceBoxHandover::getSendUserName,iceBoxHandoverPage.getSendUserName());
        }
        if(StringUtils.isNotEmpty(iceBoxHandoverPage.getReceiveUserName())){
            wrapper.like(IceBoxHandover::getReceiveUserName,iceBoxHandoverPage.getReceiveUserName());
        }
        if(iceBoxHandoverPage.getIceboxStatus() != null){
            wrapper.eq(IceBoxHandover::getIceboxStatus,iceBoxHandoverPage.getIceboxStatus());
        }
        if(iceBoxHandoverPage.getStartTime() != null){
            wrapper.ge(IceBoxHandover::getCreateTime,iceBoxHandoverPage.getStartTime());
        }
        if(iceBoxHandoverPage.getEndTime() != null){
            wrapper.le(IceBoxHandover::getCreateTime,iceBoxHandoverPage.getEndTime());
        }
        if(iceBoxHandoverPage.getHandoverStatus() != null && iceBoxHandoverPage.getHandoverStatus() > 0){
            wrapper.eq(IceBoxHandover::getHandoverStatus,iceBoxHandoverPage.getHandoverStatus());
        }
        return wrapper;
    }
}




