package com.szeastroc.icebox.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by hbl
 * 2020.04.22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IceBoxRequest {

    private String applyNumber;
    private Integer status;
    private Integer updateBy;
}
