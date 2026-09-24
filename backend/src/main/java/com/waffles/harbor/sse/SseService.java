package com.waffles.harbor.sse;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class SseService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final DockerClient dockerClient;

    public SseEmitter streamContainerLogs(String containerId){
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // for long-lived SSE connections

        executorService.execute(() -> {

            ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<Frame>(){
                @Override
                public void onNext(Frame frame) {
                    // Frames represent a line or chunk of the log (STDOUT or STDERR)
                    String log = new String(frame.getPayload(), StandardCharsets.UTF_8);

                    try{
                        emitter.send(log);

                    }catch (Exception e){
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    emitter.completeWithError(throwable);
                }

                @Override
                public void onComplete() {
                    emitter.complete();
                }
            };

            Runnable cleanup = () -> {
                try{
                    callback.close();
                }catch(IOException e){
                    throw new RuntimeException(e);
                }
            };

            emitter.onCompletion(cleanup);
            emitter.onTimeout(cleanup);
            emitter.onError(e -> cleanup.run());

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)    // Include standard output
                    .withStdErr(true)    // Include standard error
                    .withFollowStream(true)    // Stream logs in real-time (similar to 'docker logs -f')
                    .withTail(100)
                    .exec(callback);
        });

        return emitter;
    }
}
