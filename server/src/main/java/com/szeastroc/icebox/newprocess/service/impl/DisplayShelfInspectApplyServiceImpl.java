package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.icebox.vo.ShelfInspectRequest;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfDao;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfInspectApplyDao;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfInspectExtendDao;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 陈列架巡检(DisplayShelfInspectApply)表服务实现类
 *
 * @author chenchao
 * @since 2021-06-07 14:41:15
 */
@Service
public class DisplayShelfInspectApplyServiceImpl extends ServiceImpl<DisplayShelfInspectApplyDao, DisplayShelfInspectApply> implements DisplayShelfInspectApplyService {
    @Autowired
    private DisplayShelfService displayShelfService;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    FeignSupplierClient feignSupplierClient;
    @Autowired
    FeignUserClient feignUserClient;
    @Autowired
    FeignExamineClient feignExamineClient;
    @Autowired
    FeignCacheClient feignCacheClient;
    @Autowired
    DisplayShelfInspectReportService inspectReportService;
    @Autowired
    private DisplayShelfDao displayShelfDao;
    @Resource
    DisplayShelfInspectApplyDao displayShelfInspectApplyDao;
    @Resource
    DisplayShelfInspectExtendDao displayShelfInspectExtendDao;
    @Resource
    private FeignDeptClient feignDeptClient;

    @Override
    @Transactional(rollbackFor = Exception.class,transactionManager = "transactionManager")
    public List<SessionExamineVo.VisitExamineNodeVo> shelfInspect(ShelfInspectModel model) {
        String applyNumber = "INS" + IdUtil.simpleUUID().substring(0, 29);
        model.setApplyNumber(applyNumber);
        //判断货架是否全部正常，巡检状态是否全部正常
        boolean normal = false;
        List<DisplayShelf> displayShelves = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery().eq(DisplayShelf::getPutNumber, model.getCustomerNumber()));
        //不正常数量
        long count = displayShelves.stream().filter(o -> !o.getStatus().equals(IceBoxEnums.StatusEnum.NORMAL.getType())).count();
        if (count == 0) {
            if (CollectionUtils.isNotEmpty(model.getNormalShelves())) {
                //巡检正常数量
                int sum = model.getNormalShelves().stream().mapToInt(ShelfInspectModel.NormalShelf::getCount).sum();
                if (sum == displayShelves.size()) {
                    normal = true;
                }
            }
        }
        //生成巡检报表数据
        DisplayShelf displayShelf = displayShelves.iterator().next();
        DisplayShelfInspectApply apply = new DisplayShelfInspectApply();
        apply.setApplyNumber(applyNumber)
                .setCustomerNumber(model.getCustomerNumber())
                .setCustomerType(displayShelf.getCustomerType())
                .setImageUrl(String.join(",", model.getImageUrls()))
                .setCreatedBy(model.getCreateBy())
                .setCreatedTime(new Date())
                .setUserId(model.getCreateBy())
                .setRemark(model.getRemark())
                .setDeptId(model.getDeptId())
        ;
        this.save(apply);

        CompletableFuture.runAsync(()->{
            inspectReportService.build(model);
        }, ExecutorServiceFactory.getInstance());
        if (!normal) {
            SessionExamineVo sessionExamineVo = FeignResponseUtil.getFeignData(feignExamineClient.createShelfInspect(model));
            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
            if (CollectionUtils.isNotEmpty(visitExamineNodes)) {
                apply.setExamineId(visitExamineNodes.get(0).getExamineId());
                this.updateById(apply);
            }
            return visitExamineNodes;
        }
        apply.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
        this.updateById(apply);
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SessionExamineVo.VisitExamineNodeVo> submitShelfInspectDetails(ShelfInspectModel model) {
        String applyNumber = "INS" + IdUtil.simpleUUID().substring(0, 29);
        model.setApplyNumber(applyNumber);
        DisplayShelfInspectApply displayShelfInspectApply = new DisplayShelfInspectApply();
        displayShelfInspectApply.setApplyNumber(applyNumber)
                .setCustomerNumber(model.getCustomerNumber())
                .setImageUrl(String.join(",", model.getImageUrls()))
                .setCreatedBy(model.getCreateBy())
                .setCreatedTime(new Date())
                .setUserId(model.getCreateBy())
                .setCreateName(model.getCreateName())
                .setCustomerName(model.getCustomerName())
                .setRemark(model.getRemark())
                .setCustomerType(model.getCustomerType())
                .setDeptId(model.getDeptId());
        this.save(displayShelfInspectApply);

        CompletableFuture.runAsync(()->{
            inspectReportService.build(model);
        });
        //如果是正常的  不需要审批
        if(model.getInspectStatus().equals(IceBoxEnums.StatusEnum.NORMAL.getType())){
            displayShelfInspectApply.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
            List<ShelfInspectModel.NormalShelf> normalShelves = model.getNormalShelves();
            for (ShelfInspectModel.NormalShelf normalShelf : normalShelves) {
                DisplayShelfInspectExtend displayShelfInspectExtend = new DisplayShelfInspectExtend();
                displayShelfInspectExtend.setApplyNumber(applyNumber)
                        .setCount(normalShelf.getCount())
                        .setName(normalShelf.getName())
                        .setSize(normalShelf.getSize())
                        .setStatus(model.getInspectStatus())
                        .setType(normalShelf.getType());
                displayShelfInspectExtendDao.insert(displayShelfInspectExtend);
            }
        }else if(model.getInspectStatus().equals(IceBoxEnums.StatusEnum.SCRAP.getType())){
            if(Objects.nonNull(model.getScrapShelves())){
                List<ShelfInspectModel.ScrapShelf> scrapShelves = model.getScrapShelves();
                for (ShelfInspectModel.ScrapShelf scrapShelf : scrapShelves) {
                    DisplayShelfInspectExtend displayShelfInspectExtend = new DisplayShelfInspectExtend();
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,model.getCustomerNumber())
                            .eq(DisplayShelf::getType, scrapShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .eq(DisplayShelf::getPutStatus, PutStatus.FINISH_PUT.getStatus())
                            .eq(DisplayShelf::getSize,scrapShelf.getSize())
                            .eq(DisplayShelf::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.IS_SCRAPING_UNPASS.getType())
                            .set(DisplayShelf::getApplyNumber,applyNumber)
                            .last(" limit " + scrapShelf.getCount()));
                    displayShelfInspectExtend.setApplyNumber(applyNumber)
                        .setCount(scrapShelf.getCount())
                        .setName(scrapShelf.getName())
                        .setSize(scrapShelf.getSize())
                        .setStatus(model.getInspectStatus())
                        .setType(scrapShelf.getType());
                    displayShelfInspectExtendDao.insert(displayShelfInspectExtend);
                }
            }
            SessionExamineVo sessionExamineVo = FeignResponseUtil.getFeignData(feignExamineClient.createShelfInspect(model));
            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
            if (CollectionUtils.isNotEmpty(visitExamineNodes)) {
                displayShelfInspectApply.setExamineId(visitExamineNodes.get(0).getExamineId());
                this.updateById(displayShelfInspectApply);
            }
            return visitExamineNodes;
        }else if(model.getInspectStatus().equals(IceBoxEnums.StatusEnum.LOSE.getType())){
            //审批前  先将状态设置成“遗失报备中”
            List<ShelfInspectModel.LostShelf> lostShelves = model.getLostShelves();
            if(Objects.nonNull(lostShelves)){
                for (ShelfInspectModel.LostShelf lostShelf : lostShelves) {
                    DisplayShelfInspectExtend displayShelfInspectExtend = new DisplayShelfInspectExtend();
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,model.getCustomerNumber())
                            .eq(DisplayShelf::getType, lostShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .eq(DisplayShelf::getSize,lostShelf.getSize())
                            .eq(DisplayShelf::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus())
                            .eq(DisplayShelf::getPutStatus, PutStatus.FINISH_PUT.getStatus())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.IS_LOSEING_UNPASS.getType())
                            .set(DisplayShelf::getApplyNumber,applyNumber)
                            .last(" limit " + lostShelf.getCount()));
                    displayShelfInspectExtend.setApplyNumber(applyNumber)
                            .setCount(lostShelf.getCount())
                            .setName(lostShelf.getName())
                            .setSize(lostShelf.getSize())
                            .setStatus(model.getInspectStatus())
                            .setType(lostShelf.getType());
                    displayShelfInspectExtendDao.insert(displayShelfInspectExtend);
                }
            }
            SessionExamineVo sessionExamineVo = FeignResponseUtil.getFeignData(feignExamineClient.createShelfInspect(model));
            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
            if (CollectionUtils.isNotEmpty(visitExamineNodes)) {
                displayShelfInspectApply.setExamineId(visitExamineNodes.get(0).getExamineId());
                this.updateById(displayShelfInspectApply);
            }
            return visitExamineNodes;
        }
        this.updateById(displayShelfInspectApply);
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    public void doInspect(ShelfInspectRequest request) {
        //审批状态 0:审批中 1:批准 2:驳回
        //审批通过
        ShelfInspectModel model = request.getModel();
        if(request.getStatus() == 1){
            //挑选指定数量遗失
            List<ShelfInspectModel.LostShelf> lostShelves = model.getLostShelves();
            if(Objects.nonNull(lostShelves)){
                for (ShelfInspectModel.LostShelf lostShelf : lostShelves) {
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,request.getModel().getCustomerNumber())
                            .eq(DisplayShelf::getType, lostShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.IS_LOSEING_UNPASS.getType())
                            .eq(DisplayShelf::getSize,lostShelf.getSize())
                            .eq(DisplayShelf::getApplyNumber,model.getApplyNumber())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.LOSE.getType())
                            .last(" limit " + lostShelf.getCount())
                    );
                }
            }
            this.update(Wrappers.<DisplayShelfInspectApply>lambdaUpdate().eq(DisplayShelfInspectApply::getApplyNumber,model.getApplyNumber()).set(DisplayShelfInspectApply::getExamineStatus,ExamineStatus.PASS_EXAMINE.getStatus()));
            //挑选指定数量报废
            if(Objects.nonNull(model.getScrapShelves())){
                List<ShelfInspectModel.ScrapShelf> scrapShelves = model.getScrapShelves();
                for (ShelfInspectModel.ScrapShelf scrapShelf : scrapShelves) {
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,request.getModel().getCustomerNumber())
                            .eq(DisplayShelf::getType, scrapShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.IS_SCRAPING_UNPASS.getType())
                            .eq(DisplayShelf::getSize,scrapShelf.getSize())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.SCRAP.getType())
                            .eq(DisplayShelf::getApplyNumber,model.getApplyNumber())
                            .last(" limit " + scrapShelf.getCount())
                    );
                }
            }
        //审批驳回  需要将报废中和遗失中的数据改为正常
        }else if(request.getStatus() == 2){
            //驳回 ： 将遗失中的数据修改成正常
            List<ShelfInspectModel.LostShelf> lostShelves = model.getLostShelves();
            if(Objects.nonNull(lostShelves)){
                for (ShelfInspectModel.LostShelf lostShelf : lostShelves) {
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,request.getModel().getCustomerNumber())
                            .eq(DisplayShelf::getType, lostShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.IS_LOSEING_UNPASS.getType())
                            .eq(DisplayShelf::getSize,lostShelf.getSize())
                            .eq(DisplayShelf::getApplyNumber,model.getApplyNumber())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .set(DisplayShelf::getApplyNumber, 0)
                            .last(" limit " + lostShelf.getCount())
                    );
                }
            }
            //驳回 ： 将报废中的数据修改成正常
            if(Objects.nonNull(model.getScrapShelves())){
                List<ShelfInspectModel.ScrapShelf> scrapShelves = model.getScrapShelves();
                for (ShelfInspectModel.ScrapShelf scrapShelf : scrapShelves) {
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,request.getModel().getCustomerNumber())
                            .eq(DisplayShelf::getType, scrapShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.IS_SCRAPING_UNPASS.getType())
                            .eq(DisplayShelf::getSize,scrapShelf.getSize())
                            .eq(DisplayShelf::getApplyNumber,model.getApplyNumber())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .set(DisplayShelf::getApplyNumber, 0)
                            .last(" limit " + scrapShelf.getCount())
                    );
                }
            }
            this.update(Wrappers.<DisplayShelfInspectApply>lambdaUpdate().eq(DisplayShelfInspectApply::getApplyNumber,model.getApplyNumber()).set(DisplayShelfInspectApply::getExamineStatus,ExamineStatus.REJECT_EXAMINE.getStatus()));
        }
//        inspectReportService.updateStatus(request);
    }

    @Override
    public IPage<DisplayShelfInspectApply> history(ShelfInspectPage page) {
        LambdaQueryWrapper<DisplayShelfInspectApply> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByDesc(DisplayShelfInspectApply::getCreatedTime);
        wrapper.eq(DisplayShelfInspectApply::getCustomerNumber,page.getCustomerNumber()).eq(DisplayShelfInspectApply::getCreatedBy,page.getCreateBy());
        if(Objects.nonNull(page.getCreateTime())){
            Date date = new Date(page.getCreateTime().getTime());
            date.setTime(date.getTime() + 24 * 60 * 60 * 1000);
            wrapper.ge(DisplayShelfInspectApply::getCreatedTime, page.getCreateTime()).le(DisplayShelfInspectApply::getCreatedTime, date);
        }
        IPage<DisplayShelfInspectApply> iPage = this.page(page, wrapper);
        return iPage.convert(o->{
            String customerName;
            if(o.getCustomerType() == 5){
                StoreInfoDtoVo feignData = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(o.getCustomerNumber()));
                customerName = feignData.getStoreName();
            }else {
                SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(o.getCustomerNumber()));
                customerName = supplier.getName();
            }
            o.setCreateName(page.getCreateName());
            o.setCustomerName(customerName);
            return o;
        });
    }

    @Override
    public IPage<ShelfInspectVo> inspectHistory(ShelfInspectPage page){
        IPage<ShelfInspectVo> accountPage = new Page<>();
        List<ShelfInspectVo> shelfInspectVos = new ArrayList<>();

        LambdaQueryWrapper<DisplayShelfInspectApply> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByDesc(DisplayShelfInspectApply::getCreatedTime);
        wrapper.eq(DisplayShelfInspectApply::getCustomerNumber,page.getCustomerNumber()).eq(DisplayShelfInspectApply::getExamineStatus,ExamineStatus.PASS_EXAMINE.getStatus());
        if(Objects.nonNull(page.getCreateTime())){
            wrapper.ge(DisplayShelfInspectApply::getCreatedTime, page.getCreateTime()).le(DisplayShelfInspectApply::getCreatedTime, page.getCreateTime());
        }
        IPage<DisplayShelfInspectApply> iPage = this.page(page, wrapper);
        BeanUtils.copyProperties(iPage, accountPage);
        for (DisplayShelfInspectApply record : iPage.getRecords()) {
            ShelfInspectVo shelfInspectVo = new ShelfInspectVo();
            BeanUtils.copyProperties(record, shelfInspectVo);
            //1：正常 2：报废 3：遗失
            List<ShelfInspectModel.LostShelf> lostShelves = new ArrayList<>();
            List<ShelfInspectModel.ScrapShelf> scrapShelves = new ArrayList<>();
            List<ShelfInspectModel.NormalShelf> normalShelves = new ArrayList<>();
            List<DisplayShelfInspectExtend> displayShelfInspectExtends = displayShelfInspectExtendDao.selectList(Wrappers.<DisplayShelfInspectExtend>lambdaQuery().eq(DisplayShelfInspectExtend::getApplyNumber, shelfInspectVo.getApplyNumber()));
            shelfInspectVo.setInspectStatus(displayShelfInspectExtends.get(0).getStatus());
            for (DisplayShelfInspectExtend displayShelfInspectExtend : displayShelfInspectExtends) {
                if(displayShelfInspectExtend.getStatus().equals(2)){
                    ShelfInspectModel.ScrapShelf scrapShelf = new ShelfInspectModel.ScrapShelf();
                    scrapShelf.setCount(displayShelfInspectExtend.getCount())
                            .setName(displayShelfInspectExtend.getName())
                            .setSize(displayShelfInspectExtend.getSize())
                            .setType(displayShelfInspectExtend.getType());
                    scrapShelves.add(scrapShelf);
                }else if(displayShelfInspectExtend.getStatus().equals(3)){
                    ShelfInspectModel.LostShelf lostShelf = new ShelfInspectModel.LostShelf();
                    lostShelf.setCount(displayShelfInspectExtend.getCount())
                            .setName(displayShelfInspectExtend.getName())
                            .setSize(displayShelfInspectExtend.getSize())
                            .setType(displayShelfInspectExtend.getType());
                    lostShelves.add(lostShelf);
                }else if(displayShelfInspectExtend.getStatus().equals(1)){
                    ShelfInspectModel.NormalShelf normalShelf = new ShelfInspectModel.NormalShelf();
                    normalShelf.setCount(displayShelfInspectExtend.getCount())
                            .setName(displayShelfInspectExtend.getName())
                            .setSize(displayShelfInspectExtend.getSize())
                            .setType(displayShelfInspectExtend.getType());
                    normalShelves.add(normalShelf);
                }

            }
            if(CollectionUtils.isNotEmpty(scrapShelves)){
                shelfInspectVo.setScrapShelves(scrapShelves);
            }
            if(CollectionUtils.isNotEmpty(lostShelves)){
                shelfInspectVo.setLostShelves(lostShelves);
            }
            if(CollectionUtils.isNotEmpty(normalShelves)){
                shelfInspectVo.setNormalShelves(normalShelves);
            }
            System.out.println(shelfInspectVo);
            shelfInspectVos.add(shelfInspectVo);
        }
        accountPage.setRecords(shelfInspectVos);
        return accountPage;
    }

    @Override
    public List<ShelfInspectModel> submitted(String customerNumber){
        List<ShelfInspectModel> shelfInspectModels = new ArrayList<>();
        //根据巡检表中 未审核以及审核中的数据
        List<DisplayShelfInspectApply> displayShelfInspectApplies = displayShelfInspectApplyDao.selectList(Wrappers.<DisplayShelfInspectApply>lambdaQuery().eq(DisplayShelfInspectApply::getCustomerNumber, customerNumber).in(DisplayShelfInspectApply::getExamineStatus, ExamineStatus.DEFAULT_EXAMINE.getStatus(), ExamineStatus.DOING_EXAMINE.getStatus()));
        if(CollectionUtils.isEmpty(displayShelfInspectApplies)){
            return shelfInspectModels;
        }
        List<String> applyNumberList = displayShelfInspectApplies.stream().map(DisplayShelfInspectApply::getApplyNumber).collect(Collectors.toList());
        for (String applyNumber : applyNumberList) {
            ShelfInspectModel shelfInspectModel = new ShelfInspectModel();
            DisplayShelfInspectApply displayShelfInspectApply = displayShelfInspectApplyDao.selectOne(Wrappers.<DisplayShelfInspectApply>lambdaQuery().eq(DisplayShelfInspectApply::getApplyNumber, applyNumber));
            shelfInspectModel.setCreateBy(displayShelfInspectApply.getCreatedBy());
            shelfInspectModel.setCreateName(displayShelfInspectApply.getCreateName());
            shelfInspectModel.setCustomerName(displayShelfInspectApply.getCustomerName());
            shelfInspectModel.setCustomerNumber(displayShelfInspectApply.getCustomerNumber());
            shelfInspectModel.setRemark(displayShelfInspectApply.getRemark());
            shelfInspectModel.setDeptId(displayShelfInspectApply.getDeptId());
            String[] split = displayShelfInspectApply.getImageUrl().split(",");
            List<String> imageUrls = Arrays.asList(split);
            shelfInspectModel.setImageUrls(imageUrls);
            shelfInspectModel.setInspectDate(displayShelfInspectApply.getCreatedTime());
            List<DisplayShelf> displayShelfList = displayShelfDao.InspectCount(applyNumber);
            shelfInspectModel.setInspectStatus(displayShelfList.get(0).getStatus());
            List<ShelfInspectModel.ScrapShelf> scrapShelfList = new ArrayList<>();
            List<ShelfInspectModel.LostShelf> lostShelfList = new ArrayList<>();

            for (DisplayShelf displayShelf : displayShelfList) {
                //要么就是报废中 要么就是遗失中 只有一种
                ShelfInspectModel.ScrapShelf scrapShelves = new ShelfInspectModel.ScrapShelf();
                ShelfInspectModel.LostShelf lostShelf = new ShelfInspectModel.LostShelf();
                if (displayShelf.getStatus().equals(IceBoxEnums.StatusEnum.IS_SCRAPING_UNPASS.getType())){
                    scrapShelves.setCount(displayShelf.getCount()).setName(displayShelf.getName())
                            .setSize(displayShelf.getSize()).setType(displayShelf.getType());
                    scrapShelfList.add(scrapShelves);
                }
                if (displayShelf.getStatus().equals(IceBoxEnums.StatusEnum.IS_LOSEING_UNPASS.getType())){
                    lostShelf.setCount(displayShelf.getCount()).setName(displayShelf.getName())
                            .setSize(displayShelf.getSize()).setType(displayShelf.getType());
                    lostShelfList.add(lostShelf);
                }
            }
            if(CollectionUtils.isNotEmpty(scrapShelfList)){
                shelfInspectModel.setScrapShelves(scrapShelfList);
            }
            if(CollectionUtils.isNotEmpty(lostShelfList)){
                shelfInspectModel.setLostShelves(lostShelfList);
            }
            List<SessionExamineVo.VisitExamineNodeVo> examineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(applyNumber));
            shelfInspectModel.setVisitExamineNodes(examineNodeVos);
            shelfInspectModels.add(shelfInspectModel);
        }
        return shelfInspectModels;
    }
}
