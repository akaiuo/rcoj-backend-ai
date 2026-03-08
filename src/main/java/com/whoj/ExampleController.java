package com.whoj;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/")
public class ExampleController {
    @Resource
    private ChatModel chatModel;

    private final String AUTH_REQUEST_HEAD = "authKey";

    @Value("${security.authKey}")
    private String AUTH_KEY;

    @GetMapping(value = "/streamChat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam(value = "msg", defaultValue = "who are you?") String msg, HttpServletResponse response) {
        // 禁用缓存（后端层加固）
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        // 禁用压缩
        response.setHeader("Content-Encoding", "identity");
        return chatModel.stream(msg);
    }

    @PostMapping(value = "/findCodeError", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> findCodeError(@RequestBody FindErrorCodeRequest findErrorCodeRequest, HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEAD);
        if (authHeader == null || !authHeader.equals(AUTH_KEY)) {
            return Flux.just("authKey is null or invalid");
        }
        Flux<String> stringFlux = chatModel.stream("根据错误信息：" + findErrorCodeRequest.getErrMsg() + "\n" + "和代码：" + findErrorCodeRequest.getCode() + "，分析错误原因并给出解决方案。");
        System.out.println(111);
        stringFlux.doOnNext(str -> System.out.print(str) ).subscribe();
        /**
         * SSE 必须强制这几个头，少一个就可能被吞：
         * Content-Type: text/event-stream
         * Cache-Control: no-cache, no-transform
         * Connection: keep-alive
         * Transfer-Encoding: chunked
         */
        response.setHeader("Content-Type", "text/event-stream");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Transfer-Encoding", "chunked");
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        // 对换行和空格转义，不然可能会丢
        return stringFlux.map(data -> {
            data = data.replace("\n", "#n");
            data = data.replace(" ", "#sp");
            return data;
        });
    }

    @GetMapping("/testFlux")
    public Flux<String> testFlux() {
        return Flux.just("第一条数据", "第二条数据", "第三条数据", "第四条数据", "第五条数据")
                .delayElements(Duration.ofSeconds(1)) // 模拟异步流式返回
                .onErrorResume(e -> Flux.just("出错了：" + e.getMessage())); // 异常兜底
    }
}
