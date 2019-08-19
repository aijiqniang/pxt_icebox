package com.szeastroc.icebox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author yuqi9
 * @since 2019/5/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_ice_chest_info_import")
public class IceChestInfoImport {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String importNo;

    private String name;

    private String filePath;

    private Date createTime;

    private Integer totalNum;

    private Integer successNum;

    private Integer status;

}
