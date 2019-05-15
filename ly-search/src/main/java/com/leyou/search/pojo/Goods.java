package com.leyou.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@Document(indexName = "goods", type = "docs", shards = 1, replicas = 0)
public class Goods {
    @Id
    private Long id; // spuId
    
    @Field(type = FieldType.text, analyzer = "ik_max_word")
    private String all; // 所有需要被搜索的信息，包含标题，分类，甚至品牌
    
    @Field(type = FieldType.keyword, index = false)//不进行搜索，不进行分词
    private String subTitle;// 卖点
    
    private Long brandId;// 品牌id
    private Long cid1;// 1级分类id
    private Long cid2;// 2级分类id
    private Long cid3;// 3级分类id
    private Date createTime;// 创建时间
    private Set<Long> price;// 价格，对应到elasticsearch/json中是数组，一个spu有多个sku，就有多个价格
    
    @Field(type = FieldType.keyword, index = false)
    private String skus;// sku信息的json结构，只是一个展示结果
    private Map<String, Object> specs;// 可搜索的规格参数，key是参数名，值是参数值

}