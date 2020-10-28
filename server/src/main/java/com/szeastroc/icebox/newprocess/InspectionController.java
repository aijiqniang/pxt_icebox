package com.szeastroc.icebox.newprocess;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.impl.GroupMemberInspectionServiceImpl;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.icebox.newprocess.vo.StoreVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName: InspectionController
 * @Description: 资产巡检
 * @Author: 陈超
 * @Date: 2020/10/27 10:12
 **/
@RestController
@RequestMapping("/inspection")
public class InspectionController {
    @Autowired
    private GroupMemberInspectionServiceImpl groupMemberInspectionService;

    @RequestMapping("report")
    public CommonResponse<List<InspectionReportVO>> query(@RequestParam Integer deptId, @RequestParam Integer type) {
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, InspectionServiceFactory.get(type).report(deptId));
    }

    @RequestMapping("getStoreByUserId")
    public CommonResponse<List<StoreVO>> getStoreByUserId(@RequestParam Integer userId) {
        List<StoreVO> vos = groupMemberInspectionService.getStoreByUserId(userId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,vos);
    }

    @RequestMapping("getStoreByDeptId")
    public CommonResponse<List<StoreVO>> getStoreByDeptId(@RequestParam Integer deptId) {
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }
}
