package com.szeastroc.icebox.newprocess.consumer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.dao.IceBoxHandoverDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxHandover;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.vo.IceBoxPutReportExcelVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxHandoverPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 *
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/6/7 17:24
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceHandoverConsumer {

    private final IceBoxHandoverDao iceBoxHandoverDao;
    private final ImageUploadUtil imageUploadUtil;
    private final FeignExportRecordsClient feignExportRecordsClient;

    @RabbitListener(queues = MqConstant.ICE_BOX_HANDOVER_QUEUE,containerFactory = "iceboxHandoverContainer")
    public void task(IceBoxHandoverPage iceBoxHandoverPage) throws Exception{
        if(OperateTypeEnum.SELECT.getType().equals(iceBoxHandoverPage.getOperateType())){
            log.info("冰柜交接导出消费》【{}】", JSON.toJSONString(iceBoxHandoverPage));
            export(iceBoxHandoverPage);
        }

    }

    private void export(IceBoxHandoverPage iceBoxHandoverPage) throws Exception{
        LambdaQueryWrapper<IceBoxHandover> wrapper = fillWrapper(iceBoxHandoverPage);
        IPage iPage = iceBoxHandoverDao.selectPage(iceBoxHandoverPage, wrapper);
        int count = 0;
        if(iPage != null && iPage.getRecords() != null){
            count = iPage.getRecords().size();
            String[] columnName = {"本部","事业部","大区","服务处","组", "冰柜编号", "冰柜型号","冰柜状态", "交接人","交接人职务","交接时间","被交接人"
                    ,"被交接人职务", "接收时间", "接收状态"};
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
            PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, iceBoxHandoverPage.getRecordsId(),
                    (wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                        List<IceBoxHandover> excelVoList = iPage.getRecords();

                        for (int i = startRowCount; i <= endRowCount; i++) {
                            SXSSFRow eachDataRow = eachSheet.createRow(i);
                            if ((i - startRowCount) < excelVoList.size()) {
                                IceBoxHandover excelVo = excelVoList.get(i - startRowCount);
                                eachDataRow.createCell(0).setCellValue(excelVo.getHeadquartersDeptName());
                                eachDataRow.createCell(1).setCellValue(excelVo.getBusinessDeptName());
                                eachDataRow.createCell(2).setCellValue(excelVo.getRegionDeptName());
                                eachDataRow.createCell(3).setCellValue(excelVo.getServiceDeptName());
                                eachDataRow.createCell(4).setCellValue(excelVo.getGroupDeptName());
                                eachDataRow.createCell(5).setCellValue(excelVo.getIceBoxAssetid());
                                eachDataRow.createCell(6).setCellValue(excelVo.getModelName());
                                switch (excelVo.getIceboxStatus()){
                                    case 0: eachDataRow.createCell(7).setCellValue("未投放");
                                        break;
                                    case 1: eachDataRow.createCell(7).setCellValue("已锁定");
                                        break;
                                    case 2: eachDataRow.createCell(7).setCellValue("投放中");
                                        break;
                                    case 3: eachDataRow.createCell(7).setCellValue("已投放");
                                        break;
                                }
                                eachDataRow.createCell(8).setCellValue(excelVo.getSendUserName());
                                eachDataRow.createCell(9).setCellValue(excelVo.getSendUserOfficeName());
                                eachDataRow.createCell(10).setCellValue(dateFormat.format(excelVo.getCreateTime()));
                                eachDataRow.createCell(11).setCellValue(excelVo.getReceiveUserName());
                                eachDataRow.createCell(12).setCellValue(excelVo.getReceiveUserOfficeName());
                                eachDataRow.createCell(13).setCellValue(dateFormat.format(excelVo.getHandoverTime()));
                                switch (excelVo.getHandoverStatus()){
                                    case 1:eachDataRow.createCell(14).setCellValue("交接中");
                                        break;
                                    case 2:eachDataRow.createCell(14).setCellValue("已交接");
                                        break;
                                    case 3:eachDataRow.createCell(14).setCellValue("已驳回");
                                        break;
                                }
                            }
                        }
                    });
        }
    }


    private LambdaQueryWrapper<IceBoxHandover> fillWrapper(IceBoxHandoverPage iceBoxHandoverPage) {
        LambdaQueryWrapper<IceBoxHandover> wrapper = Wrappers.<IceBoxHandover>lambdaQuery();
        if(iceBoxHandoverPage.getGroupDeptId() != null){
            wrapper.eq(IceBoxHandover::getGroupDeptId,iceBoxHandoverPage.getGroupDeptId());
        }
        if(iceBoxHandoverPage.getServiceDeptId() != null){
            wrapper.eq(IceBoxHandover::getServiceDeptId,iceBoxHandoverPage.getServiceDeptId());
        }
        if(iceBoxHandoverPage.getRegionDeptId() != null){
            wrapper.eq(IceBoxHandover::getRegionDeptId,iceBoxHandoverPage.getRegionDeptId());
        }
        if(iceBoxHandoverPage.getBusinessDeptId() != null){
            wrapper.eq(IceBoxHandover::getBusinessDeptId,iceBoxHandoverPage.getBusinessDeptId());
        }
        if(iceBoxHandoverPage.getHeadquartersDeptId() != null){
            wrapper.eq(IceBoxHandover::getHeadquartersDeptId,iceBoxHandoverPage.getHeadquartersDeptId());
        }
        if(StringUtils.isNotEmpty(iceBoxHandoverPage.getIceBoxAssetid())){
            wrapper.eq(IceBoxHandover::getIceBoxAssetid,iceBoxHandoverPage.getIceBoxAssetid());
        }
        if(StringUtils.isNotEmpty(iceBoxHandoverPage.getSendUserName())){
            wrapper.like(IceBoxHandover::getSendUserName,iceBoxHandoverPage.getSendUserName());
        }
        if(StringUtils.isNotEmpty(iceBoxHandoverPage.getReceiveUserName())){
            wrapper.like(IceBoxHandover::getReceiveUserId,iceBoxHandoverPage.getReceiveUserName());
        }
        if(iceBoxHandoverPage.getStartTime() != null){
            wrapper.ge(IceBoxHandover::getHandoverTime,iceBoxHandoverPage.getStartTime());
        }
        if(iceBoxHandoverPage.getEndTime() != null){
            wrapper.le(IceBoxHandover::getHandoverTime,iceBoxHandoverPage.getEndTime());
        }
        if(iceBoxHandoverPage.getHandoverStatus() != null && iceBoxHandoverPage.getHandoverStatus() > 0){
            wrapper.eq(IceBoxHandover::getHandoverStatus,iceBoxHandoverPage.getHandoverStatus());
        }
        return wrapper;
    }
}
