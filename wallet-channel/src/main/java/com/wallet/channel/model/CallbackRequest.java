package com.wallet.channel.model;

import java.util.Map;

/**
 * 异步回调请求。
 *
 * <p>刻意不依赖 Servlet API：调用方 Controller 负责把 HTTP 请求摘取为
 * method/uri/headers/body，内核与传输层解耦（也便于在 MQ 转发等场景复用）。</p>
 *
 * @param channelCode   渠道编码
 * @param orderNo       业务订单号
 * @param outTradeNo    交易号
 * @param httpMethod    HTTP 方法（验签可能需要），如 "POST"
 * @param requestUri    请求 URI（验签可能需要）
 * @param headers       请求头（验签常用：signature、request-time 等），key 建议小写
 * @param body          原始报文体（验签必须用原始字节串，不要用反序列化后再序列化的结果）
 * @param parsedRequest 已解析的回调对象，可为 null，渠道实现自行决定用 body 还是它
 */
public record CallbackRequest(String channelCode, String orderNo, String outTradeNo, String httpMethod,
    String requestUri, Map<String, String> headers, String body, Object parsedRequest) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String channelCode;
        private String orderNo;
        private String outTradeNo;
        private String httpMethod;
        private String requestUri;
        private Map<String, String> headers;
        private String body;
        private Object parsedRequest;

        public Builder channelCode(String channelCode) {
            this.channelCode = channelCode;
            return this;
        }

        public Builder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        public Builder outTradeNo(String outTradeNo) {
            this.outTradeNo = outTradeNo;
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder parsedRequest(Object parsedRequest) {
            this.parsedRequest = parsedRequest;
            return this;
        }

        public CallbackRequest build() {
            return new CallbackRequest(channelCode, orderNo, outTradeNo, httpMethod, requestUri, headers, body,
                parsedRequest);
        }
    }
}
