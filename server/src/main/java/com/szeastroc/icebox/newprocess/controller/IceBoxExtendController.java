package com.szeastroc.icebox.newprocess.controller;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.rabbitMQ.DataPack;
import com.szeastroc.icebox.rabbitMQ.DirectProducer;
import com.szeastroc.icebox.rabbitMQ.MethodNameOfMQ;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.visit.client.FeignExportRecordsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author xiao
 * @Date create in 2020/6/12 16:53
 * @Description:
 */
@Slf4j
@RestController
@RequestMapping("/iceBoxExtend")
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxExtendController {

    private final DirectProducer directProducer;
    //    private final IceBoxService iceBoxService;
    private final FeignUserClient feignUserClient;
    private final JedisClient jedisClient;
    private final FeignExportRecordsClient feignExportRecordsClient;
    private final FeignDeptClient feignDeptClient;

    /**
     * @Date: 2020/6/12 16:54 xiao
     * 导出冰柜excel
     */
    @PostMapping("/exportExcel")
    public CommonResponse<String> exportExcel(IceBoxPage iceBoxPage) throws Exception {

        // 从session 中获取用户信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        Integer userId = userManageVo.getSessionUserInfoVo().getId();
        String userName = userManageVo.getSessionUserInfoVo().getRealname();
        // 控制导出的请求频率
        String key = "ice_export_excel_" + userId;
        String value = jedisClient.get(key);
//        if (StringUtils.isNotBlank(value)) {
//            throw new NormalOptionException(Constants.API_CODE_FAIL, "请到“首页-下载任务”中查看导出结果，请勿频繁操作(间隔3分钟)...");
//        }
//        jedisClient.setnx(key, userId.toString(), 180);

        // 塞入数据到下载列表中  exportRecordId
        Integer exportRecordId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userId, userName, JSON.toJSONString(iceBoxPage), "冰柜记录导出"));
        iceBoxPage.setExportRecordId(exportRecordId);
        List<Integer> deptIdList = FeignResponseUtil.getFeignData(feignDeptClient.findDeptInfoIdsBySessionUser());
        iceBoxPage.setDeptIdList(deptIdList);
        DataPack dataPack = new DataPack();
        dataPack.setMethodName(MethodNameOfMQ.EXPORT_EXCEL_METHOD);
        dataPack.setObj(iceBoxPage);
        directProducer.sendMsg(MqConstant.directRoutingKey, dataPack);
        //iceBoxService.exportExcel(iceBoxPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

}
