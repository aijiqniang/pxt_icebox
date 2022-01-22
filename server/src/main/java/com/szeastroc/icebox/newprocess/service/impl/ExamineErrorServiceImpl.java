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

        Map<Integer, SessionDeptInfoVo> deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(examineError.getDeptId()));
        SessionDeptInfoVo group = deptInfoVoMap.get(1);
        SessionDeptInfoVo service = deptInfoVoMap.get(2);

        Map<Integer, SessionUserInfoVo> userInfoVoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(examineError.getDeptId()));
        SessionUserInfoVo userInfoVo1 = userInfoVoMap.get(0);
        SessionUserInfoVo userInfoVo2 = userInfoVoMap.get(1);

        if(examineError.getIsLeader().equals(1)){
            //领导
            if(examineError.getDeptId().equals(group.getId())){
                //组长 给服务处经理发个通知审批
                examineError.setSendUserId1(userInfoVo1.getId());
                examineError.setSendUserName1(userInfoVo1.getRealname());
                examineError.setPassStatus(0);

            }else if(examineError.getDeptId().equals(service.getId())){
                //服务处经理 直接通过
                examineError.setPassStatus(1);
            }

        }else {
            if(examineError.getDeptId().equals(group.getId())){
                //业代 给组长一级审批  给服务处经理通知不需要审批

            }
        }
        examineErrorMapper.insert(examineError);

        //发送代办
        String relateCode ="";
                NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(iceBox.getAssetId()+"_冰柜报警:"+ IceAlarmTypeEnum.DISTANCE.getDesc())
                .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                .relateCode(relateCode)
                .sendUserId(iceBox.getResponseManId())
                .build();
        // 创建通知
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

    }
}




