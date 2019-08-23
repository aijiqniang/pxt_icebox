package com.szeastroc.icebox.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.dao.MarketAreaDao;
import com.szeastroc.icebox.entity.MarketArea;
import com.szeastroc.icebox.service.MarketAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by Tulane
 * 2019/5/28
 */
@Service
public class MarketAreaServiceImpl extends ServiceImpl<MarketAreaDao, MarketArea> implements MarketAreaService {

    @Autowired
    private MarketAreaDao marketAreaDao;

    @Transactional(value = "transactionManager")
    @Override
    public void updateStoreMarketAreaList(List<MarketArea> marketAreas) {
        for (MarketArea marketArea : marketAreas) {
            MarketArea marketAreaTmp = marketAreaDao.selectById(marketArea.getId());
            if(marketAreaTmp == null){
                int oldId = marketArea.getId();
                //如果传入的服务处的id不存在数据库中, 创建此服务处
                marketAreaDao.insert(marketArea);
                marketAreaDao.updateNewKey(oldId, marketArea.getId());
            }
        }
    }
}
