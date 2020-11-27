package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class IceChangeHistoryPage extends Page {

    private Integer iceBoxId;

}
