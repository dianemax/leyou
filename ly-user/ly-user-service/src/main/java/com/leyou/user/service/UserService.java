package com.leyou.user.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:verify:phone:";

    //校验登录数据
    public Boolean checkData(String data, Integer type) {
        User record = new User();
        switch (type){
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }
        return userMapper.selectCount(record) == 0;//为0说明该手机号或者用户名可用，因为用户名或者手机号必须唯一
    }

    // 发送短信验证码
    public void sendCode(String phone) {
        // 生成key
        String key = KEY_PREFIX + phone;
        // 生成验证码
        String code = NumberUtils.generateCode(6);

        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",msg);


        // 保存验证码到redis中 以供后续验证
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    }

    // 用户注册
    public void register(@Valid User user, String code) {

        // 校验验证码
        String cacheCode = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if(!StringUtils.equals(cacheCode, code)){
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }

        // 对密码进行加密
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        user.setPassword(CodecUtils.md5Hex(user.getPassword() , salt));

        // 写入数据库
        user.setCreated(new Date());
        userMapper.insert(user);
    }

    public User queryUsernameAndPassword(String username, String password) {

        User record = new User();
        record.setUsername(username);
        // 根据用户名查询用户
        User user = userMapper.selectOne(record);
        // 校验用户名
        if(user == null){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        // 校验密码
        if(! StringUtils.equals(user.getPassword(), CodecUtils.md5Hex(password , user.getSalt()))){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }

        return user;
    }
}
