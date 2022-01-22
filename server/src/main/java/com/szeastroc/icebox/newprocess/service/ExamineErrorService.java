package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.entity.ExamineError;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 *
 */
public interface ExamineErrorService extends IService<ExamineError> {

    void insert(ExamineError examineError);
}
