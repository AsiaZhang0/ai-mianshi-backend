package org.example.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度实时语音识别配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baidu.asr")
public class BaiduAsrConfig {

    /** 百度语音 App ID */
    private int appId;

    /** 百度语音 API Key */
    private String apiKey;

    /** 百度语音 Secret Key */
    private String secretKey;

    /** 语言 PID：1537=普通话, 1737=英语 */
    private int devPid = 1537;
}
