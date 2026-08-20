package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 异步回调指令。
 *
 * <p>刻意不依赖 Servlet API：宿主 Controller 负责把 HTTP 请求摘取为
 * method/uri/headers/body，SDK 与传输层解耦（也便于在 MQ 转发等场景复用）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackCommand {

    private String channelCode;

    private String orderNo;

    private String outTradeNo;

    /** HTTP 方法（验签可能需要），如 "POST" */
    private String httpMethod;

    /** 请求 URI（验签可能需要） */
    private String requestUri;

    /** 请求头（验签常用：signature、request-time、client-id 等），key 建议小写 */
    private Map<String, String> headers;

    /** 原始报文体（验签必须用原始字节串，不要用反序列化后再序列化的结果） */
    private String body;

    /** 已解析的回调对象（宿主 Controller @RequestBody 的产物），可为 null，Provider 自行决定用 body 还是它 */
    private Object parsedRequest;
}
