package com.leyou.order.config;


import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {

    private String pubKeyPath;// 公钥
    private String cookieName;

    private PublicKey publicKey; // 公钥

    // 对象一旦实例化后，就应该读取公钥和私钥
    @PostConstruct // 构造函数执行完毕后就执行
    public void init(){

        // 获取公钥和私钥
        try {
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

