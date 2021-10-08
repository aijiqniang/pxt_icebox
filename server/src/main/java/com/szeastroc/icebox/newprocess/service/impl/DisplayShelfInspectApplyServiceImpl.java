package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.icebox.vo.ShelfInspectRequest;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfInspectApplyDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectReport;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApplyRelate;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.ExamineStatus;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            inspectReportService.build(model,displayShelf);
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
    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    public void doInspect(ShelfInspectRequest request) {
        //审批状态 0:审批中 1:批准 2:驳回
        //审批通过
        if(request.getStatus() == 1){
            ShelfInspectModel model = request.getModel();
            //全部变为正常
            displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                    .eq(DisplayShelf::getPutNumber,request.getModel().getCustomerNumber())
                    .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
            );
            //挑选指定数量遗失
            List<ShelfInspectModel.LostShelf> lostShelves = model.getLostShelves();
            if(Objects.nonNull(lostShelves)){
                for (ShelfInspectModel.LostShelf lostShelf : lostShelves) {
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,request.getModel().getCustomerNumber())
                            .eq(DisplayShelf::getType, lostShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.LOSE.getType())
                            .last(" limit " + lostShelf.getCount())
                    );
                }
            }
            //挑选指定数量报废
            if(Objects.nonNull(model.getScrapShelves())){
                List<ShelfInspectModel.ScrapShelf> scrapShelves = model.getScrapShelves();
                for (ShelfInspectModel.ScrapShelf scrapShelf : scrapShelves) {
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getPutNumber,request.getModel().getCustomerNumber())
                            .eq(DisplayShelf::getType, scrapShelf.getType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.SCRAP.getType())
                            .last(" limit " + scrapShelf.getCount())
                    );
                }
            }
        }
        inspectReportService.updateStatus(request);
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


}
