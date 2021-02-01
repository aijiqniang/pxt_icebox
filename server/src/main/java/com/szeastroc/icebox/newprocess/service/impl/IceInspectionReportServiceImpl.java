package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.dao.IceInspectionReportDao;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 冰柜巡检报表 (TIceInspectionReport)表服务实现类
 *
 * @author chenchao
 * @since 2020-12-16 16:46:21
 */
@Service
public class IceInspectionReportServiceImpl extends ServiceImpl<IceInspectionReportDao, IceInspectionReport> implements IceInspectionReportService {

    /**
     * 获取业务员当月报表
     *
     * @param userId
     * @return
     */
    @Override
    public IceInspectionReport getCurrentMonthReport(Integer userId) {
        LambdaQueryWrapper<IceInspectionReport> wrapper = Wrappers.<IceInspectionReport>lambdaQuery();
        wrapper.eq(IceInspectionReport::getUserId, userId).eq(IceInspectionReport::getInspectionDate, new DateTime().toString("yyyy-MM")).last("limit 1");
        return this.getOne(wrapper);
    }

    /**
     * 获取直接在服务处的业务员的报表
     *
     * @param deptId
     * @return
     */
    @Override
    public List<IceInspectionReport> getInService(Integer deptId) {
        LambdaQueryWrapper<IceInspectionReport> wrapper = Wrappers.<IceInspectionReport>lambdaQuery();
        wrapper.eq(IceInspectionReport::getServiceDeptId, deptId).eq(IceInspectionReport::getInspectionDate, new DateTime().toString("yyyy-MM")).isNull(IceInspectionReport::getGroupDeptId);
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 获取服务处下各组的总和
     *
     * @param deptId 服务处id
     * @return
     */
    @Override
    public List<InspectionReportVO> getGroupReports(Integer deptId) {
        return this.baseMapper.getGroupReports(deptId);
    }

    /**
     * 获取大区下个服务处的总和
     *
     * @param deptId
     * @return
     */
    @Override
    public List<InspectionReportVO> getServiceReports(Integer deptId) {
        return this.baseMapper.getServiceReports(deptId);
    }

    @Override
    public void truncate() {
        this.baseMapper.truncate();
    }
}