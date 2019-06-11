
# 引言
## RabbitMQ
基本介绍及使用：[消息服务器RabbitMQ及其五种消息模型](https://blog.csdn.net/sinat_38570489/article/details/90726808)

## Spring AMQP
Spring中已经实现了对AMQP的支持， Spring-amqp是对AMQP协议的抽象实现，而spring-rabbit 是对协议的具体实现，也是目前的唯一实现。底层使用的就是RabbitMQ。

**依赖和配置：**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

在`application.yml`中添加RabbitMQ地址：

```yaml
spring:
  rabbitmq:
    host: 192.168.124.128
    username: leyou
    password: leyou
    virtual-host: /leyou
```

**监听者**

在SpringAmqp中，对消息的消费者进行了封装和抽象，一个普通的JavaBean中的普通方法，只要**通过简单的注解，就可以成为一个消费者**。

```java
@Component
public class Listener {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "spring.test.queue", durable = "true"),
            exchange = @Exchange(
                    value = "spring.test.exchange",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC
            ),
            key = {"#.#"}))
    public void listen(String msg){
        System.out.println("接收到消息：" + msg);
    }
}
```

- `@Componet`：类上的注解，注册到Spring容器
- `@RabbitListener`：方法上的注解，声明这个方法是一个消费者方法，需要指定下面的属性：
  - `bindings`：指定绑定关系，可以有多个。值是`@QueueBinding`的数组。`@QueueBinding`包含下面属性：
    - `value`：这个消费者关联的队列。值是`@Queue`，代表一个队列
    - `exchange`：队列所绑定的交换机，值是`@Exchange`类型
    - `key`：队列和交换机绑定的`RoutingKey`

类似listen这样的方法在一个类中可以写多个，就代表多个消费者。

**AmqpTemplate**：

Spring最擅长的事情就是封装，把他人的框架进行封装和整合。Spring为AMQP提供了统一的消息处理模板：AmqpTemplate，非常方便的发送消息。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190603205250724.png)

# 1 项目改造思路分析
> 发送方：商品微服务

- 什么时候发？

  当商品服务对商品进行写操作：增、删、改的时候，需要发送一条消息，通知其它服务。

- 发送什么内容？

  对商品的增删改时其它服务可能需要新的商品数据，但是如果消息内容中包含全部商品信息，数据量太大，而且并不是每个服务都需要全部的信息。因此我们**只发送商品id**，其它服务可以根据id查询自己需要的信息。

> 接收方：搜索微服务、静态页微服务

- 接收消息后如何处理？
  - 搜索微服务：
    - 增/改：添加新的数据到索引库
    - 删：删除索引库数据
  - 静态页微服务：
    - 增：创建新的静态页
    - 删：删除原来的静态页
    - 改：创建新的静态页并删除原来的

# 2 优化商品微服务——生产者

我们先在商品微服务`ly-item-service`中实现发送消息。

## 2.1 引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## 2.2 配置文件

我们在application.yml中添加一些有关RabbitMQ的配置：

```yaml
spring:
  rabbitmq:
    host: 192.168.56.101
    username: leyou
    password: leyou
    virtual-host: /leyou
    template:
      retry:
        enabled: true
        initial-interval: 10000ms
        max-interval: 300000ms
        multiplier: 2
      exchange: ly.item.exchange
    publisher-confirms: true
```
publisher-confirms：**生产者确认机制**，确保消息会正确发送，如果发送失败会有错误回执，从而触发重试。

## 2.3 改造GoodsService
新增时：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190603213304453.png)

修改时：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190603213525406.png)

# 3 优化搜索微服务——消费者

搜索服务接收到消息后要做的事情：

- 增：添加新的数据到索引库
- 删：删除索引库数据
- 改：修改索引库数据

因为索引库的新增和修改方法是合二为一的，因此我们可以将这两类消息一同处理，删除另外处理。

## 3.1 引入依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```
## 3.2 添加配置

```yaml
spring:
  rabbitmq:
    host: 192.168.124.128
    username: leyou
    password: leyou
    virtual-host: /leyou
```

这里只是接收消息而不发送，所以不用配置template相关内容。

## 3.3 编写监听器——处理消息

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190603213809463.png)

```java
@Component
public class ItemListener {

    @Autowired
    private SearchService searchService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.item.insert.queue", durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange", type = ExchangeTypes.TOPIC),
            key = {"item.insert", "item.update"}
    ))
    public void listenInsertOrUpdate(Long spuId){
        if(spuId == null){
            return;
        }
        // 处理消息,对索引库进行新增或者修改
        searchService.createOrUpdateIndex(spuId);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.item.delete.queue", durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange", type = ExchangeTypes.TOPIC),
            key = {"item.delete"}
    ))
    public void listenDelete(Long spuId){
        if(spuId == null){
            return;
        }
        // 处理消息,对索引库进行新增或者修改
        searchService.deleteIndex(spuId);
    }
}
```

## 3.4 编写创建和删除索引方法
```java
public void createOrUpdateIndex(Long spuId) {
    // 查询spu
    Spu spu = goodsClient.querySpuById(spuId);
    // 构建goods
    Goods goods = buildGoods(spu);
    // 存入索引库
    repository.save(goods);
}
```
```java
public void deleteIndex(Long spuId) {
	// 删除索引库
    repository.deleteById(spuId);
}
```

# 4 优化页面微服务——消费者
## 4.1 引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## 4.2 添加配置

```yaml
spring:
  rabbitmq:
    host: 192.168.56.101
    username: leyou
    password: leyou
    virtual-host: /leyou
```

这里只是接收消息而不发送，所以不用配置template相关内容。

## 4.3 编写监听器
```java
@Component
public class ItemListener {

    @Autowired
    private PageService pageService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "page.item.insert.queue", durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange", type = ExchangeTypes.TOPIC),
            key = {"item.insert", "item.update"}
    ))
    public void listenInsertOrUpdate(Long spuId){
        if(spuId == null){
            return;
        }
        // 处理消息,生成静态页
        pageService.createHtml(spuId);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "page.item.delete.queue", durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange", type = ExchangeTypes.TOPIC),
            key = {"item.delete"}
    ))
    public void listenDelete(Long spuId){
        if(spuId == null){
            return;
        }
        // 处理消息,对静态页进行删除
        pageService.deleteHtml(spuId);
    }
}
```

## 4.4 添加删除页面方法
```java
public void deleteHtml(Long spuId) {
    // 输出流(流可以自动释放)
    File dest = new File("H:\\javacode\\idea\\upload", spuId + ".html");
    if(dest.exists()){
        dest.delete();
    }
}
```
