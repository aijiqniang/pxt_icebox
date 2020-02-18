package com.szeastroc.icebox.oldprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.oldprocess.entity.MarketArea;

import java.util.List;

/**
 * Created by Tulane
 * 2019/5/28
 */
public interface MarketAreaService extends IService<MarketArea> {

    void updateStoreMarketAreaList(List<MarketArea> marketAreas);
}
