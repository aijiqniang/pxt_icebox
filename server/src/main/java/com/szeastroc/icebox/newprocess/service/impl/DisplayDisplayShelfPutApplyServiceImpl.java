package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.icebox.enums.IceBoxStatus;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.user.session.MatchRuleVo;
import com.szeastroc.common.entity.user.vo.SysRuleIceDetailVo;
import com.szeastroc.common.feign.user.FeignDeptRuleClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfPutApplyDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.enums.StoreSignStatus;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * <p>
 * 业务员申请表  服务实现类
 * </p>
 *
 * @author 陈超
 * @since 2021-06-01
 */
@Service
public class DisplayDisplayShelfPutApplyServiceImpl extends ServiceImpl<DisplayShelfPutApplyDao, DisplayShelfPutApply> implements DisplayShelfPutApplyService {
    @Autowired
    private FeignDeptRuleClient feignDeptRuleClient;
    @Autowired
    private DisplayShelfService displayShelfService;

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void updateStatus(IceBoxRequest request) {
        DisplayShelfPutApply displayShelfPutApply = this.getOne(Wrappers.<DisplayShelfPutApply>lambdaQuery().eq(DisplayShelfPutApply::getApplyNumber, request.getApplyNumber()));
        Optional.ofNullable(displayShelfPutApply).ifPresent(o -> {
            displayShelfPutApply.setExamineStatus(request.getExamineStatus());
            displayShelfPutApply.setUpdatedBy(request.getUpdateBy());
            displayShelfPutApply.setUpdateTime(new Date());
            this.updateById(displayShelfPutApply);
        });
        //审批通过将冰箱置为投放中状态，商户签收将状态置为已投放
        if (IceBoxStatus.IS_PUTING.getStatus().equals(request.getStatus())) {
            Optional.ofNullable(displayShelfPutApply).ifPresent(o -> {
                //查询冰柜投放规则
                MatchRuleVo matchRuleVo = new MatchRuleVo();
                matchRuleVo.setOpreateType(11);
                matchRuleVo.setDeptId(request.getMarketAreaId());
                matchRuleVo.setType(3);
                SysRuleIceDetailVo approvalRule = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchIceRule(matchRuleVo));
                Optional.ofNullable(approvalRule).ifPresent(rule->{
                    //不需要签收
                    if(!rule.getIsSign()){
                        displayShelfPutApply.setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
                        displayShelfPutApply.setUpdateTime(new Date());
                        this.updateById(displayShelfPutApply);
                        //更改货架状态
                        displayShelfService.doPut(displayShelfPutApply.getApplyNumber());
                    }
                });

            });
        }
    }
}
