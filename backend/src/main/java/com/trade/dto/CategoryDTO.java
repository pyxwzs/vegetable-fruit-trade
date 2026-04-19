package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class CategoryDTO {

    private Long id;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 50, message = "分类编码长度不能超过50")
    private String code;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    private String name;

    private Long parentId;

    private CategoryDTO parent;

    private List<CategoryDTO> children;

    private Integer sortOrder;

    @Size(max = 200, message = "图标URL长度不能超过200")
    private String icon;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;
}
