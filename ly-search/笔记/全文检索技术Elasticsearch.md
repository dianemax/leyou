@[TOC](全文检索技术：)
# 1 Elasticsearch是什么
**Elasticsearch**是一个分布式的[Restfull风格](https://blog.csdn.net/sinat_38570489/article/details/90602762)的搜索和数据分析引擎，他有以下特点：
- **分布式**：无需人工搭建集群，会自动扩展
- **Restful风格**，一切API都遵循Rest原则，容易上手
- 近**实时搜索**，数据更新在Elasticsearch中几乎是完全同步的。

为什么说Elasticsearch是**近乎**实时的呢？

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Elasticsearch和磁盘之间还有一层称为**FileSystem Cache的系统缓存**，正是由于这层cache的存在才使得es能够拥有更快搜索响应能力。
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;在Elasticsearch中新增的document（相当于数据库的行）会被收集到indexing buffer（索引，相当于数据库）区后被重写成一个segment然后直接写入filesystem cache中，这个操作是非常轻量级的，相对耗时较少，之后经过一定的间隔或外部触发后才会被flush到磁盘上，这个操作非常耗时。但**只要sengment文件被写入cache后，这个sengment就可以打开和查询**，从而确保在短时间内就可以搜到，而不用执行一个full commit也就是fsync操作，这是一个非常轻量级的处理方式而且是可以高频次的被执行，而不会破坏Elasticsearch的性能。
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;默认情况下，**每隔1秒自动refresh一次**，这就是我们为什么说es是近实时的搜索引擎而不是实时的，也就是说给索引插入一条数据后，我们需要等待1秒才能被搜到这条数据，这是es对**写入和查询一个平衡的设置方式**，这样设置既提升了es的索引写入效率同时也使得es能够近实时检索数据。
参考资料：[为什么说Elasticsearch搜索是近实时的？](https://blog.csdn.net/u010454030/article/details/79586072)

# 2 索引

==Elasticsearch和MySQL的对比关系==：

> 索引（indices）--------------------------------Databases 数据库
类型（type）------------------------------------Table 数据表
文档（Document）----------------------------Row 行
字段（Field）-----------------------------------Columns 列

- 数据库里面的每一**行**商品数据就可以称为一个**文档**
- 商品名称这一**列**，就可以称为一个**字段**

| 概念                 | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 索引库（indices)     | indices是index的复数，代表许多的索引，                       |
| 类型（type）         | 类型是模拟mysql中的table概念，一个索引库下可以有不同类型的索引，比如商品索引，订单索引，其数据格式不同。不过这会导致索引库混乱，因此未来版本中会移除这个概念 |
| 文档（document）     | 存入索引库原始的数据。比如每一条商品信息，就是一个文档       |
| 字段（field）        | 文档中的属性                                                 |
| 映射配置（mappings） | 字段的数据类型、属性、是否索引、是否存储等特性               |

## 2.1 对索引的操作
**增：**
Elasticsearch采用Rest风格API，因此其API就是一次http请求

创建索引的请求格式：

- 请求方式：PUT

- 请求路径：/索引库名

- 请求参数：json格式：
```json
{
    "settings": {
        "number_of_shards": 3,
        "number_of_replicas": 2
    }
}
```
**查：**
```
GET /索引库名
```
**删：**
```
DELETE /索引库名
```
## 2.2 映射的配置
索引有了，接下来肯定是添加数据。不过数据存储到索引库中，必须指定一些相关属性，比如：

- 字段的数据类型
- 是否要存储
- 是否要索引
- 是否分词
- 分词器是什么

创建映射：PUT
```
PUT /索引库名/_mapping/类型名称
{
  "properties": {
    "字段名": {
      "type": "类型",
      "index": true，
      "store": true，
      "analyzer": "分词器"
    }
  }
}
```
**查看映射：**
```
GET /索引库名/_mapping
```

## 2.3 对数据的操作
**增：**
通过POST请求，可以向一个已经存在的索引库中添加数据。
随机产生数据id：
```
POST /索引库名/类型名
{
    "key":"value"
}
```
自定义id：
```
POST /索引库名/类型/id值
{
    ...
}
```

**改：**
请求方式为put，不过修改必须指定id。
- id对应文档存在，则修改
- id对应文档不存在，则新增

**删：**
删除使用DELETE请求，同样，需要根据id进行删除：


```
DELETE /索引库名/类型名/id值
```

# 3 查询

**词条匹配：**

`term` 查询被用于精确值 匹配，这些精确值可能是数字、时间、布尔或者那些**未分词**的字符串。`terms` 查询和 term 查询一样，但它允许你指定多值进行匹配。

**结果过滤：**

默认情况下，elasticsearch在搜索的结果中，会把文档中保存在`_source`的所有字段都返回。如果我们只想获取其中的部分字段，我们可以添加`_source`的过滤

**includes和excludes**

- includes：来指定想要显示的字段
- excludes：来指定不想要显示的字段

二者都是可选的。

**高级查询——布尔查询**

`bool`把各种其它查询通过`must`（与）、`must_not`（非）、`should`（或）的方式进行组合

**范围查询(range)：**

`range` 查询找出那些落在指定区间内的数字或者时间

**模糊查询(fuzzy)：**

`fuzzy` 查询是 `term` 查询的模糊等价。它允许用户搜索词条与实际词条的拼写出现偏差，但是偏差的编辑距离不得超过2，我们可以通过`fuzziness`来指定允许的编辑距离

**排序：**

`sort` 可以让我们按照不同的字段进行排序，并且通过`order`指定排序的方式


# 4 聚合aggregations
聚合可以让我们极其方便的实现对数据的统计、分析。例如：

- 什么品牌的手机最受欢迎？
- 这些手机的平均价格、最高价格、最低价格？
- 这些手机每月的销售情况如何？

实现这些统计功能的比数据库的sql要方便的多，而且查询速度非常快，可以实现近实时搜索效果。

## 4.1 基本概念

Elasticsearch中的聚合，包含多种类型，最常用的两种，一个叫`桶`，一个叫`度量`：

> **桶（bucket）**

桶的作用，是按照某种方式对数据进行分组，每一组数据在ES中称为一个`桶`，例如我们根据国籍对人划分，可以得到 中国桶、 英国桶，日本桶……或者我们按照年龄段对人进行划分：0-10,10-20,20-30,30-40等。

综上所述，我们发现bucket aggregations 只负责对数据进行分组，并不进行计算，因此往往bucket中往往会嵌套另一种聚合：metrics aggregations即度量



> **度量（metrics）**

分组完成以后，我们一般会对组中的数据进行聚合运算，例如求平均值、最大、最小、求和等，这些在ES中称为`度量`

比较常用的一些度量聚合方式：

- Avg Aggregation：求平均值
- Max Aggregation：求最大值
- Min Aggregation：求最小值
- Percentiles Aggregation：求百分比
- Stats Aggregation：同时返回avg、max、min、sum、count等
- Sum Aggregation：求和
- Top hits Aggregation：求前几
- Value Count Aggregation：求总数
- ……

**注意**：在ES中，需要进行聚合、排序、过滤的字段其处理方式比较特殊，因此**不能被分词**。这里我们将这些字段设置为**keyword**类型，这个类型不会被分词，将来就可以参与聚合（text类型可以被分词）

导入数据

```json
POST /cars/transactions/_bulk
{ "index": {}}
{ "price" : 10000, "color" : "red", "make" : "honda", "sold" : "2014-10-28" }
{ "index": {}}
{ "price" : 20000, "color" : "red", "make" : "honda", "sold" : "2014-11-05" }
{ "index": {}}
{ "price" : 30000, "color" : "green", "make" : "ford", "sold" : "2014-05-18" }
{ "index": {}}
{ "price" : 15000, "color" : "blue", "make" : "toyota", "sold" : "2014-07-02" }
{ "index": {}}
{ "price" : 12000, "color" : "green", "make" : "toyota", "sold" : "2014-08-19" }
{ "index": {}}
{ "price" : 20000, "color" : "red", "make" : "honda", "sold" : "2014-11-05" }
{ "index": {}}
{ "price" : 80000, "color" : "red", "make" : "bmw", "sold" : "2014-01-01" }
{ "index": {}}
{ "price" : 25000, "color" : "blue", "make" : "ford", "sold" : "2014-02-12" }
```





## 4.2 聚合为桶

首先，我们按照 汽车的颜色`color来`划分`桶`

```json
GET /cars/_search
{
    "size" : 0,
    "aggs" : { 
        "popular_colors" : { 
            "terms" : { 
              "field" : "color"
            }
        }
    }
}
```

- size： 查询条数，这里设置为0，因为我们不关心搜索到的数据，只关心聚合结果，提高效率
- aggs：声明这是一个聚合查询，是aggregations的缩写
  - popular_colors：给这次聚合起一个名字，任意。
    - terms：划分桶的方式，这里是根据词条划分
      - field：划分桶的字段

结果：

```json
{
  "took": 1,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": 8,
    "max_score": 0,
    "hits": []
  },
  "aggregations": {
    "popular_colors": {
      "doc_count_error_upper_bound": 0,
      "sum_other_doc_count": 0,
      "buckets": [
        {
          "key": "red",
          "doc_count": 4
        },
        {
          "key": "blue",
          "doc_count": 2
        },
        {
          "key": "green",
          "doc_count": 2
        }
      ]
    }
  }
}
```

- hits：查询结果为空，因为我们设置了size为0
- aggregations：聚合的结果
- popular_colors：我们定义的聚合名称
- buckets：查找到的桶，每个不同的color字段值都会形成一个桶
  - key：这个桶对应的color字段的值
  - doc_count：这个桶中的文档数量

通过聚合的结果我们发现，目前红色的小车比较畅销！



