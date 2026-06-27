package org.example.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Sentinel Push 模式 - Nacos 数据源配置
 * <p>
 * 使用 Nacos 作为 Sentinel 规则的持久化存储和动态推送中心。
 * 规则变更流程：Sentinel Dashboard → Nacos Config → Sentinel Client（实时生效）
 * <p>
 * Nacos 中需要创建以下 Data ID 的配置：
 * <ul>
 *   <li>{@code sentinel-gateway-flow-rules} — 网关限流规则（GatewayFlowRule JSON 数组）</li>
 *   <li>{@code sentinel-gateway-api-definitions} — 网关 API 分组定义</li>
 *   <li>{@code sentinel-degrade-rules} — 熔断降级规则（DegradeRule JSON 数组）</li>
 * </ul>
 * <p>
 * 注意事项：
 * <ul>
 *   <li>规则首次从 Nacos 拉取，后续 Nacos 推送变更实时生效</li>
 *   <li>若 Nacos 不可达，将使用 {@link SentinelGatewayConfig} 中的硬编码默认规则</li>
 * </ul>
 */
@Slf4j
@Configuration
public class SentinelNacosDataSourceConfig {

    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String nacosServerAddr;

    @Value("${spring.cloud.nacos.discovery.username}")
    private String nacosUsername;

    @Value("${spring.cloud.nacos.discovery.password}")
    private String nacosPassword;

    /** Nacos 配置分组 */
    private static final String GROUP_ID = "SENTINEL_GROUP";

    // ==================== Data ID 常量 ====================

    /** 网关限流规则 Data ID */
    private static final String FLOW_RULES_DATA_ID = "sentinel-gateway-flow-rules";

    /** 网关 API 分组 Data ID */
    private static final String API_DEFINITIONS_DATA_ID = "sentinel-gateway-api-definitions";

    /** 熔断降级规则 Data ID */
    private static final String DEGRADE_RULES_DATA_ID = "sentinel-degrade-rules";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initNacosDataSources() {
        log.info("正在初始化 Sentinel Nacos 数据源，Nacos 地址: {}", nacosServerAddr);
        try {
            initGatewayFlowRulesDataSource();
            initApiDefinitionsDataSource();
            initDegradeRulesDataSource();
            log.info("Sentinel Nacos 数据源初始化完成");
        } catch (Exception e) {
            log.error("Sentinel Nacos 数据源初始化失败，将使用默认规则兜底", e);
        }
    }

    /**
     * 构建 Nacos 连接配置
     */
    private Properties buildNacosProperties() {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacosServerAddr);
        if (nacosUsername != null && !nacosUsername.isBlank()) {
            properties.setProperty(PropertyKeyConst.USERNAME, nacosUsername);
        }
        if (nacosPassword != null && !nacosPassword.isBlank()) {
            properties.setProperty(PropertyKeyConst.PASSWORD, nacosPassword);
        }
        return properties;
    }

    /**
     * 初始化网关限流规则数据源（Push 模式）
     * <p>
     * 使用 NacosDataSource 自动订阅 Nacos 配置变更，
     * 当 Dashboard 推送规则到 Nacos 后，客户端自动加载新规则。
     */
    private void initGatewayFlowRulesDataSource() {
        NacosDataSource<Set<GatewayFlowRule>> dataSource = new NacosDataSource<>(
                buildNacosProperties(), GROUP_ID, FLOW_RULES_DATA_ID,
                source -> {
                    try {
                        List<GatewayFlowRule> list = objectMapper.readValue(source,
                                new TypeReference<List<GatewayFlowRule>>() {});
                        Set<GatewayFlowRule> rules = new HashSet<>(list);
                        log.info("[Sentinel Push] 网关限流规则已从 Nacos 加载，共 {} 条", rules.size());
                        rules.forEach(r -> log.info("  -> resource={}, count={}, grade={}",
                                r.getResource(), r.getCount(), r.getGrade()));
                        return rules;
                    } catch (Exception e) {
                        log.error("解析网关限流规则失败，将使用现有规则", e);
                        return null;
                    }
                });

        GatewayRuleManager.register2Property(dataSource.getProperty());
        log.info("网关限流规则数据源已注册: group={}, dataId={}", GROUP_ID, FLOW_RULES_DATA_ID);
    }

    /**
     * 初始化网关 API 分组定义数据源（Push 模式）
     */
    private void initApiDefinitionsDataSource() {
        NacosDataSource<Set<ApiDefinition>> dataSource = new NacosDataSource<>(
                buildNacosProperties(), GROUP_ID, API_DEFINITIONS_DATA_ID,
                source -> {
                    try {
                        Set<ApiDefinition> definitions = objectMapper.readValue(source,
                                new TypeReference<Set<ApiDefinition>>() {});
                        log.info("[Sentinel Push] 网关 API 分组已从 Nacos 加载，共 {} 组", definitions.size());
                        definitions.forEach(d -> log.info("  -> apiName={}", d.getApiName()));
                        return definitions;
                    } catch (Exception e) {
                        log.error("解析网关 API 分组定义失败，将使用现有定义", e);
                        return null;
                    }
                });

        GatewayApiDefinitionManager.register2Property(dataSource.getProperty());
        log.info("网关 API 分组数据源已注册: group={}, dataId={}", GROUP_ID, API_DEFINITIONS_DATA_ID);
    }

    /**
     * 初始化熔断降级规则数据源（Push 模式）
     */
    private void initDegradeRulesDataSource() {
        NacosDataSource<List<DegradeRule>> dataSource = new NacosDataSource<>(
                buildNacosProperties(), GROUP_ID, DEGRADE_RULES_DATA_ID,
                source -> {
                    try {
                        List<DegradeRule> rules = objectMapper.readValue(source,
                                new TypeReference<List<DegradeRule>>() {});
                        log.info("[Sentinel Push] 熔断降级规则已从 Nacos 加载，共 {} 条", rules.size());
                        rules.forEach(r -> log.info("  -> resource={}, grade={}, count={}, timeWindow={}s",
                                r.getResource(), r.getGrade(), r.getCount(), r.getTimeWindow()));
                        return rules;
                    } catch (Exception e) {
                        log.error("解析熔断降级规则失败，将使用现有规则", e);
                        return null;
                    }
                });

        DegradeRuleManager.register2Property(dataSource.getProperty());
        log.info("熔断降级规则数据源已注册: group={}, dataId={}", GROUP_ID, DEGRADE_RULES_DATA_ID);
    }
}
