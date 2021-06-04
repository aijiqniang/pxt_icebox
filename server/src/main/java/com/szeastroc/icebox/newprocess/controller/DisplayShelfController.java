package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.enums.DisplayShelfTypeEnum;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.SupplierDisplayShelfVO;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfStockRequest;
import com.szeastroc.icebox.newprocess.vo.request.SignShelfRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * (DisplayShelf)表控制层
 *
 * @author chenchao
 * @since 2021-05-28 09:36:31
 */
@Api(tags = {"陈列架接口"} )
@RestController
@RequestMapping("displayShelf")
public class DisplayShelfController {

    @Autowired
    private DisplayShelfService displayShelfService;
    @Autowired
    private DisplayShelfPutApplyService displayShelfPutApplyService;

    @PostMapping("page")
    @ApiOperation(value = "陈列货架分页", notes = "陈列货架分页", produces = "application/json")
    public CommonResponse<IPage<DisplayShelf>> page(@RequestBody DisplayShelfPage page){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,displayShelfService.selectPage(page));
    }

    @PostMapping("details")
    @ApiOperation(value = "陈列货架详情列表", notes = "陈列货架详情列表", produces = "application/json")
    public CommonResponse<IPage<DisplayShelf>> details(@RequestBody DisplayShelfPage page){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,displayShelfService.selectDetails(page));
    }

    @GetMapping("downloadTmp")
    @ApiOperation(value = "下载陈列架导入模板", notes = "下载陈列架导入模板", produces = "application/json")
    public void downloadTmp(HttpServletResponse response){
        try {
            ClassPathResource resource = new ClassPathResource("/templates/陈列架导入模板.xlsx");
            InputStream inputStream = resource.getInputStream();
            // 设置输出的格式
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("陈列架导入模板.xlsx", "UTF-8"));
            // 循环取出流中的数据
            byte[] b = new byte[100];
            int len;
            while ((len = inputStream.read(b)) > 0) {
                response.getOutputStream().write(b, 0, len);
            }
            inputStream.close();
        } catch (IOException e) {
            throw new NormalOptionException(Constants.API_CODE_FAIL,"下载模板失败");
        }
    }

    @PostMapping("import")
    @ApiOperation(value = "导入陈列架数据", notes = "导入陈列架数据", produces = "application/json")
    public CommonResponse importData(@RequestParam MultipartFile file){
        displayShelfService.importData(file);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("types")
    @ApiOperation(value = "获取陈列货架类型", notes = "获取陈列货架类型", produces = "application/json")
    public CommonResponse types(){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null, Arrays.stream(DisplayShelfTypeEnum.values()).map(o->{
            HashMap<Object, Object> hashMap = new HashMap<>();
            hashMap.put(o.getDesc(),o.getType());
            return hashMap;
        }).collect(Collectors.toList()));
    }

    @PostMapping("canPut")
    @ApiOperation(value = "获取客户可投放货架", notes = "获取客户可投放货架", produces = "application/json")
    public CommonResponse<List<SupplierDisplayShelfVO>> canPut(@RequestBody ShelfStockRequest request){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,displayShelfService.canPut(request));
    }

    @PostMapping("shelfPut")
    @ApiOperation(value = "陈列架投放", notes = "陈列架投放", produces = "application/json")
    public CommonResponse shelfPut(@RequestBody ShelfPutModel model){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,displayShelfService.shelfPut(model));
    }

    @PostMapping("sign")
    @ApiOperation(value = "签收陈列货架", notes = "签收陈列货架", produces = "application/json")
    public CommonResponse sign(@RequestBody SignShelfRequest request){
        displayShelfPutApplyService.sign(request);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

}
