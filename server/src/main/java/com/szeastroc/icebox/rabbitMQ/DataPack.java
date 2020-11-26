package com.szeastroc.icebox.rabbitMQ;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class DataPack implements Serializable {
    private String methodName;
    private Object obj;
}