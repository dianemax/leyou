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
 * @Author:yuqin_su@163.com
 */
@Table(name = "tb_category")//当实体类与其映射的数据库表名不同名时需要使用 @Table 标注说明
                            //name属性用于指定数据库表名称
                            //若不指定则以实体类名称作为表名
@Data
public class Category {
    @Id //声明一个实体类的属性映射为数据库的主键列
    @KeySql(useGeneratedKeys=true) //使用 JDBC 方式获取主键，优先级最高，设置为 true 后，不对其他配置校验
    private Long id;
    private String name;
    private Long parentId;
    private Boolean isParent;
    private Integer sort;

    @Transient  //修饰的字段不会被持久化
    private List<Category> categoryList;

}
