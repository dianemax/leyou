package com.leyou.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

/**
 * ClassName:Category
 * PackageName:com.leyou.item.pojo
 * Description:
 *
 * @Date:2019/3/15 21:50
 * @Author:dianemax@163.com
 */
@Table(name = "tb_category")
@Data
public class Category {
    @Id
    @KeySql(useGeneratedKeys=true)
    private Long id;
    private String name;
    private Long parentId;
    private Boolean isParent;
    private Integer sort;
    //////////////
    @Transient
    private List<Category> categoryList;

}
