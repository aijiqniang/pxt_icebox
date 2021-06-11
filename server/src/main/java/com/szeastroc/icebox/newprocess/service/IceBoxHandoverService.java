package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBoxHandover;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxHandoverPage;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface IceBoxHandoverService extends IService<IceBoxHandover> {

    Map<String, List<Map<String,Object>>> findByUserid(Integer userId,Integer receiveUserId, String storeName);

    void sendHandOverRequest(List<IceBoxHandover> iceBoxHandovers);

    void passHandOverRequest(List<Integer> ids);

    void rejectHandOverRequest(List<Integer> ids);

    IPage<IceBoxHandover> findByPage(IceBoxHandoverPage iceBoxHandoverPage);

    CommonResponse exportIceHandover(IceBoxHandoverPage iceBoxHandoverPage);

    Map<String, List<Map<String, Object>>> findByUseridNew(Integer sendUserId, Integer receiveUserId, String storeName,String relateCode);

    void updateResponseMan(List<Integer> iceboxIds);
}
