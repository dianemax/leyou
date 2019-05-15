package com.leyou.order.config;

import com.github.wxpay.sdk.WXPayConfig;
import lombok.Data;

import java.io.InputStream;

@Data
public class PayConfig implements WXPayConfig {
    private String appID;//= "wx8397f8696b538317"; // 公众账号ID

    private String mchID;// = "1473426802"; // 商户号

    private String key;// = "T6m9iK73b0kn9g5v426MKfHQH7X8rKwb"; // 生成签名的密钥

    private int httpConnectTimeoutMs;// = 1000; // 连接超时时间

    private int httpReadTimeoutMs;// = 5000;// 读取超时时间

    private String notifyUrl;

    @Override
    public InputStream getCertStream() {
        return null;
    }
}