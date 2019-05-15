package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;


    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsRepository repository;

    public Goods buildGoods(Spu spu){

        Goods goods = new Goods();

        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        goods.setId(spu.getId());

        //////////////////////////////////////////////////////////////////////////////////////////
        // all --- 搜索字段：标题、分类、品牌、规格
        // 标题 spu.getTitle()
        // 查询分类
        List<String> names = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                .stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(names)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        // 查询品牌
        ////////////////////////////////////////////////////////////
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if(brand == null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        // all
        String all = spu.getTitle() + StringUtils.join(names," ") + brand.getName();


        // sku --- 所有sku的集合的json格式
        List<Sku> skuList = goodsClient.querySkuBySpuId(spu.getId());
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.SKU_NOT_FOUND);
        }
        // 搜索字段只需要部分数据(id,title,price,image) 所以要对sku进行处理
        ArrayList<Map<String,Object>> skus = new ArrayList<>();
        // price
        Set<Long> priceList = new HashSet<>();
        for (Sku sku : skuList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id",sku.getId());
            map.put("title",sku.getTitle());
            map.put("price",sku.getPrice());
            map.put("image",StringUtils.substringBefore(sku.getImages(),","));//sku中有多个图片，只展示第一张

            skus.add(map);

            //处理价格
            priceList.add(sku.getPrice());

        }

        // 查询规格参数  结果是一个map
        // 规格参数表
        List<SpecParam> params = specificationClient.querySpecParams(null, spu.getCid3(), true);
        if(CollectionUtils.isEmpty(params)){
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        // 规格详情表
        SpuDetail spuDetail = goodsClient.querySpuDetailById(spu.getId());
        // 获取通用规格参数
        Map<Long, String> genericSpec = JsonUtils.parseMap(spuDetail.getGenericSpec(), Long.class, String.class);
        //获取特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(
                spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {});

        //将参数填入map
        Map<String,Object> specs = new HashMap<>();
        for (SpecParam param : params) {
            // 规格名字 key
            String key = param.getName();
            Object value = "";

            //规格参数 value
            if(param.getGeneric()){
                // 通用属性
                value = genericSpec.get(param.getId());// 通用参数的数值类型有分段的情况存在，要做一个处理,不能按上面那种方法获得value
                //判断是否为数值类型 处理成段,覆盖之前的value
                if(param.getNumeric()){
                    value = chooseSegment(value.toString(),param);
                }
            }else {
                // 特殊属性
                value = specialSpec.get(param.getId());
            }
            value = (value == null ? "其他" : value);

            specs.put(key,value);
        }


        goods.setAll(all); // 搜索字段，包含标题、分类、品牌、规格
        goods.setSkus(JsonUtils.serialize(skus)); // 所有sku的集合的json格式
        goods.setPrice(priceList); // 所有sku的价格集合
        goods.setSpecs(specs); // 所有可搜索的规格参数

        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * 搜索功能
     * @param request
     * @return
     * */

    public SearchResult search(SearchRequest request) {

        String key = request.getKey(); // 搜索条件 eg:手机
        if (StringUtils.isBlank(key)) {
            throw new LyException(ExceptionEnum.INVALID_PARAM);
        }

        int page = request.getPage() - 1;// page，elasticSearch默认从0开始，要进行减一操作否则一直查询不到第一页
        int size = request.getSize();

        // 1 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 2 分页
        queryBuilder.withPageable(PageRequest.of(page,size));

        // 3 过滤
        // 3.1 结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 3.2 过滤

        QueryBuilder baseQuery = buildBaseQuery(request);
        queryBuilder.withQuery(baseQuery);

        // 4 聚合
        // 4.1 聚合分类
        String CategoryAggName = "categoryAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(CategoryAggName).field("cid3"));

        // 4.2 聚合品牌
        String BrandAggName = "brandAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(BrandAggName).field("brandId"));

        // 5 查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 6 解析结果
        // 6.1 解析分页结果
        long total = result.getTotalElements();
        int totalPage = result.getTotalPages(); //int totalPage = ((int) total + size -1)/size;

        // 6.2 解析聚合结果
        Aggregations aggs = result.getAggregations();
        List<Category> categories = parseCategoryAgg(aggs.get(CategoryAggName));
        List<Brand> brands = parseBrandAgg(aggs.get(BrandAggName));

        // 规格参数的聚合
        List<Map<String, Object>> specs = null;
        // 商品分类存在切值为1，才可以进行规格参数的聚合
        if(categories != null && categories.size() == 1){
            specs = buildSpecificationAgg(categories.get(0).getId(),baseQuery);
        }

        List<Goods> goodsList = result.getContent();
        return new SearchResult(total, totalPage, goodsList,categories,brands,specs);
    }

    private QueryBuilder buildBaseQuery(SearchRequest request) {

        // 创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        // 查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()));
        // 过滤条件 (有n个过滤条件因此要遍历map)
        Map<String, String> map = request.getFilter();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            System.out.println("entry key = " + key);
            // 处理key
            if(!"cid3".equals(key) && !"brandId".equals(key)){
                key = "specs." + key + ".keyword";
            }
            String value = entry.getValue();
            queryBuilder.filter(QueryBuilders.termQuery(key,value));
        }

        return queryBuilder;

    }

    private List<Map<String,Object>> buildSpecificationAgg(Long cid, QueryBuilder baseQuery) {

        List<Map<String,Object>> specs = new ArrayList<>();

        // 查询需要聚合的规格参数
        List<SpecParam> params = specificationClient.querySpecParams(null, cid, true);
        // 聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 1.1 带上基础查询条件
        queryBuilder.withQuery(baseQuery);
        // 1.2 遍历params 聚合名字 字段
        for (SpecParam param : params) {
            String name = param.getName();//规格参数的名字的不会重复 作为聚合的name
            queryBuilder.addAggregation(
                    AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        }

        // 获取结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 解析结果
        Aggregations aggs = result.getAggregations();

        // 有几个param就要做几个聚合
        for (SpecParam param : params) {
            String name = param.getName();
            StringTerms terms = aggs.get(name);
            List<Object> options = terms.getBuckets().stream()
                    .map(b -> b.getKey()).collect(Collectors.toList());
            // 准备map
            Map<String, Object> map = new HashMap<>();
            map.put("k",name);
            map.put("options",options);

            specs.add(map);
        }

        return specs;
    }

    private List<Category> parseCategoryAgg(LongTerms terms) {

        try {
            List<Long> ids = terms.getBuckets().stream()
                    .map(bucket -> bucket.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Category> categories = categoryClient.queryCategoryByIds(ids);

            return categories;
        }catch (Exception e){
            return null;
        }
    }

    private List<Brand> parseBrandAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets().stream()
                    .map(bucket -> bucket.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());

            List<Brand> brands = brandClient.queryBrandsByIds(ids);
            return brands;
        }catch (Exception e){
            return null;
        }
    }


    public void createOrUpdateIndex(Long spuId) {
        // 查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        // 构建goods
        Goods goods = buildGoods(spu);
        // 存入索引库
        repository.save(goods);

    }

    public void deleteIndex(Long spuId) {
        repository.deleteById(spuId);
    }
}
