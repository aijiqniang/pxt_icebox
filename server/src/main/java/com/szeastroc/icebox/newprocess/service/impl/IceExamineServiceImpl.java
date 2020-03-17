package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceExamineDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.enums.ExamineEnums;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExamineRequest;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.vo.SessionUserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IceExamineServiceImpl extends ServiceImpl<IceExamineDao, IceExamine> implements IceExamineService {

    @Autowired
    private IceExamineDao iceExamineDao;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private IceBoxExtendDao iceBoxExtendDao;

    @Override
    public void doExamine(IceExamine iceExamine) {

        if (!iceExamine.validate()) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "参数不完整");
        }

        iceExamineDao.insert(iceExamine);
        Integer iceExamineId = iceExamine.getId();
        Integer iceBoxId = iceExamine.getIceBoxId();

        IceBoxExtend iceBoxExtend = new IceBoxExtend();
        iceBoxExtend.setLastExamineId(iceExamineId);
        iceBoxExtend.setLastExamineTime(new Date());

        iceBoxExtendDao.update(iceBoxExtend,Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId,iceBoxId));
    }

    @Override
    public IPage<IceExamineVo> findExamine(IceExamineRequest iceExamineRequest) {
        LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();
        wrapper.orderByDesc(IceExamine::getCreateTime);

        Integer iceBoxId = iceExamineRequest.getIceBoxId();

        if (iceBoxId == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        wrapper.eq(IceExamine::getIceBoxId,iceBoxId);

        Integer createBy = iceExamineRequest.getCreateBy();

        if (createBy == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        wrapper.eq(IceExamine::getCreateBy, createBy);

        String storeNumber = iceExamineRequest.getStoreNumber();
        if (StringUtils.isBlank(storeNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        wrapper.eq(IceExamine::getStoreNumber,storeNumber);
        Date createTime = iceExamineRequest.getCreateTime();
        if (createTime != null) {
            Date date = new Date(createTime.getTime());
            date.setTime(date.getTime() + 24 * 60 * 60 * 1000);
            wrapper.ge(IceExamine::getCreateTime, createTime).le(IceExamine::getCreateTime, date);
        }

        IPage<IceExamine> iPage = iceExamineDao.selectPage(iceExamineRequest, wrapper);


        List<IceExamine> records = iPage.getRecords();

        IPage<IceExamineVo> page = new Page<>();

        if (CollectionUtil.isNotEmpty(records)) {

            List<Integer> collect = records.stream().map(IceExamine::getCreateBy).distinct().collect(Collectors.toList());

            Map<Integer, SessionUserInfoVo> map = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(collect));

            StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(storeNumber));

            page = iPage.convert(iceExamine -> {

                SessionUserInfoVo sessionUserInfoVo = map.get(createBy);
                return IceExamineVo.builder()
                        .id(iceExamine.getId())
                        .createBy(iceExamine.getCreateBy())
                        .createName(sessionUserInfoVo.getRealname())
                        .displayImage(iceExamine.getDisplayImage())
                        .exteriorImage(iceExamine.getExteriorImage())
                        .createTime(iceExamine.getCreateTime())
                        .storeName(storeInfoDtoVo.getStoreName())
                        .storeNumber(storeNumber)
                        .iceBoxId(iceExamine.getIceBoxId())
                        .build();

            });
        }
        return page;
    }


    @Override
    public IceExamineVo findOneExamine(IceExamineRequest iceExamineRequest) {
        String storeNumber = iceExamineRequest.getStoreNumber();
        Integer createBy = iceExamineRequest.getCreateBy();
        Integer type = iceExamineRequest.getType();
        Integer iceBoxId = iceExamineRequest.getIceBoxId();

        if (iceBoxId == null || createBy == null || StringUtils.isBlank(storeNumber) || type == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();

        wrapper.eq(IceExamine::getCreateBy, createBy).eq(IceExamine::getStoreNumber, storeNumber).eq(IceExamine::getIceBoxId,iceBoxId).last("limit 1");

        if (type.equals(ExamineEnums.ExamineTime.FIRST_TIME.getType())) {
            // 第一次巡检

            wrapper.orderByAsc(IceExamine::getCreateTime);
        } else {
            // 最后一次巡检
            wrapper.orderByDesc(IceExamine::getCreateTime);
        }

        IceExamine iceExamine = iceExamineDao.selectOne(wrapper);

        IceExamineVo iceExamineVo;

        if (iceExamine != null) {

            ArrayList<Integer> list = new ArrayList<>();
            list.add(createBy);
            Map<Integer, SessionUserInfoVo> map = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(list));
            SessionUserInfoVo sessionUserInfoVo = map.get(createBy);
            StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(storeNumber));

            iceExamineVo = IceExamineVo.builder()
                    .id(iceExamine.getId())
                    .createBy(iceExamine.getCreateBy())
                    .createName(sessionUserInfoVo.getRealname())
                    .displayImage(iceExamine.getDisplayImage())
                    .exteriorImage(iceExamine.getExteriorImage())
                    .createTime(iceExamine.getCreateTime())
                    .storeName(storeInfoDtoVo.getStoreName())
                    .storeNumber(storeNumber)
                    .iceBoxId(iceExamine.getIceBoxId())
                    .build();
        } else {
            iceExamineVo = null;
        }
        return iceExamineVo;
    }
}
