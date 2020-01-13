package com.szeastroc.icebox.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.entity.MarketArea;
import com.szeastroc.icebox.service.MarketAreaService;
import com.szeastroc.icebox.vo.MarketAreaVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/marketarea")
public class MarketAreaController {

    @Autowired
    private MarketAreaService marketAreaService;

    @GetMapping("/get")
    public CommonResponse<List<MarketAreaVo>> getMarketAreaVos(){
        List<MarketArea> marketAreas = marketAreaService.list();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,
                marketAreas.stream().map(MarketArea::convertVo).collect(Collectors.toList()));
    }
}
