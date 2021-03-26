package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.MemberInfoVo;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.feign.visit.FeignIceboxQueryClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.enums.VisitCycleEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxPutReportExcelVo;
import com.szeastroc.icebox.util.JudgeCustomerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IceBoxPutApplyExamineConsumer {


    @Autowired
    private IceBoxService iceBoxService;

    @RabbitListener(queues = MqConstant.ICE_BOX_PUT_APPLY_Q, containerFactory = "iceboxPutApplyContainer")
    public void task(IceBoxRequest iceBoxRequest) {
        try {
            iceBoxService.checkIceBoxNew(iceBoxRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   }