package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.beans.Transient;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        //分页
        PageHelper.startPage(page,rows);
        //过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //搜索字段过滤
        if(StringUtils.isNotBlank(key))
            criteria.andLike("title","%"+key+"%");
        //上下架过滤
        if(saleable!=null)
            criteria.andEqualTo("saleable",saleable);
        //默认排序
        example.setOrderByClause("last_update_time DESC");
        //查询
        List<Spu> spus = spuMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(spus))
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        //解析分类和品牌名称
        loadCategoryAndBrandName(spus);

        //解析分页结果
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);
        return new PageResult<>(pageInfo.getTotal(),spus);
    }

    private void loadCategoryAndBrandName(List<Spu> spus) {
        for (Spu spu : spus) {
            //处理分类名称
            List<String> names = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names,"/"));
            //处理品牌名称
            spu.setBname(brandService.queryById(spu.getBrandId()).getName());
        }
    }

    @Transactional
    public void saveGoods(Spu spu) {
        //新增spu
        spu.setId(null);
        spu.setSaleable(true);
        spu.setValid(false);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        int count = spuMapper.insert(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        //新增detail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        spuDetailMapper.insert(spuDetail);
        //新增sku和库存
        saveSkuAndStock(spu);

        //发送mq消息
        amqpTemplate.convertAndSend("item.insert",spu.getId());

    }

    private void saveSkuAndStock(Spu spu) {
        int count;//新增sku
        List<Sku> skus = spu.getSkus();
        List<Stock> stockList = new ArrayList<>();
        for (Sku sku : skus) {
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            sku.setSpuId(spu.getId());

            count = skuMapper.insert(sku);
            if(count!=1)
                throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);

            //新增库存
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockList.add(stock);
        }
        //批量新增库存
        count = stockMapper.insertList(stockList);
        if(count!=stockList.size())
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
    }

    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if(spuDetail==null)
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        return spuDetail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        //查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if(CollectionUtils.isEmpty(skuList))
            throw new LyException(ExceptionEnum.SKU_NOT_FOUND);

        //查询库存
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(stockList))
            throw new LyException(ExceptionEnum.STOCK_NOT_FOUND);

        //把stock变成一个map，其key：skuId,值：库存值
        Map<Long, Integer> stockMap = stockList.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s ->s.setStock(stockMap.get(s.getId())));
        return skuList;
    }


    @Transactional
    public void updateGoods(Spu spu) {
        if(spu.getId() == null)
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        //查询sku
        List<Sku> skuList = skuMapper.select(sku);
        if(!CollectionUtils.isEmpty(skuList)){
            //删除sku
            skuMapper.delete(sku);
            //删除库存
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        //修改spu
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setCreateTime(null);
        spu.setLastUpdateTime(new Date());

        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if(count!=1)
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        //修改detail
        spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if(count!=1)
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        //新增sku和库存
        saveSkuAndStock(spu);

        //发送mq消息
        amqpTemplate.convertAndSend("item.update",spu.getId());
    }

    public Spu querySpuById(Long id) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if(spu == null){
            throw new LyException(ExceptionEnum.SPU_NOT_FOUND);
        }
        // 查询sku
        List<Sku> skus = querySkuBySpuId(id);
        spu.setSkus(skus);

        // 查询detail
        SpuDetail spuDetail = queryDetailById(id);
        spu.setSpuDetail(spuDetail);

        return  spu;
    }

    public List<Sku> querySkuBySpuIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnum.SKU_NOT_FOUND);
        }

        //查询库存
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(stockList))
            throw new LyException(ExceptionEnum.STOCK_NOT_FOUND);

        //把stock变成一个map，其key：skuId,值：库存值
        Map<Long, Integer> stockMap = stockList.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skus.forEach(s ->s.setStock(stockMap.get(s.getId())));

        return skus;
    }

    @Transactional
    public void decreaseStock(List<CartDTO> cartDTOS) {
        // 不能用if判断来实现减库存，当线程很多的时候，有可能引发超卖问题
        // 加锁也不可以  性能太差，只有一个线程可以执行，当搭了集群时synchronized只锁住了当前一个tomcat
        for (CartDTO cartDTO : cartDTOS) {
            int count = stockMapper.decreaseStock(cartDTO.getSkuId(), cartDTO.getNum());
            if(count != 1){
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
