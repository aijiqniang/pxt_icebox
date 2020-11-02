package com.szeastroc.icebox.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @Author xiao
 * @Date create in 2020/6/14 14:14
 * @Description:
 */
@Getter
@Setter
public class DataPack implements Serializable {
    private String methodName;
    private Object obj;
}
