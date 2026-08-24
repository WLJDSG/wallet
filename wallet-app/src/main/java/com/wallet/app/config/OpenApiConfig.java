package com.wallet.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger 3）文档信息。在线文档：/swagger-ui.html（业务端口 8080）。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI walletOpenApi() {
        return new OpenAPI().info(new Info()
            .title("wallet 钱包 API")
            .description("拆分支付（券/积分/余额/三方渠道）+ 用户资产 + 退款。"
                + "用户识别用请求头 X-Uid（会员中心接入后由网关注入）；来源商城用 X-App-Id（缺省 DEFAULT）。"
                + "统一返回体 ApiResult{code, message, data, traceId, timestamp, success}，code=\"0\" 即成功；"
                + "业务失败 HTTP 200、参数错 400、限流/锁冲突 429、系统异常 500。")
            .version("1.0.0"));
    }
}
