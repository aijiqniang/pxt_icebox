package com.szeastroc.icebox.oldprocess.controller.manage;

import com.szeastroc.common.bean.Result;
import com.szeastroc.common.controller.BaseController;
import com.szeastroc.icebox.oldprocess.entity.IceChestInfoImport;
import com.szeastroc.icebox.oldprocess.service.IceChestInfoService;
import com.szeastroc.icebox.util.excel.ExcelUtil;
import com.szeastroc.icebox.oldprocess.vo.IceChestInfoExcelVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author yuqi9
 * @since 2019/5/27
 */
@Slf4j
@RestController
@RequestMapping("/iceChestInfo")
public class ImportExcelController extends BaseController {

    @Resource
    private IceChestInfoService iceChestInfoService;

    @RequestMapping("/importBlack")
    public Result importCodeFile(@RequestParam MultipartFile file, HttpServletRequest request){
        log.info("FILE"+file);
        File tmpFile=  null;
        if(file.isEmpty()){
            return renderError("文件为空,请重新上传");
        }
        if(!StringUtils.containsIgnoreCase(file.getOriginalFilename(), ".xls") && !StringUtils.containsIgnoreCase(file.getOriginalFilename(), ".xlsx")){
            return renderError("文件格式不正确");
        }
        String result = "";
        try {
            //转换file
            String realPath = request.getSession().getServletContext().getRealPath("");
            tmpFile = new File(realPath, file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(file.getInputStream(),tmpFile);

            ExcelUtil<IceChestInfoExcelVo> excel = new ExcelUtil<>(new IceChestInfoExcelVo());
            List<IceChestInfoExcelVo> list = excel.readFromFile(tmpFile, 0);
            IceChestInfoImport iceChestInfoImport = new IceChestInfoImport();
            iceChestInfoImport.setFilePath(tmpFile.getPath());
            iceChestInfoImport.setName(file.getOriginalFilename());
            iceChestInfoImport.setImportNo(UUID.randomUUID().toString());
            result = iceChestInfoService.importIceInfoExcelVo(list,iceChestInfoImport);
            if(!result.equals("success")){
                return renderError(result);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return renderError("上传异常");
        } catch (IOException e) {
            e.printStackTrace();
            return renderError("上传异常");
        }catch (MyBatisSystemException e){
            return renderError("不允许重复导入");
        }catch (Exception e) {
            e.printStackTrace();
            return renderError("上传异常");
        }
        return render();
    }

}
