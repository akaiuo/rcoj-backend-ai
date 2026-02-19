package com.whoj;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AgentExample {
    @Value("${spring.ai.dashscope.api-key}")
    public String AI_DASHSCOPE_API_KEY;
    public void test() throws Exception {
        System.out.println(AI_DASHSCOPE_API_KEY);
        // 创建模型实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(AI_DASHSCOPE_API_KEY)
                .build();
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
        // 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("weather_agent")
                .model(chatModel)
                .instruction("你是一个天气预报助手")
                .build();

        // 运行 Agent
        AssistantMessage call = agent.call("广州的天气预报?");
        System.out.println(call.getText());
    }
}