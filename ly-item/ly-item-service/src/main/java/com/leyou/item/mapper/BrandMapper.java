package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.*;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends BaseMapper<Brand> {

    @Insert("INSERT INTO tb_category_brand (category_id, brand_id) VALUES (#{cid},#{bid})")
    //@Param是mybatis中的注解，用注解来简化xml配置的时候,
        // @Param注解的作用是给参数命名,参数命名后就能根据名字得到参数值,正确的将参数传入sql语句中
    int insertCategoryBrand(@Param("cid") Long cid, @Param("bid")Long bid);

    @Select("SELECT b.* FROM tb_brand b LEFT JOIN tb_category_brand cb ON b.id = cb.brand_id WHERE cb.category_id = #{cid}")
    List<Brand> queryByCategoryId(@Param("cid")Long cid);


    ///////////////////////////////////////////////////////////////////////////////
    @Select("SELECT brand_id from tb_category_brand where category_id = #{cid}")
    List<Brand> queryBrandByCid(@Param("cid")Long cid);

    @Update("UPDATE tb_category_brand SET category_id = #{cid} where brand_id = #{bid}" )
    void updateCategoryBrand(@Param("cid") Long categoryId, @Param("bid") Long id);

    @Delete("DELETE from tb_category_brand where brand_id = #{bid}")
    void deleteCategoryBrandByBid(@Param("bid") Long bid);


}
