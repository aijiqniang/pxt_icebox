package com.szeastroc.icebox.newprocess.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.CodeVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.rabbitMQ.DirectProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    private final RabbitTemplate rabbitTemplate;

    /**
     * @Date: 2020/6/12 16:54 xiao
     * ????????????excel
     */
    @PostMapping("/exportExcel")
    public CommonResponse<Void> exportExcel(@RequestBody IceBoxPage iceBoxPage) throws Exception {

        String prefix = "ice_export_excel_";
        String jobName = "??????????????????";
        // ???session ?????????????????????
        Integer exportRecordId = setExportRecord(iceBoxPage, prefix, jobName);
        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.EXPORT_EXCEL_QUEUE, exportRecordId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * @Date: 2020/9/7 17:36 xiao
     * ??????????????????
     */
    @GetMapping("/codeGenerator")
    public void codeGenerator(Integer num, HttpServletResponse response) throws IOException {

        if (num == null || num < 1 || num > 100000) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????? 1-100000 ?????????");
        }
        // List<String> list = Lists.newArrayList();
        HashSet<String> sets = Sets.newHashSet();
        String string = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";//????????????0-9 ??? ???????????????
        int length = string.length();
        for (int k = 1; k <= num; k++) {
            StringBuilder sb = new StringBuilder(); //???????????? StringBuilder ??????sb ?????? ?????????
            sb.append("http://bx.szeastroc.com/");
            for (int i = 0; i < 13; i++) {
                Random random = new Random();//????????????????????????????????????
                int index = random.nextInt(length);//??????[0,string.length)?????????int???    ?????????????????????
                char ch = string.charAt(index);//charAt() : ???????????????????????? char ???   ==????????????char????????????ch
                sb.append(ch);// append(char c) :??? char ????????????????????????????????????????????????  ==????????????????????????ch????????????
            }
            String sbStr = sb.toString();
            if (sets.contains(sbStr)) {
                k--;
                continue;
            }
            sets.add(sbStr);
//            if (list.contains(sbStr)) {
//                k--;
//                continue;
//            }
//            list.add(sbStr);
            // System.out.println(sb.toString());
        }

        List<CodeVo> list = Lists.newArrayListWithCapacity(sets.size());
        for (String codeLink : sets) {
            CodeVo codeVo = CodeVo.builder().codeLink(codeLink).build();
            list.add(codeVo);
        }
        // ???????????? ?????????????????????swagger ??????????????????????????????????????????????????????postman
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // ??????URLEncoder.encode???????????????????????? ?????????easyexcel????????????
        String fileName = URLEncoder.encode("???????????????", "UTF-8");
        // String fileName = "??????demo";
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), CodeVo.class).sheet("??????").doWrite(list);

    }

    /**
     * @Date: 2021/1/7 9:46 xiao
     * ???????????????????????? ???excel
     */
    @PostMapping("/exportChangeRecord")
    public CommonResponse<Void> exportChangeRecord(@RequestBody IceBoxPage iceBoxPage) throws Exception {

        String prefix = "export_change_record_";
        String jobName = "????????????????????????";
        Integer exportRecordId = setExportRecord(iceBoxPage, prefix, jobName);
        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.EXPORT_CHANGE_RECORD_QUEUE, exportRecordId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    private Integer setExportRecord(@RequestBody IceBoxPage iceBoxPage, String prefix, String jobName) {
        // ???session ?????????????????????
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        Integer userId = userManageVo.getSessionUserInfoVo().getId();
        String userName = userManageVo.getSessionUserInfoVo().getRealname();
        // ???????????????????????????
        List<Integer> deptIdList = FeignResponseUtil.getFeignData(feignDeptClient.findDeptInfoIdsBySessionUser());
        if (CollectionUtils.isEmpty(deptIdList)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????");
        }
        // ???????????????????????????
        String key = prefix + userId;
        String value = jedisClient.get(key);
        if (StringUtils.isNotBlank(value)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???????????????-?????????????????????????????????????????????????????????(??????3??????)...");
        }
        jedisClient.setnx(key, userId.toString(), 180);
        // ??????????????????
        iceBoxPage.setDeptIdList(deptIdList.contains(1) ? Lists.newArrayList(1) : deptIdList);
        // ??????????????????????????????  exportRecordId
        String param = JSON.toJSONString(iceBoxPage);
        String redisKey=key+"exp";
        jedisClient.set(redisKey, param, 180, TimeUnit.SECONDS);
        Integer integer = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecordsRedis(userId, userName, redisKey, jobName));
        return integer;
    }

}
