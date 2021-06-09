package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.icebox.vo.ShelfInspectRequest;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfInspectApplyDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApplyRelate;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
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
    private DisplayShelfPutApplyRelateService shelfPutApplyRelateService;
    @Autowired
    private DisplayShelfService displayShelfService;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    FeignSupplierClient feignSupplierClient;
    @Autowired
    FeignUserClient feignUserClient;

    @Override
    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    public void doInspect(ShelfInspectRequest request) {
        //审批通过
        if(request.getStatus() == 1){
            ShelfInspectModel model = request.getModel();
            DisplayShelfInspectApply inspectApply = this.getOne(Wrappers.<DisplayShelfInspectApply>lambdaQuery().eq(DisplayShelfInspectApply::getApplyNumber, model.getApplyNumber()));
            List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, inspectApply.getPutNumber()));
            //全部变为正常
            displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                    .in(DisplayShelf::getId, relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()))
                    .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
            );
            //挑选指定数量遗失
            List<ShelfInspectModel.LostShelf> lostShelves = model.getLostShelves();
            for (ShelfInspectModel.LostShelf lostShelf : lostShelves) {
                displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                        .in(DisplayShelf::getId, relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()))
                        .eq(DisplayShelf::getType, lostShelf.getType())
                        .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                        .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.LOSE.getType())
                        .last(" limit " + lostShelf.getCount())
                );
            }
            //挑选指定数量报废
            List<ShelfInspectModel.ScrapShelf> scrapShelves = model.getScrapShelves();
            for (ShelfInspectModel.ScrapShelf scrapShelf : scrapShelves) {
                displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                        .in(DisplayShelf::getId, relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()))
                        .eq(DisplayShelf::getType, scrapShelf.getType())
                        .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                        .set(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.SCRAP.getType())
                        .last(" limit " + scrapShelf.getCount())
                );
            }
        }

    }

    @Override
    public IPage<DisplayShelfInspectApply> history(ShelfInspectPage page) {
        LambdaQueryWrapper<DisplayShelfInspectApply> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByDesc(DisplayShelfInspectApply::getCreatedTime);
        wrapper.eq(DisplayShelfInspectApply::getPutNumber,page.getPutNumber()).eq(DisplayShelfInspectApply::getCreatedBy,page.getCreateBy());
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
