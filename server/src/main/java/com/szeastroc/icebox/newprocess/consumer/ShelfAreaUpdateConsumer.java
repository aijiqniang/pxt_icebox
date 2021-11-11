package com.szeastroc.icebox.newprocess.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.szeastroc.common.entity.customer.msg.CustomerChangeMsg;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfDao;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfPutReportDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@RabbitListener(queues = MqConstant.Q_SHELF_UPDATE)
public class ShelfAreaUpdateConsumer {
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Resource
    private DisplayShelfPutReportDao displayShelfPutReportDao;
    @Autowired
    private DisplayShelfDao displayShelfDao;
    @RabbitHandler
    public void task(CustomerChangeMsg customerChangeMsg) throws Exception {
          if(customerChangeMsg.getIsStore()){
              StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(customerChangeMsg.getCustomerNumber()));
              DisplayShelfPutReport displayShelfPutReport = new DisplayShelfPutReport();
              //t_display_shelf_put_report
              displayShelfPutReportDao.update(displayShelfPutReport,new LambdaUpdateWrapper<DisplayShelfPutReport>()
                      .eq(DisplayShelfPutReport::getPutCustomerNumber,customerChangeMsg.getCustomerNumber())
                      .set(DisplayShelfPutReport::getGroupDeptId,store.getGroupDeptId()).set(DisplayShelfPutReport::getGroupDeptName,store.getGroupDeptName())
                      .set(DisplayShelfPutReport::getServiceDeptId,store.getServiceDeptId()).set(DisplayShelfPutReport::getServiceDeptName,store.getServiceDeptName())
                      .set(DisplayShelfPutReport::getRegionDeptId,store.getRegionDeptId()).set(DisplayShelfPutReport::getRegionDeptName,store.getRegionDeptName())
                      .set(DisplayShelfPutReport::getBusinessDeptId,store.getBusinessDeptId()).set(DisplayShelfPutReport::getBusinessDeptName,store.getBusinessDeptName())
                      .set(DisplayShelfPutReport::getHeadquartersDeptId,store.getHeadquartersDeptId()).set(DisplayShelfPutReport::getHeadquartersDeptName,store.getHeadquartersDeptName())
                      .set(DisplayShelfPutReport::getPutCustomerName,store.getStoreName()).set(DisplayShelfPutReport::getPutCustomerType,store.getStoreType()));
              //t_display_shelf
              DisplayShelf displayShelf = new DisplayShelf();
              displayShelfDao.update(displayShelf, new LambdaUpdateWrapper<DisplayShelf>()
                      .eq(DisplayShelf::getPutNumber,customerChangeMsg.getCustomerNumber())
                      .set(DisplayShelf::getGroupDeptId,store.getGroupDeptId()).set(DisplayShelf::getGroupDeptName,store.getGroupDeptName())
                      .set(DisplayShelf::getServiceDeptId,store.getServiceDeptId()).set(DisplayShelf::getServiceDeptName,store.getServiceDeptName())
                      .set(DisplayShelf::getRegionDeptId,store.getRegionDeptId()).set(DisplayShelf::getRegionDeptName,store.getRegionDeptName())
                      .set(DisplayShelf::getBusinessDeptId,store.getBusinessDeptId()).set(DisplayShelf::getBusinessDeptName,store.getBusinessDeptName())
                      .set(DisplayShelf::getHeadquartersDeptId,store.getHeadquartersDeptId()).set(DisplayShelf::getHeadquartersDeptName,store.getHeadquartersDeptName())
                      .set(DisplayShelf::getPutName,store.getStoreName()).set(DisplayShelf::getCustomerType,store.getStoreType()));
          }
    }
}
