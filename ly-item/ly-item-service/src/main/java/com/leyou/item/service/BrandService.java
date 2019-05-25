package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {
    @Autowired
    private BrandMapper brandMapper;


    /**
     * 根据多个条件查询品牌信息
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @param key
     * @return
     */
    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, boolean desc, String key) {
        //分页
        //会利用`mybatis`的拦截器对接下来的`sql`语句进行拦截，自动在该sql后面**动态拼接sql语句**，如分页方法中需要拼接`limit`语句
        PageHelper.startPage(page,rows);

        //过滤: 过滤条件(模糊查询+准确查询(首字母))
        //将`Brand.class`字节码文件传给Example，通过 反射 得到实体类中的表的名字，主键等信息
        //动态拼接sql语句
        Example example = new Example(Brand.class);
        if(StringUtils.isNotBlank(key)){
            example.createCriteria().orLike("name","%"+key+"%").orEqualTo("letter",key.toUpperCase());
        }

        //排序
        //ORDER BY关键字是可以自动生成的，重点是后面的 根据什么排序 sql语句不知道，所以要写一个orderByClause——排序子句
        if(StringUtils.isNotBlank(sortBy)){
            String orderByClause = sortBy + (desc ? " DESC" : " ASC");
            example.setOrderByClause(orderByClause);
        }

        //查询
        List<Brand> list = brandMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        //解析分页结果
        PageInfo<Brand> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(),list);
    }

    /**
     * 品牌的修改
     *
     * @param categories
     * @param brand
     */
    @Transactional
    public void updateBrand(List<Long> categories, Brand brand) {
        //修改品牌
        brandMapper.updateByPrimaryKeySelective(brand);
        //维护中间表
        for (Long categoryId : categories) {
            brandMapper.updateCategoryBrand(categoryId, brand.getId());
        }

    }



    //在配置文件中做相关的事务规则声明(或通过基于@Transactional注解的方式)，便可以将事务规则应用到业务逻辑中
    //声明式事务管理也有两种常用的方式，一种是基于tx和aop名字空间的xml配置文件，另一种就是基于@Transactional注解
    //当作用于类上时，该类的所有 public 方法将都具有该类型的事务属性，同时，我们也可以在方法级别使用该标注来覆盖类级别的定义
    //如果被注解的数据库操作方法中发生了unchecked异常，所有的数据库操作将rollback
    /**
     * 品牌新增
     */
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        //新增品牌
        brand.setId(null);//初始设置成null，新增之后回显
        int count = brandMapper.insert(brand);
        if(count != 1){
            throw new LyException(ExceptionEnum.BRAND_CREATE_FAILED);
        }
        //中间表新增。但是中间表没有实体类，没有通用mapper
        for (Long cid : cids){
            count = brandMapper.insertCategoryBrand(cid,brand.getId());
            if(count != 1){
                throw new LyException(ExceptionEnum.BRAND_CREATE_FAILED);
            }
        }
    }

    /**
     * 品牌的删除后
     * @param bid
     */
    public void deleteBrand(Long bid) {
        //删除品牌表
        brandMapper.deleteByPrimaryKey(bid);
        //维护中间表
        brandMapper.deleteCategoryBrandByBid(bid);
    }

    /**
     * 根据品牌brandid查询品牌名称
     * @param id
     * @return
     */
    public Brand queryById(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);

        if(brand == null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        return brand;
    }

    /**
     * 根据cid查到所有的品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {

        //cid: category id 和 tb_category,tb_category_brand表都有关系，要自己写sql语句

        List<Brand> brands = brandMapper.queryByCategoryId(cid);

        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        return brands;

    }

    /**
     * 根据bid的集合查询品牌信息
     */
    public List<Brand> queryBrandsByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
