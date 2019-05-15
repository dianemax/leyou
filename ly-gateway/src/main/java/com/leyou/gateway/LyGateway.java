package com.leyou.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * ClassName:LyGateway
 * PackageName:com.leyou.gateway
 * Description:
 *
 * @Date:2019/3/14 11:07
 * @Author:dianemax@163.com
 */


@SpringCloudApplication
@EnableZuulProxy
public class LyGateway {
    public static void main(String[] args) {
        SpringApplication.run(LyGateway.class);
    }
}
