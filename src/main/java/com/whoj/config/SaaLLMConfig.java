package com.whoj.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

// todo 跟着视频敲的，不知道有什么用
@Configuration
public class SaaLLMConfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

//    @Bean
//    public DashScopeApi dashScopeApi() {
//        return DashScopeApi.builder()
//                .apiKey(apiKey)
//                .build();
//    }

    @Bean
    public DashScopeApi dashScopeApi(DashScopeConnectionProperties connectionProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10 * 10);
        requestFactory.setReadTimeout(30 * 1000);

        // 配置 Reactor Netty 的连接池策略
        ConnectionProvider provider = ConnectionProvider
                .builder("dashscope-provider")
                .maxConnections(500)
                // 空闲30秒后主动关闭，避免复用已被服务端关闭的连接
                .maxIdleTime(Duration.ofSeconds(45))
                .maxLifeTime(Duration.ofMinutes(1440))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .compress(true)
                .keepAlive(true)
                .responseTimeout(java.time.Duration.ofSeconds(60));
        return DashScopeApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .webClientBuilder(WebClient.builder()
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                        .defaultHeader("Connection", "close")
                )
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        // 创建自定义的连接提供者，设置最大空闲时间为 30 秒
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-connection-pool")
                .maxIdleTime(Duration.ofSeconds(30)).maxLifeTime(Duration.ofSeconds(60)) // 核心配置！
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000) // 连接超时
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30))   // 读超时
                                .addHandlerLast(new WriteTimeoutHandler(30)) // 写超时
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
