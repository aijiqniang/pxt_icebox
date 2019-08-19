package com.szeastroc.icebox.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.utils.SpringContextUtil;
import com.szeastroc.icebox.entity.*;
import com.szeastroc.icebox.service.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class IceChestInfoImportThread implements Runnable{

	private List<IceChestInfo> iceChestInfoList;

	private Integer importId;

	public IceChestInfoImportThread(){

	}

	public IceChestInfoImportThread(List<IceChestInfo> iceChestInfoList, Integer importId){
		this.iceChestInfoList = iceChestInfoList;
		this.importId = importId;
	}
	
	@Override
	public void run() {
		int successCount = 0;
		IceChestInfoService iceChestInfoService = SpringContextUtil.getBean(IceChestInfoService.class);
		IceChestInfoImportService iceChestInfoImportService = SpringContextUtil.getBean(IceChestInfoImportService.class);
		IceChestInfoImportErrorService iceChestInfoImportErrorService = SpringContextUtil.getBean(IceChestInfoImportErrorService.class);
		ClientInfoService clientInfoService = SpringContextUtil.getBean(ClientInfoService.class);
		MarketAreaService marketAreaService = SpringContextUtil.getBean(MarketAreaService.class);
		String errorMsg = "";
		IceChestInfoImportError iceChestInfoImportError = null;

		MarketArea marketArea = null;
		for (IceChestInfo iceChestInfo : iceChestInfoList) {
			try{
				//查询黑名单表  是否有   没有则新增 有则修改记录
				IceChestInfo iceChestInfo1 = iceChestInfoService.getOne(Wrappers.<IceChestInfo>lambdaQuery().eq(IceChestInfo::getAssetId, iceChestInfo.getAssetId()));
				if(null != iceChestInfo1){
					//数据已存在
					errorMsg = "冰箱数据已存在";
				}else{
					//新增client数据
					marketArea = marketAreaService.getOne(Wrappers.<MarketArea>lambdaQuery().eq(MarketArea::getName, iceChestInfo.getMarketAreaName()));

					ClientInfo clientInfo = clientInfoService.getOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, iceChestInfo.getPxtId()));

					if(clientInfo == null) {
						clientInfo = new ClientInfo();
						clientInfo.setClientNumber(iceChestInfo.getPxtId());
						clientInfo.setMarketAreaId(marketArea.getId());
						clientInfo.setClientName(iceChestInfo.getJxsName());
						clientInfo.setClientPlace(iceChestInfo.getJxsAddress());
						clientInfo.setContactName(iceChestInfo.getJxsContact());
						clientInfo.setContactMobile(iceChestInfo.getJxsContactMobile());
						clientInfoService.save(clientInfo);
					}
					//新增冰箱数据
					iceChestInfo.setClientId(clientInfo.getId());
					iceChestInfo.setMarketAreaId(marketArea.getId());
					iceChestInfoService.save(iceChestInfo);
					successCount++;
					continue;
				}
			}catch(Exception e){
				errorMsg = "导入异常";
				log.error("导入冰箱异常,冰箱数据:"+ JSON.toJSONString(iceChestInfo));
				e.printStackTrace();
			}
			//新增异常数据`
			iceChestInfoImportError = new IceChestInfoImportError();
			iceChestInfoImportError.setImportId(importId);
			iceChestInfoImportError.setMsg(errorMsg);
			iceChestInfoImportError.setAssetId(iceChestInfo.getAssetId());
			iceChestInfoImportErrorService.save(iceChestInfoImportError);
		}
		//执行完成   更新成功数量   更新状态为导入完成
		IceChestInfoImport iceChestInfoImport = new IceChestInfoImport();
		iceChestInfoImport.setId(importId);
		iceChestInfoImport.setStatus(2);
		iceChestInfoImport.setSuccessNum(successCount);
		iceChestInfoImportService.updateById(iceChestInfoImport);
	}

}
