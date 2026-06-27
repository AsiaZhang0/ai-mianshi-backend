package org.example.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sentinel Gateway 默认规则配置（Fallback）
 * <p>
 * 基准：登录接口可稳定承受 761 TPS，错误率 0%
 * <p>
 * 三层防护：
 * 1. 全局限流：网关总入口 QPS ≤ 1826，防整体过载
 * 2. 路由限流 + IP 级限流：每路由 QPS ≤ 608，单 IP 每路由 QPS ≤ 100，防单用户刷接口
 * 3. 熔断降级：异常比例 ≥ 30% → 熔断 10s，防故障蔓延
 * <p>
 * 规则优先级：Nacos 数据源（Push 模式）> 本默认规则
 * 本类作为 Nacos 不可用时的兜底默认规则，在应用启动后延迟加载。
 * 当 Nacos 数据源就绪后，Nacos 中的规则将自动覆盖此默认规则。
 *
 * @see SentinelNacosDataSourceConfig Nacos Push 模式数据源
 */
//@Configuration
public class SentinelGatewayConfig {

    /** 登录接口基准 TPS（实测 761 TPS 0% 错误率） */
    private static final int BASELINE_TPS = 761;
    /** 路由级限流阈值 = 基准 × 安全系数（80%） */
    private static final int ROUTE_FLOW_LIMIT = (int) (BASELINE_TPS * 0.8);
    /** 全局总入口限流阈值 */
    private static final int GLOBAL_FLOW_LIMIT = (int)(BASELINE_TPS * 0.8*3);
    /** 单 IP 每路由 QPS 限流阈值 */
    private static final int IP_PER_ROUTE_LIMIT = 100;

    /**
     * 使用 ApplicationReadyEvent 延迟加载默认规则。
     * 若 Nacos 数据源已注册（在 @PostConstruct 阶段），则 Nacos 规则优先；
     * 若 Nacos 中无配置，本默认规则作为兜底。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultRules() {
        initApiDefinitions();
        initFlowRules();
        initDegradeRules();
    }

    /**
     * 自定义 API 分组
     * gateway-global：匹配所有 /api/** 路径，用于全局限流
     */
    private void initApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();

        ApiDefinition globalApi = new ApiDefinition("gateway-global");
        Set<ApiPredicateItem> predicates = new HashSet<>();
        predicates.add(new ApiPathPredicateItem()
                .setPattern("/api/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        globalApi.setPredicateItems(predicates);
        definitions.add(globalApi);

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    /**
     * 限流规则
     * 1. 全局限流：网关总入口 QPS ≤ 1000
     * 2. 路由限流：每路由 QPS ≤ 600
     * 3. IP 限流：单 IP 每路由 QPS ≤ 20（防恶意刷接口）
     */
    private void initFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

//        // ---- 全局限流 ----
        rules.add(new GatewayFlowRule("gateway-global")
                .setCount(GLOBAL_FLOW_LIMIT)
                .setGrade(RuleConstant.FLOW_GRADE_QPS));

        // ---- 路由级限流 + IP 限流 ----
        // 注意：resource 名称必须与 yaml 中 gateway.routes[].id 完全一致
        String[] routeIds = {"user-service", "questions-service", "questionbank"};
        for (String routeId : routeIds) {
            // 路由总 QPS 限流
            rules.add(new GatewayFlowRule(routeId)
                    .setCount(ROUTE_FLOW_LIMIT)
                    .setGrade(RuleConstant.FLOW_GRADE_QPS));

            // 单 IP 限流：同一 IP 对该路由每秒最多 20 次请求
            rules.add(new GatewayFlowRule(routeId)
                    .setCount(IP_PER_ROUTE_LIMIT)
                    .setGrade(RuleConstant.FLOW_GRADE_QPS)
                    .setParamItem(new GatewayParamFlowItem()
                            .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP)));
        }

        GatewayRuleManager.loadRules(rules);
    }

    /**
     * 熔断降级规则
     * 注意：Sentinel Gateway 的熔断降级对路由级 resource（如 "user-service"）支持有限，
     * 改为对全局 API 分组 "gateway-global" 做熔断，更稳定可靠。
     * 10s 窗口内请求 ≥ 50 次 & 异常比例 ≥ 30% → 熔断 10s
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 全局熔断：整个网关的异常比例超过 30% 时熔断
        DegradeRule globalRule = new DegradeRule("gateway-global")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.3)
                .setTimeWindow(10)
                .setMinRequestAmount(50)
                .setStatIntervalMs(10000);
        rules.add(globalRule);

        DegradeRuleManager.loadRules(rules);
    }
}
