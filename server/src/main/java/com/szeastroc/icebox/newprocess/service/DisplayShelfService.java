package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.vo.DisplayShelfPutApplyVo;
import com.szeastroc.icebox.newprocess.vo.SupplierDisplayShelfVO;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfStockRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * (DisplayShelf)表服务接口
 *
 * @author chenchao
 * @since 2021-05-28 09:36:31
 */
public interface DisplayShelfService extends IService<DisplayShelf> {

    List<DisplayShelf> selectDetails();

    IPage<DisplayShelf> selectPage(DisplayShelfPage page);

    void importData(MultipartFile file);

    List<SupplierDisplayShelfVO> canPut(ShelfStockRequest request);

    void doPut(String applyNumber);

    List<DisplayShelf.DisplayShelfType> customerTotalCount(String customerNumber);

    List<DisplayShelf.DisplayShelfType> customerDetail(String customerNumber);

    List<DisplayShelfPutApplyVo> examineDetails(String code);
}
