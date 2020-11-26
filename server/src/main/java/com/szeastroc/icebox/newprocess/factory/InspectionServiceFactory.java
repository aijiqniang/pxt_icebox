package com.szeastroc.icebox.newprocess.factory;

import com.szeastroc.icebox.newprocess.service.InspectionService;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: InspectionServiceFactory
 * @Description:
 * @Author: 陈超
 * @Date: 2020/10/27 17:12
 **/
public class InspectionServiceFactory {

    private static Map<Integer, InspectionService> inspectionServiceHashMap = new HashMap<>();

    public static void register(Integer type, InspectionService inspectionService) {
        inspectionServiceHashMap.put(type, inspectionService);
    }

    /**
     * @param type 1业代 2组长 3服务处经理 4大区总
     * @return
     */
    public static InspectionService get(Integer type) {
        return inspectionServiceHashMap.get(type);
    }

}
