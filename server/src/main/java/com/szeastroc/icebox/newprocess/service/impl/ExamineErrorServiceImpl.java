package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.user.vo.SessionUserInfoVo;
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
import com.szeastroc.icebox.newprocess.service.ExamineErrorService;
import com.szeastroc.icebox.newprocess.dao.ExamineErrorMapper;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
        DateTime now = new DateTime();
        Date monthStart = now.dayOfMonth().withMinimumValue().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
        Date monthEnd = now.dayOfMonth().withMaximumValue().withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
        List<ExamineError> examineErrors = examineErrorMapper.selectList(Wrappers.<ExamineError>lambdaQuery().eq(ExamineError::getCreateUserId,examineError.getCreateUserId()).eq(ExamineError::getBoxAssetid,examineError.getBoxAssetid()).ge(ExamineError::getCreateTime, monthStart).le(ExamineError::getCreateTime, monthEnd).ne(ExamineError::getPassStatus,2));
        if(examineErrors != null && examineErrors.size() > 0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"本月该冰柜已提交无法巡检,不需要再次添加");
        }
        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, examineError.getBoxAssetid()));
        if(iceBox == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"冰柜不存在");
        }
        examineError.setBoxId(iceBox.getId());
        examineError.setCreateTime(new Date());

        Map<Integer, SessionUserInfoVo> userInfoVoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(examineError.getDeptId()));
        SessionUserInfoVo userInfoVo1 = userInfoVoMap.get(0);
        SessionUserInfoVo userInfoVo2 = userInfoVoMap.get(1);

        if(userInfoVo1 == null || userInfoVo1.getId() == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"找不到组长");
        }
        if(userInfoVo2 == null || userInfoVo2.getId() == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"找不到服务处经理");
        }

        examineError.setSendUserId1(userInfoVo1.getId());
        examineError.setSendUserName1(userInfoVo1.getRealname());
        examineError.setSendUserId2(userInfoVo2.getId());
        examineError.setSendUserName2(userInfoVo2.getRealname());
        examineError.setPassStatus(0);
        examineErrorMapper.insert(examineError);

        //发送代办
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMddHHmmss");
        String relateCode =iceBox.getAssetId()+"_"+examineError.getId()+"_"+userInfoVo1.getId()+"_"+prefix;
        NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                                .backlogName(iceBox.getAssetId()+"_冰柜无法巡检")
                                .noticeTypeEnum(NoticeTypeEnum.ICE_CANT_EXAMINE)
                                .relateCode(relateCode)
                                .sendUserId(userInfoVo1.getId())
                                .build();
        // 创建通知
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

        String relateCode2 =iceBox.getAssetId()+"_"+examineError.getId()+"_"+userInfoVo2.getId()+"_"+prefix;
        noticeBacklogRequestVo.setSendUserId(userInfoVo2.getId());
        noticeBacklogRequestVo.setRelateCode(relateCode2);
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

        String relateCode3 =iceBox.getAssetId()+"_"+examineError.getId()+"_"+examineError.getCreateUserId()+"_"+prefix;
        noticeBacklogRequestVo.setSendUserId(examineError.getCreateUserId());
        noticeBacklogRequestVo.setRelateCode(relateCode3);
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

    }
}




