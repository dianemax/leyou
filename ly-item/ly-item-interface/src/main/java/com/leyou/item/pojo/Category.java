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
@Table(name = "tb_category")//当实体类与其映射的数据库表名不同名时需要使用 @Table 标注说明
                            //name属性用于指定数据库表名称
                            //若不指定则以实体类名称作为表名
@Data
public class Category {
    @Id
    @KeySql(useGeneratedKeys=true)
    private Long id;
    private String name;
    private Long parentId;
    private Boolean isParent;
    private Integer sort;

    @Transient  //修饰的字段不会被持久化
    private List<Category> categoryList;

}
