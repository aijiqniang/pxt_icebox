package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IceBoxTransferHistoryPage extends Page {
    private Integer supplierId;
}
