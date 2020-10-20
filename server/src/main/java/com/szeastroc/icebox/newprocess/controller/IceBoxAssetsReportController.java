package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.IceBoxAssetsReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxAssetReportPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author xiao
 * @Date create in 2020/10/20 15:42
 * @Description:
 */
@Slf4j
@RestController
@RequestMapping("/iceBoxReport")
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxAssetsReportController {

    private final IceBoxAssetsReportService iceBoxAssetsReportService;

    /**
     * 分页查找数据
     */
    @PostMapping("/readPage")
    public CommonResponse<IPage> readPage(IceBoxAssetReportPage reportPage) {

        IPage page = iceBoxAssetsReportService.readPage(reportPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,page);
    }

    /**
     * @Date: 2020/10/20 16:33 xiao
     *  将老数据同步到
     */

}
