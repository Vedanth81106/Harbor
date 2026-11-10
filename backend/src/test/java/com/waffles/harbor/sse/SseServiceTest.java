package com.waffles.harbor.sse;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

    private static final String CONTAINER_ID = "container-id";

    @Mock
    private ThreadPoolTaskExecutor executor;

    @Mock
    private DockerClient dockerClient;

    @InjectMocks
    private SseService sseService;

    @Test
    void shouldScheduleLogStreamingTask() {
        SseEmitter emitter = sseService.streamContainerLogs(CONTAINER_ID);

        assertNotNull(emitter);
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    void shouldConfigureLogCommandToFollowStdOutAndStdErr() {
        sseService.streamContainerLogs(CONTAINER_ID);
        Runnable task = captureScheduledTask();
        LogContainerCmd logContainerCmd = stubLogCommand();

        task.run();

        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(logContainerCmd).withStdOut(true);
        verify(logContainerCmd).withStdErr(true);
        verify(logContainerCmd).withFollowStream(true);
        verify(logContainerCmd).withTail(100);
        verify(logContainerCmd).exec(any());
    }

    @Test
    void shouldForwardLogFramesToTheEmitter() {
        SseEmitter emitter = sseService.streamContainerLogs(CONTAINER_ID);
        ResultCallback.Adapter<Frame> callback = runTaskAndCaptureCallback();

        callback.onNext(new Frame(StreamType.STDOUT, "hello".getBytes(StandardCharsets.UTF_8)));

        // SseEmitter frames a value as three parts: "data:", the payload, and the trailing newline.
        @SuppressWarnings("unchecked")
        Set<ResponseBodyEmitter.DataWithMediaType> earlySendAttempts =
                (Set<ResponseBodyEmitter.DataWithMediaType>) ReflectionTestUtils.getField(
                        emitter,
                        "earlySendAttempts"
                );
        assertNotNull(earlySendAttempts);
        assertTrue(
                earlySendAttempts.stream()
                        .map(ResponseBodyEmitter.DataWithMediaType::getData)
                        .anyMatch("hello"::equals)
        );
    }

    @Test
    void shouldCompleteEmitterWhenLogStreamCompletes() {
        SseEmitter emitter = sseService.streamContainerLogs(CONTAINER_ID);
        ResultCallback.Adapter<Frame> callback = runTaskAndCaptureCallback();

        callback.onComplete();

        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(emitter, "complete"));
    }

    @Test
    void shouldCompleteEmitterWithErrorWhenLogStreamFails() {
        SseEmitter emitter = sseService.streamContainerLogs(CONTAINER_ID);
        ResultCallback.Adapter<Frame> callback = runTaskAndCaptureCallback();

        RuntimeException failure = new RuntimeException("docker stream failed");
        callback.onError(failure);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(emitter, "complete"));
        assertSame(failure, ReflectionTestUtils.getField(emitter, "failure"));
    }

    @Test
    void shouldCompleteEmitterWithErrorWhenExecutorRejectsTask() {
        TaskRejectedException rejection = new TaskRejectedException("executor is full");
        doThrow(rejection).when(executor).execute(any(Runnable.class));

        SseEmitter emitter = sseService.streamContainerLogs(CONTAINER_ID);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(emitter, "complete"));
        assertSame(rejection, ReflectionTestUtils.getField(emitter, "failure"));
        verifyNoInteractions(dockerClient);
    }

    private Runnable captureScheduledTask() {
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(taskCaptor.capture());
        return taskCaptor.getValue();
    }

    private LogContainerCmd stubLogCommand() {
        LogContainerCmd logContainerCmd = mock(LogContainerCmd.class);

        when(dockerClient.logContainerCmd(CONTAINER_ID)).thenReturn(logContainerCmd);
        when(logContainerCmd.withStdOut(true)).thenReturn(logContainerCmd);
        when(logContainerCmd.withStdErr(true)).thenReturn(logContainerCmd);
        when(logContainerCmd.withFollowStream(true)).thenReturn(logContainerCmd);
        when(logContainerCmd.withTail(100)).thenReturn(logContainerCmd);
        when(logContainerCmd.exec(any())).thenAnswer(invocation -> invocation.getArgument(0));

        return logContainerCmd;
    }

    @SuppressWarnings("unchecked")
    private ResultCallback.Adapter<Frame> runTaskAndCaptureCallback() {
        Runnable task = captureScheduledTask();
        LogContainerCmd logContainerCmd = stubLogCommand();

        task.run();

        ArgumentCaptor<ResultCallback.Adapter<Frame>> callbackCaptor =
                ArgumentCaptor.forClass(ResultCallback.Adapter.class);
        verify(logContainerCmd).exec(callbackCaptor.capture());

        return callbackCaptor.getValue();
    }
}
