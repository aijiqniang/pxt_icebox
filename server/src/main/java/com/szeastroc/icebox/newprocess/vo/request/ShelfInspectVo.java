package com.szeastroc.icebox.newprocess.vo.request;

import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel
public class ShelfInspectVo {
    private String customerNumber;
    private String applyNumber;
    private String customerName;
    private String remark;
    private String imageUrl;
    private Integer createdBy;
    private String createName;
    private Date createdTime;
    @ApiModelProperty("巡检状态")
    private Integer inspectStatus;
    @ApiModelProperty("正常货架")
    private List<ShelfInspectModel.NormalShelf> normalShelves;

    @ApiModelProperty("报废货架")
    private List<ShelfInspectModel.ScrapShelf> scrapShelves;

    @ApiModelProperty("遗失货架")
    private List<ShelfInspectModel.LostShelf> lostShelves;



    @Data
    @ApiModel
    @Accessors(chain = true)
    public static class NormalShelf {

        @ApiModelProperty("货架名称")
        private String name;

        @ApiModelProperty("巡检数量")
        private Integer count;

        @ApiModelProperty("货架类型")
        private Integer type;

        @ApiModelProperty("货架大小")
        private String size;
    }

    @Data
    @ApiModel
    @Accessors(chain = true)
    public static class ScrapShelf {

        @ApiModelProperty("货架名称")
        private String name;

        @ApiModelProperty("巡检数量")
        private Integer count;

        @ApiModelProperty("货架类型")
        private Integer type;

        @ApiModelProperty("货架大小")
        private String size;
    }

    @Data
    @ApiModel
    @Accessors(chain = true)
    public static class LostShelf {

        @ApiModelProperty("货架名称")
        private String name;

        @ApiModelProperty("巡检数量")
        private Integer count;

        @ApiModelProperty("货架类型")
        private Integer type;

        @ApiModelProperty("货架大小")
        private String size;
    }
}
