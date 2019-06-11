# 1 Redis是什么
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019060409581829.png)

Redis是一种NoSql数据库，NoSql的全称是Not Only Sql。NoSql指的是非关系型数据库，而我们常用的都是关系型数据库。就像我们常用的mysql，sqlserver一样，这些数据库一般用来存储重要信息，应对普通的业务是没有问题的。但是，随着互联网的高速发展，传统的关系型数据库在应付**超大规模，超大流量以及高并发**的时候力不从心。而就在这个时候，NoSql就派上了用场。

## 1.1 Nosql和关系型数据库的区别
**1.存储方式**

　　关系型数据库是**表格式**的，因此存储在表的行和列中。他们之间很容易关联协作存储，提取数据很方便。而Nosql数据库则与其相反，他是大块的组合在一起。通常存储在数据集中，就像文档、键值对或者图结构。


**2.存储结构**

　　关系型数据库对应的是结构化数据，**数据表都预先定义了结构**（列的定义），结构描述了数据的形式和内容。这一点对数据建模至关重要，虽然预定义结构带来了可靠性和稳定性，但是**修改这些数据比较困难**。而**Nosql数据库基于动态结构**，使用与非结构化数据。因为Nosql数据库是动态结构，可以很容易适应数据类型和结构的变化。


**3.存储规范**

　　关系型数据库的数据存储为了更高的规范性，把数据分割为最小的关系表以避免重复，获得精简的空间利用。虽然管理起来很清晰，但是单个操作设计到多张表的时候，数据管理就显得有点麻烦。而Nosql数据存储在平面数据集中，数据经常可能会重复。单个数据库很少被分隔开，而是存储成了一个整体，这样整块数据更加便于读写


**4.存储扩展**

　　这可能是两者之间最大的区别，**关系型数据库是纵向扩展，也就是说想要提高处理能力，要使用速度更快的计算机**。因为数据存储在关系表中，操作的性能瓶颈可能涉及到多个表，需要通过提升计算机性能来克服。虽然有很大的扩展空间，但是最终会达到纵向扩展的上限。而**Nosql数据库是横向扩展的，它的存储天然就是分布式的，可以通过给资源池添加更多的普通数据库服务器来分担负载**。


5.查询方式

　　关系型数据库通过结构化查询语言来操作数据库（就是我们通常说的SQL）。SQL支持数据库CURD操作的功能非常强大，是业界的标准用法。而Nosql查询以块为单元操作数据，使用的是非结构化查询语言（UnQl），它是没有标准的。关系型数据库表中主键的概念对应Nosql中存储文档的ID。关系型数据库使用预定义优化方式（比如索引）来加快查询操作，而Nosql更简单更精确的数据访问模式。


**6.事务**

　　关系型数据库遵循ACID规则（原子性(Atomicity)、一致性(Consistency)、隔离性(Isolation)、持久性(Durability)），而Nosql数据库遵循BASE原则（基本可用（Basically Availble）、软/柔性事务（Soft-state ）、最终一致性（Eventual Consistency））。由于关系型数据库的数据强一致性，所以对事务的支持很好。关系型数据库支持对事务原子性细粒度控制，并且易于回滚事务。而Nosql数据库是在CAP（一致性、可用性、分区容忍度）中任选两项，因为基于节点的分布式系统中，很难全部满足，所以对事务的支持不是很好，虽然也可以使用事务，但是并不是Nosql的闪光点。


**7.性能**

　　**关系型数据库为了维护数据的一致性付出了巨大的代价，读写性能比较差。在面对高并发读写性能非常差，面对海量数据的时候效率非常低**。而**Nosql存储的格式都是key-value类型的，并且存储在内存中，非常容易存储**，而且对于数据的 一致性是 弱要求。Nosql无需sql的解析，提高了读写性能。


**8.授权方式**

　　关系型数据库通常有SQL Server，Mysql，Oracle。主流的Nosql数据库有redis，memcache，MongoDb。大多数的关系型数据库都是付费的并且价格昂贵，成本较大，而Nosql数据库通常都是开源的。


## 1.2 缓存

我们今天要用的Redis就是一个**缓存**的nosql数据库，那么常见的缓存有些什么呢？

- IO流：缓冲流，可以有效提高IO读取效率，减少与磁盘交互次数
- CPU的缓存：包括CPU的一级缓存。二级缓存，CPU运行速度非常快，瓶颈是内存和CPU之间的数据传输。CPU要运算还要从内存中读取数据，读到CPU里才能运算，数据传输非常慢，于是就加了缓存，把内存中的数据加载到缓存里，这样运算时不用去找内存，而是去快速缓存去找，提升效率很快。

在我们**实际项目使用中**，需要频繁查询数据库，请求到达tomcat，tomcat每次都要查数据库，数据库的并发相对较大，因为要经过磁盘io，并发能力差；如果我们把**请求在前边通过redis做一个拦截**，那么请求到达以后不是去查数据库，而是直接去查redis，而redis并发是非常强，可以有效的提高程序运行的速度。

## 1.3 Redis与Memcache
Redis与Memcache是目前非常流行的两种NoSql数据库，都可以用于服务端缓存
- 从实现来看：
	- Redis：单线程
	- Memcache:多线程（多线程运行会对CPU有一定损耗，因为CPU要在线程间进行切换，因此**多线程不一定性能优于单线程**）
- 从存储方式来看：
	- Redis：支持数据**持久化和主从备份**，数据更安全
	- Memcache：数据存于内存，没有持久化功能
- 从功能来看：
	- Redis：除了基本的k-v结构外，支持多种其他复杂结构，事务等高级功能（不仅用来缓存）。
	- Memcache：只支持基本的k-v结构（纯粹用来做缓存）
- 从可用性看：
	- Redis：支持集群、主从备份、数据分片、哨兵监控，高可用
	- Memcache：没有分片功能，需要从客户端支持

# 2 Redis命令行
首先使用redis-cli连接Redis，之后输入命令行：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604103432223.png)

<font color="green" size=4>Redis中存储任何数据都是K-V结构，key的类型永远都是String，而Value的类型是可以变的。</font>

如果Value是一个String类型的，那就是String类型，如果Value是一个List，那就是List类型，以此类推。Redis中存储数据结构都是类似java的map类型，Redis不同数据类型，只是‘map’的值的类型不同。

## 2.1 通用指令
通用指令就是几种数据结构都可以用。

**keys：**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604104509623.png)

==注：keys指令生产环境禁用==，因为redis是单线程的，keys用正则表达式去模糊匹配，假如数据库中右几千万条数据，如果这种情况下输入keys去匹配，这种情况下其他任务都会被阻塞，直到keys查询完成，这个命令特别消耗CPU，可能导致服务器宕机。

**exist：**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604105100353.png)

**select**：

redis默认有16个库，角标：0-15,select命令可以切换库。


**del**与**expire**：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604105429597.png)

**TTL**：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604105648305.png)

**persist**：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604105814652.png)

## 2.2 字符串指令
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604110243164.png)

## 2.3 hash结构指令
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604110420186.png)

- HKEYS：获取所有哈希表中的字段
- HVALS：获取哈希表中所有值
- HDEL：删除哈希表key中的一个或多个字段
- HSET key field value：将哈希表key中的字段field的值设为value
- HGET key field：用于返回哈希表中指定字段的值
- HGETALL key：获取在哈希表中指定key的所有字段和值

# 3 Redis持久化
由于Redis的数据都存放在**内存**中，如果没有配置持久化，redis重启后数据就全丢失了，于是需要开启redis的持久化功能，将数据保存到磁盘上，当**redis重启后，可以从磁盘中恢复数据**。redis提供两种方式进行持久化，一种是**RDB持久化**（原理是将Reids在内存中的数据库记录定时dump到磁盘上的RDB持久化），另外一种是**AOF（append only file）持久化**（原理是将Reids的操作日志以追加的方式写入文件）

**实现原理：**

RDB持久化是指在指定的时间间隔内将内存中的数据集快照写入磁盘，实际操作过程是fork一个子进程，先将数据集写入临时文件，写入成功后，再替换之前的文件，用二进制压缩存储。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604111114708.png)

AOF持久化以日志的形式记录服务器所处理的每一个写、删除操作，查询操作不会记录，以文本的方式记录，可以打开文件看到详细的操作记录

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604111207723.png)

参考资料：[redis持久化的几种方式](https://www.cnblogs.com/chenliangcl/p/7240350.html)

# 4 SpringDataRedis
之前Redis都是采用的Jedis客户端，不过我们既然选择了SpringBoot来开发，那么我们就选择使用Spring对Redis封装的套件。

## 4.1 Spring Data Redis

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604113014352.png)

Spring Data Redis，是Spring Data 家族的一部分。 对Jedis客户端进行了封装，与spring进行了整合。可以非常方便的来实现redis的配置和操作。 

## 4.2 RedisTemplate

Spring Data Redis 提供了一个工具类：RedisTemplate。里面封装了对于Redis的五种数据结构的各种操作，包括：

- redisTemplate.opsForValue() ：操作string
- redisTemplate.opsForHash() ：操作hash
- redisTemplate.opsForList()：操作list
- redisTemplate.opsForSet()：操作set
- redisTemplate.opsForZSet()：操作zset

其它一些**通用命令**，如expire，可以通过redisTemplate.xx()来**直接调用**

5种结构：

- String：等同于java中的，`Map<String,String>`
- list：等同于java中的`Map<String,List<String>>`
- set：等同于java中的`Map<String,Set<String>>`
- sort_set：可排序的set
- hash：等同于java中的：`Map<String,Map<String,String>>`

## 4.3 StringRedisTemplate
RedisTemplate在创建时，可以指定其泛型类型：
- K：代表key 的数据类型
- V: 代表value的数据类型

注意：这里的类型不是Redis中存储的数据类型，而是Java中的数据类型，RedisTemplate会自动将Java类型转为Redis支持的数据类型：字符串、字节、二进制等等。

![在这里插入图片描述](https://img-blog.csdnimg.cn/2019060411501826.png)

不过RedisTemplate默认会采用JDK自带的序列化（Serialize）来对对象进行转换。生成的数据十分庞大，因此一般我们都会**指定key和value为String类型**，这样就由我们自己把对象序列化为json字符串来存储即可。

因为大部分情况下，我们都会使用key和value都为String的RedisTemplate，因此Spring就默认提供了这样一个实现：
!
[在这里插入图片描述](https://img-blog.csdnimg.cn/20190604115524311.png)
## 4.4 引入依赖
项目中引入Redis启动器：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
## 4.5 配置文件
在配置文件中指定Redis地址：

```yaml
spring:
  redis:
    host: 192.168.124.128
```

然后就可以直接注入`StringRedisTemplate`对象了
