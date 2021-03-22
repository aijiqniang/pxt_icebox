package com.szeastroc.icebox.oldprocess.controller;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.annotation.IgnoreResponseAdvice;
import com.szeastroc.common.bean.Result;
import com.szeastroc.common.controller.BaseController;
import com.szeastroc.common.exception.DongPengException;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


/**
 * Hisense对接类
 * @author yuqi9
 * @since 2019/5/23
 */
@Slf4j
@RestController
@RequestMapping("/external/hisense")
public class HisenseController extends BaseController {


    @Resource
    private IceEventRecordService iceEventRecordService;

    /**
     * 接收hisense推送冰箱数据
     * @author island
     * @param hisenseDTO;
     * @return com.szeastroc.common.bean.Result
     * @since 2019/5/23
     */
    @IgnoreResponseAdvice
    @RequestMapping(value = "/eventPush")
    public Result EventPush(@RequestBody HisenseDTO hisenseDTO){
        try{
            if(hisenseDTO.validate()){
                return renderFailed("参数错误");
            }
            //调用推送业务
            iceEventRecordService.EventPush(hisenseDTO);
        }catch (DongPengException e){
            log.info(e.getMessage());
            return renderFailed(e.getMessage());
        }catch (Exception e){
            log.info("推送冰箱数据接口异常", e);
            e.printStackTrace();
            return renderError("接收异常");
        }
        log.info("推送结果:"+ JSON.toJSONString(render()));
        return render();
    }

}
