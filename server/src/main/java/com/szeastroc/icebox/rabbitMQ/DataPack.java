package com.szeastroc.icebox.rabbitMQ;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author xiao
 * @Date create in 2020/6/14 14:14
 * @Description:
 */
@Getter
@Setter
public class DataPack {
    private String methodName;
    private Object obj;
}
