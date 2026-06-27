package org.example.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cos")
public class CosProperties {

    /**
     * 腾讯云 SecretId
     */
    private String secretId;

    /**
     * 腾讯云 SecretKey
     */
    private String secretKey;

    /**
     * COS 地域，如 ap-guangzhou
     */
    private String region;

    /**
     * 存储桶名称（含 APPID 后缀），如 example-1234567890
     */
    private String bucketName;

    /**
     * 自定义访问域名（可选），不填则使用默认 CDN 域名
     */
    private String baseUrl;

}
