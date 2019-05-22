package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * ClassName:CatagoryService
 * PackageName:com.leyou.item.service
 * Description:
 *
 * @Date:2019/3/16 21:50
 * @Author:yuqin_su@163.com
 */

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;


    /**
     * 查询商品分类的方法
     * @param pid
     * @return
     */
    public List<Category> queryCategoryListByPid(Long pid) {

        Category t = new Category();
        t.setParentId(pid);
        //查询的对象需要自己new出来，并将这个对象中的非空字段作为查询条件
        List<Category> list = categoryMapper.select(t);

        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }

        return list;
    }

    /**
     * 根据商品分类cid列表查询分类集合
     * @param cids
     * @return
     */
    public List<Category> queryByIds(List<Long> cids){

        List<Category> idList = categoryMapper.selectByIdList(cids);

        if(CollectionUtils.isEmpty(idList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }

        return idList;

    }
}
