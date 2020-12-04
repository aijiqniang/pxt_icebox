package com.szeastroc.icebox.newprocess.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamineStatusVo {

    private Integer type;

    private String message;

}
