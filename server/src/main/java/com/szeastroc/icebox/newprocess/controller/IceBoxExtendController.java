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
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.vo.CodeVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.rabbitMQ.DataPack;
import com.szeastroc.icebox.rabbitMQ.DirectProducer;
import com.szeastroc.icebox.rabbitMQ.MethodNameOfMQ;
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
import java.util.Random;

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
     * 导出冰柜excel
     */
    @PostMapping("/exportExcel")
    public CommonResponse<Void> exportExcel(@RequestBody IceBoxPage iceBoxPage) throws Exception {

        String prefix="ice_export_excel_";
        String jobName="冰柜记录导出";
        // 从session 中获取用户信息
        Integer exportRecordId = setExportRecord(iceBoxPage, prefix, jobName);
        rabbitTemplate.convertAndSend(MqConstant.directExchange,MqConstant.EXPORT_EXCEL_QUEUE,exportRecordId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * @Date: 2020/9/7 17:36 xiao
     * 二维码生成器
     */
    @GetMapping("/codeGenerator")
    public void codeGenerator(Integer num, HttpServletResponse response) throws IOException {

        if (num == null || num < 1 || num > 100000) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "请填写 1-100000 的整数");
        }
        // List<String> list = Lists.newArrayList();
        HashSet<String> sets = Sets.newHashSet();
        String string = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";//保存数字0-9 和 大小写字母
        int length = string.length();
        for (int k = 1; k <= num; k++) {
            StringBuilder sb = new StringBuilder(); //声明一个 StringBuilder 对象sb 保存 验证码
            sb.append("http://bx.szeastroc.com/");
            for (int i = 0; i < 13; i++) {
                Random random = new Random();//创建一个新的随机数生成器
                int index = random.nextInt(length);//返回[0,string.length)范围的int值    作用：保存下标
                char ch = string.charAt(index);//charAt() : 返回指定索引处的 char 值   ==》赋值给char字符对象ch
                sb.append(ch);// append(char c) :将 char 参数的字符串表示形式追加到此序列  ==》即将每次获取的ch值作拼接
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
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("冰柜二维码", "UTF-8");
        // String fileName = "测试demo";
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), CodeVo.class).sheet("模板").doWrite(list);

    }

    /**
     * @Date: 2021/1/7 9:46 xiao
     *  导出冰柜变更记录 到excel
     */
    @PostMapping("/exportChangeRecord")
    public CommonResponse<Void> exportChangeRecord(@RequestBody IceBoxPage iceBoxPage) throws Exception {

        String prefix="export_change_record_";
        String jobName="冰柜变更记录导出";
        Integer exportRecordId = setExportRecord(iceBoxPage, prefix, jobName);
        rabbitTemplate.convertAndSend(MqConstant.directExchange,MqConstant.EXPORT_CHANGE_RECORD_QUEUE,exportRecordId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    private Integer setExportRecord(@RequestBody IceBoxPage iceBoxPage, String prefix, String jobName) {
        // 从session 中获取用户信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        Integer userId = userManageVo.getSessionUserInfoVo().getId();
        String userName = userManageVo.getSessionUserInfoVo().getRealname();
        // 登入者部门权限集合
        List<Integer> deptIdList = FeignResponseUtil.getFeignData(feignDeptClient.findDeptInfoIdsBySessionUser());
        if (CollectionUtils.isEmpty(deptIdList)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "暂无数据权限");
        }
        // 控制导出的请求频率
        String key = prefix + userId;
        String value = jedisClient.get(key);
        if (StringUtils.isNotBlank(value)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "请到“首页-下载任务”中查看导出结果，请勿频繁操作(间隔3分钟)...");
        }
        jedisClient.setnx(key, userId.toString(), 180);
        // 塞入部门集合
        iceBoxPage.setDeptIdList(deptIdList);
        // 塞入数据到下载列表中  exportRecordId
        Integer integer = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userId, userName, JSON.toJSONString(iceBoxPage), jobName));
        return integer;
    }

}
