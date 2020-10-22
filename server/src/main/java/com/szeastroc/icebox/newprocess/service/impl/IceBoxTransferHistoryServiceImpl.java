package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxTransferHistoryDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxTransferHistory;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import com.szeastroc.icebox.newprocess.vo.IceBoxTransferHistoryVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxTransferHistoryPage;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.common.SessionExamineVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IceBoxTransferHistoryServiceImpl extends ServiceImpl<IceBoxTransferHistoryDao, IceBoxTransferHistory> implements IceBoxTransferHistoryService {

    @Autowired
    private IceBoxTransferHistoryDao iceBoxTransferHistoryDao;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private FeignExamineClient feignExamineClient;


    @Override
    public List<IceBoxTransferHistoryVo> findListBySupplierId(Integer supplierId) {
        LambdaQueryWrapper<IceBoxTransferHistory> wrappers = Wrappers.lambdaQuery();
        wrappers.eq(IceBoxTransferHistory::getOldSupplierId,supplierId);
        List<IceBoxTransferHistory> historyList = iceBoxTransferHistoryDao.selectList(wrappers);
        List<IceBoxTransferHistoryVo> historyVoList = new ArrayList<>();
        if(CollectionUtil.isEmpty(historyList)){
            return historyVoList;
        }
        Map<String, List<IceBoxTransferHistory>> historyMap = historyList.stream().collect(Collectors.groupingBy(IceBoxTransferHistory::getTransferNumber));
        for(String transferNumber:historyMap.keySet()){
            IceBoxTransferHistoryVo historyVo = new IceBoxTransferHistoryVo();
            List<IceBoxTransferHistory> list = historyMap.get(transferNumber);
            if(CollectionUtil.isEmpty(list)){
                continue;
            }
            IceBoxTransferHistory history = list.get(0);
            BeanUtil.copyProperties(history,historyVo);
            List<Integer> iceBoxIds = list.stream().map(x -> x.getIceBoxId()).collect(Collectors.toList());
            List<IceBox> iceBoxList = iceBoxDao.selectBatchIds(iceBoxIds);
            historyVo.setIceBoxs(iceBoxList);
            if(history.getIsCheck().equals(1)){
                List<SessionExamineVo.VisitExamineNodeVo> examineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(transferNumber));
                historyVo.setExamineNodeVoList(examineNodeVos);
            }
            historyVoList.add(historyVo);
        }
        return historyVoList;
    }
}
