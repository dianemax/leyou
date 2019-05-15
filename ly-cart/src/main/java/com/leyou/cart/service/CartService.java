package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.interceptor.UserInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:uid:";

    public void addCart(Cart cart) {
        // 获取登录的用户 -- 从线程中获得
        UserInfo user = UserInterceptor.getUser();

        // redis存储的结构是一个Map<String,Map<String,String>>,第一个key是用户的key，第二个key是商品的key，value是商品信息
        String key = KEY_PREFIX + user.getId();
        String hashKey = cart.getSkuId().toString();
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);

        if(operation.hasKey(hashKey)){
            // 如果存在  商品数量新增,新增之前先取出商品信息
            String json = operation.get(hashKey).toString();
            Cart cacheCart = JsonUtils.parse(json, Cart.class);
            cacheCart.setNum(cacheCart.getNum() + cart.getNum());
            operation.put(hashKey,JsonUtils.serialize(cacheCart));
        }else{
            // 如果不存在 新增
            operation.put(hashKey, JsonUtils.serialize(cart));
        }

    }

    public List<Cart> queryCartList() {
        // 获取登录的用户 -- 从线程中获得
        UserInfo user = UserInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();

        if(!redisTemplate.hasKey(key)){
            throw new LyException(ExceptionEnum.SKU_NOT_FOUND);
        }

        // 获取登录用户的所有购物车
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);

        List<Cart> carts = operation.values().stream()
                .map(o -> JsonUtils.parse(o.toString(), Cart.class))
                .collect(Collectors.toList());

        return carts;
    }

    public void updateCartNum(Long skuId, Integer num) {
        // 获取登录的用户 -- 从线程中获得
        UserInfo user = UserInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();

        // 获取登录用户的所有购物车
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);

        // 查询
        String json = operation.get(skuId.toString()).toString();
        Cart cart = JsonUtils.parse(json, Cart.class);
        cart.setNum(num);

        // 写回redis
        operation.put(skuId.toString(), JsonUtils.serialize(cart));

    }

    public void deleteCart(Long skuId) {
        // 获取登录的用户 -- 从线程中获得
        UserInfo user = UserInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();

        // 删除
        redisTemplate.opsForHash().delete(key, skuId.toString());
    }
}
