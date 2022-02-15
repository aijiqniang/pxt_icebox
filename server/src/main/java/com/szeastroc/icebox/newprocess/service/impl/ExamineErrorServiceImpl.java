package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.user.vo.DeptVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SessionUserInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleDeptInfoVo;
import com.szeastroc.common.entity.visit.NoticeBacklogRequestVo;
import com.szeastroc.common.entity.visit.enums.NoticeTypeEnum;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignOutBacklogClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.ExamineError;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.IceAlarmTypeEnum;
import com.szeastroc.icebox.newprocess.service.ExamineErrorService;
import com.szeastroc.icebox.newprocess.mapper.ExamineErrorMapper;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Service
public class ExamineErrorServiceImpl extends ServiceImpl<ExamineErrorMapper, ExamineError>
implements ExamineErrorService{
    @Resource
    private ExamineErrorMapper examineErrorMapper;
    @Resource
    private IceBoxDao iceBoxDao;
    @Resource
    private FeignDeptClient feignDeptClient;
    @Resource
    private FeignUserClient feignUserClient;
    @Resource
    private FeignCacheClient feignCacheClient;
    @Resource
    private FeignOutBacklogClient feignOutBacklogClient;

    @Override
    public void insert(ExamineError examineError) {
        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, examineError.getBoxAssetid()));
        if(iceBox == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"冰柜不存在");
        }
        examineError.setBoxId(iceBox.getId());
        examineError.setCreateTime(new Date());

        Map<Integer, SessionUserInfoVo> userInfoVoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(examineError.getDeptId()));
        SessionUserInfoVo userInfoVo1 = userInfoVoMap.get(0);
        SessionUserInfoVo userInfoVo2 = userInfoVoMap.get(1);

        if(userInfoVo1 == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"找不到组长");
        }
        if(userInfoVo2 == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"找不到服务处经理");
        }

        examineError.setSendUserId1(userInfoVo1.getId());
        examineError.setSendUserName1(userInfoVo1.getRealname());
        examineError.setPassStatus(0);
        examineErrorMapper.insert(examineError);

        //发送代办
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMddHHmmss");
        String relateCode =iceBox.getAssetId()+"_"+examineError.getId()+"_"+prefix;
                NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(iceBox.getAssetId()+"_冰柜无法巡检:")
                .noticeTypeEnum(NoticeTypeEnum.ICE_CANT_EXAMINE)
                .relateCode(relateCode)
                .sendUserId(userInfoVo1.getId())
                .build();
        // 创建通知
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

    }
}




