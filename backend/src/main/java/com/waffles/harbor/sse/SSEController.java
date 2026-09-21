package com.waffles.harbor.sse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SSEController {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @GetMapping("/sse")
    public SseEmitter emitSse(){
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // for long lived SSE connections

        executorService.execute(() -> {
            try{
                emitter.send("hi");

                emitter.complete();
            }catch (Exception e){
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
