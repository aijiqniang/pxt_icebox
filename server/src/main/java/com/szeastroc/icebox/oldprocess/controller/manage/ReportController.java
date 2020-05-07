package com.szeastroc.icebox.oldprocess.controller.manage;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.oldprocess.service.IceChestPutRecordService;
import com.szeastroc.icebox.util.ExcelUtil;
import com.szeastroc.icebox.oldprocess.vo.IceDepositResponse;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
import com.szeastroc.icebox.oldprocess.vo.report.IceDepositReport;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报表模块
 */
@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private IceChestPutRecordService iceChestPutRecordService;

    /**
     * 查询押金明细
     * @param iceDepositPage
     * @return
     */
    @PostMapping("/queryIceDeposits")
    public CommonResponse<IPage<IceDepositResponse>> queryIceDeposits(@RequestBody IceDepositPage iceDepositPage){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,
                iceChestPutRecordService.queryIceDeposits(iceDepositPage));
    }

    /**
     * 查询押金支付明细
     * @param iceDepositPage
     * @return
     */
    @PostMapping("/queryIceDepositsForPut")
    public CommonResponse<IPage<IceDepositResponse>> queryIceDepositsForPut(@RequestBody IceDepositPage iceDepositPage){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,
                iceChestPutRecordService.queryIceDepositsForPut(iceDepositPage));
    }



    @PostMapping("/excel")
    public void excel(@RequestBody IceDepositPage iceDepositPage, HttpServletResponse response){
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        String fileName = "冰柜押金明细表";
        String titleName = "冰柜押金明细表";
        String[] columnName = {"客户编号", "客户名称", "联系人", "联系电话", "门店地址", "服务处", "设备型号", "设备名称", "资产编号", "支付金额", "支付时间", "交易号", "设备价值"};
        ExcelUtil<IceDepositReport> excelUtil = new ExcelUtil<>();

        List<IceDepositReport> iceDepositReports = getIceDepositReports(iceDepositPage);
        excelUtil.exportExcel(fileName, titleName, columnName, iceDepositReports, response);
    }

    @PostMapping("/excelWithoutPage")
    public void excelWithoutPage(@RequestBody IceDepositPage iceDepositPage, HttpServletResponse response){
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        String fileName = "冰柜押金明细表";
        String titleName = "冰柜押金明细表";
        String[] columnName = {"客户编号", "客户名称", "联系人", "联系电话", "门店地址", "服务处", "设备型号", "设备名称", "资产编号", "支付金额", "支付时间", "交易号", "设备价值"};
        ExcelUtil<IceDepositReport> excelUtil = new ExcelUtil<>();

        iceDepositPage.setCurrent(1);
        iceDepositPage.setSize(Integer.MAX_VALUE);
        List<IceDepositReport> iceDepositReports = getIceDepositReports(iceDepositPage);
        excelUtil.exportExcel(fileName, titleName, columnName, iceDepositReports, response);
    }

    private List<IceDepositReport> getIceDepositReports(IceDepositPage iceDepositPage) {
        return iceChestPutRecordService.queryIceDeposits(iceDepositPage).getRecords().stream().map(x -> {
            IceDepositReport iceDepositReport = new IceDepositReport();
            BeanUtils.copyProperties(x, iceDepositReport);
            iceDepositReport.setPayTimeStr(new DateTime(x.getPayTime()).toString("YYYY-MM-dd HH:mm"));
            return iceDepositReport;
        }).collect(Collectors.toList());
    }
}
