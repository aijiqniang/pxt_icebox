package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;
import org.apache.ibatis.annotations.Param;
import org.apache.shiro.crypto.hash.Hash;

import java.util.HashMap;
import java.util.List;

/**
 * (DisplayShelf)表数据库访问层
 *
 * @author chenchao
 * @since 2021-05-28 09:36:31
 */
public interface DisplayShelfDao extends BaseMapper<DisplayShelf> {

    List<DisplayShelf> selectDetails();

    IPage<DisplayShelf> selectPage(DisplayShelfPage page);

    List<DisplayShelf.DisplayShelfType> selectType(@Param("supplierNumber") String supplierNumber);

    List<DisplayShelf> noPutShelves(@Param("serviceId") Integer serviceId, @Param("typeArr") String[] typeArr);

    List<DisplayShelf.DisplayShelfType> typeCount(@Param("customerNumber") String customerNumber);

    List<DisplayShelf.DisplayShelfType> customerDetail(@Param("customerNumber") String customerNumber);

}
