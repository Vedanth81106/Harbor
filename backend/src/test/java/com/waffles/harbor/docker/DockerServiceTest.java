package com.waffles.harbor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.command.DisconnectFromNetworkCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.InspectNetworkCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.command.RemoveNetworkCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerServiceTest {

    private static final UUID DEPLOYMENT_ID =
            UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final String NETWORK_NAME = "net-12345678";

    @Mock
    private DockerClient dockerClient;

    @InjectMocks
    private DockerService dockerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dockerService, "domainName", "example.com");
    }

    // ---------- buildImage ----------

    @Test
    void shouldBuildImageWithDirectoryAndDockerfile(@TempDir Path directory) {
        BuildImageCmd buildImageCmd = mock(BuildImageCmd.class);
        BuildImageResultCallback callback = mock(BuildImageResultCallback.class);

        when(dockerClient.buildImageCmd()).thenReturn(buildImageCmd);
        when(buildImageCmd.withBaseDirectory(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withDockerfile(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withTags(anySet())).thenReturn(buildImageCmd);
        when(buildImageCmd.exec(any(BuildImageResultCallback.class))).thenReturn(callback);
        when(callback.awaitImageId()).thenReturn("image-id");

        dockerService.buildImage(directory.toString(), "project:latest");

        verify(buildImageCmd).withBaseDirectory(new File(directory.toString()));
        verify(buildImageCmd).withDockerfile(new File(directory.toFile(), "Dockerfile"));
        verify(buildImageCmd).withTags(Set.of("project:latest"));
        verify(callback).awaitImageId();
    }

    // ---------- createContainer ----------

    @Test
    void shouldCreateContainerOnIsolatedNetworkWithTraefikLabels() {
        CreateNetworkCmd createNetworkCmd = mock(CreateNetworkCmd.class);
        when(dockerClient.createNetworkCmd()).thenReturn(createNetworkCmd);
        when(createNetworkCmd.withName(NETWORK_NAME)).thenReturn(createNetworkCmd);
        when(createNetworkCmd.withDriver("bridge")).thenReturn(createNetworkCmd);

        CreateContainerCmd createContainerCmd = mock(CreateContainerCmd.class);
        when(dockerClient.createContainerCmd("project:latest")).thenReturn(createContainerCmd);
        when(createContainerCmd.withLabels(anyMap())).thenReturn(createContainerCmd);
        when(createContainerCmd.withName("harbor-12345678")).thenReturn(createContainerCmd);
        when(createContainerCmd.withHostConfig(any(HostConfig.class))).thenReturn(createContainerCmd);

        CreateContainerResponse response = new CreateContainerResponse();
        response.setId("container-id");
        when(createContainerCmd.exec()).thenReturn(response);

        String containerId = dockerService.createContainer(
                DEPLOYMENT_ID,
                "project:latest",
                8080,
                "harbor-12345678"
        );

        assertEquals("container-id", containerId);
        verify(createNetworkCmd).exec();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> labelsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(createContainerCmd).withLabels(labelsCaptor.capture());
        Map<String, String> labels = labelsCaptor.getValue();

        assertEquals("true", labels.get("traefik.enable"));
        assertEquals(NETWORK_NAME, labels.get("traefik.docker.network"));
        assertEquals(
                "web",
                labels.get("traefik.http.routers.harbor-12345678.entrypoints")
        );
        assertEquals(
                "Host(`harbor-12345678.example.com`)",
                labels.get("traefik.http.routers.harbor-12345678.rule")
        );
        assertEquals(
                "8080",
                labels.get("traefik.http.services.harbor-12345678.loadbalancer.server.port")
        );

        ArgumentCaptor<HostConfig> hostConfigCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(createContainerCmd).withHostConfig(hostConfigCaptor.capture());
        HostConfig hostConfig = hostConfigCaptor.getValue();

        assertEquals(NETWORK_NAME, hostConfig.getNetworkMode());
        assertEquals(512L * 1024 * 1024, hostConfig.getMemory().longValue());
        assertEquals(100_000L, hostConfig.getCpuPeriod().longValue());
        assertEquals(50_000L, hostConfig.getCpuQuota().longValue());
        assertEquals(100L, hostConfig.getPidsLimit().longValue());
        assertNotNull(hostConfig.getRestartPolicy());
    }

    @Test
    void shouldRemoveNetworkWhenContainerCreationFails() {
        CreateNetworkCmd createNetworkCmd = mock(CreateNetworkCmd.class);
        when(dockerClient.createNetworkCmd()).thenReturn(createNetworkCmd);
        when(createNetworkCmd.withName(NETWORK_NAME)).thenReturn(createNetworkCmd);
        when(createNetworkCmd.withDriver("bridge")).thenReturn(createNetworkCmd);

        CreateContainerCmd createContainerCmd = mock(CreateContainerCmd.class);
        when(dockerClient.createContainerCmd("project:latest")).thenReturn(createContainerCmd);
        when(createContainerCmd.withLabels(anyMap())).thenReturn(createContainerCmd);
        when(createContainerCmd.withName("harbor-12345678")).thenReturn(createContainerCmd);
        when(createContainerCmd.withHostConfig(any(HostConfig.class))).thenReturn(createContainerCmd);
        when(createContainerCmd.exec()).thenThrow(new RuntimeException("docker failure"));

        RemoveNetworkCmd removeNetworkCmd = mock(RemoveNetworkCmd.class);
        when(dockerClient.removeNetworkCmd(NETWORK_NAME)).thenReturn(removeNetworkCmd);

        assertThrows(
                RuntimeException.class,
                () -> dockerService.createContainer(
                        DEPLOYMENT_ID,
                        "project:latest",
                        8080,
                        "harbor-12345678"
                )
        );

        verify(removeNetworkCmd).exec();
    }

    // ---------- startContainer ----------

    @Test
    void shouldConnectTraefikAndStartContainer() {
        InspectContainerCmd inspectTraefik = mock(InspectContainerCmd.class);
        InspectContainerResponse traefikInfo = mock(InspectContainerResponse.class);
        when(dockerClient.inspectContainerCmd("traefik")).thenReturn(inspectTraefik);
        when(inspectTraefik.exec()).thenReturn(traefikInfo);
        when(traefikInfo.getId()).thenReturn("traefik-id");

        InspectNetworkCmd inspectNetwork = mock(InspectNetworkCmd.class);
        Network network = mock(Network.class);
        when(dockerClient.inspectNetworkCmd()).thenReturn(inspectNetwork);
        when(inspectNetwork.withNetworkId(NETWORK_NAME)).thenReturn(inspectNetwork);
        when(inspectNetwork.exec()).thenReturn(network);
        when(network.getContainers()).thenReturn(Map.of());

        ConnectToNetworkCmd connect = mock(ConnectToNetworkCmd.class);
        when(dockerClient.connectToNetworkCmd()).thenReturn(connect);
        when(connect.withNetworkId(NETWORK_NAME)).thenReturn(connect);
        when(connect.withContainerId("traefik-id")).thenReturn(connect);

        StartContainerCmd start = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("container-id")).thenReturn(start);

        dockerService.startContainer("container-id", DEPLOYMENT_ID);

        verify(connect).exec();
        verify(start).exec();
    }

    @Test
    void shouldNotReconnectTraefikWhenAlreadyConnected() {
        InspectContainerCmd inspectTraefik = mock(InspectContainerCmd.class);
        InspectContainerResponse traefikInfo = mock(InspectContainerResponse.class);
        when(dockerClient.inspectContainerCmd("traefik")).thenReturn(inspectTraefik);
        when(inspectTraefik.exec()).thenReturn(traefikInfo);
        when(traefikInfo.getId()).thenReturn("traefik-id");

        InspectNetworkCmd inspectNetwork = mock(InspectNetworkCmd.class);
        Network network = mock(Network.class);
        when(dockerClient.inspectNetworkCmd()).thenReturn(inspectNetwork);
        when(inspectNetwork.withNetworkId(NETWORK_NAME)).thenReturn(inspectNetwork);
        when(inspectNetwork.exec()).thenReturn(network);
        when(network.getContainers()).thenReturn(
                Map.of("traefik-id", mock(Network.ContainerNetworkConfig.class))
        );

        StartContainerCmd start = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("container-id")).thenReturn(start);

        dockerService.startContainer("container-id", DEPLOYMENT_ID);

        verify(dockerClient, never()).connectToNetworkCmd();
        verify(start).exec();
    }

    // ---------- stopContainer / removeContainer ----------

    @Test
    void shouldStopRunningContainer() {
        InspectContainerCmd inspect = mock(InspectContainerCmd.class);
        InspectContainerResponse info = mock(InspectContainerResponse.class);
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspect);
        when(inspect.exec()).thenReturn(info);
        when(info.getState()).thenReturn(state);
        when(state.getRunning()).thenReturn(true);

        StopContainerCmd stop = mock(StopContainerCmd.class);
        when(dockerClient.stopContainerCmd("container-id")).thenReturn(stop);

        dockerService.stopContainer("container-id");

        verify(stop).exec();
    }

    @Test
    void shouldNotStopAlreadyStoppedContainer() {
        InspectContainerCmd inspect = mock(InspectContainerCmd.class);
        InspectContainerResponse info = mock(InspectContainerResponse.class);
        InspectContainerResponse.ContainerState state = mock(InspectContainerResponse.ContainerState.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspect);
        when(inspect.exec()).thenReturn(info);
        when(info.getState()).thenReturn(state);
        when(state.getRunning()).thenReturn(false);

        dockerService.stopContainer("container-id");

        verify(dockerClient, never()).stopContainerCmd(any());
    }

    @Test
    void shouldIgnoreMissingContainerWhenStopping() {
        InspectContainerCmd inspect = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspect);
        when(inspect.exec()).thenThrow(new NotFoundException("absent"));

        assertDoesNotThrow(() -> dockerService.stopContainer("container-id"));

        verify(dockerClient, never()).stopContainerCmd(any());
    }

    @Test
    void shouldIgnoreMissingContainerWhenRemoving() {
        RemoveContainerCmd remove = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("container-id")).thenReturn(remove);
        when(remove.exec()).thenThrow(new NotFoundException("absent"));

        assertDoesNotThrow(() -> dockerService.removeContainer("container-id"));
    }

    // ---------- images ----------

    @Test
    void shouldRemoveImageByItsId() {
        InspectImageCmd inspect = mock(InspectImageCmd.class);
        InspectImageResponse image = new InspectImageResponse().withId("image-id");
        when(dockerClient.inspectImageCmd("project:latest")).thenReturn(inspect);
        when(inspect.exec()).thenReturn(image);

        RemoveImageCmd remove = mock(RemoveImageCmd.class);
        when(dockerClient.removeImageCmd("image-id")).thenReturn(remove);

        dockerService.removeImage("project:latest");

        verify(remove).exec();
    }

    @Test
    void shouldIgnoreMissingImageWhenRemoving() {
        InspectImageCmd inspect = mock(InspectImageCmd.class);
        when(dockerClient.inspectImageCmd("project:latest")).thenReturn(inspect);
        when(inspect.exec()).thenThrow(new NotFoundException("absent"));

        assertDoesNotThrow(() -> dockerService.removeImage("project:latest"));

        verify(dockerClient, never()).removeImageCmd(any());
    }

    @Test
    void shouldReturnFirstExposedImagePort() {
        InspectImageCmd inspect = mock(InspectImageCmd.class);
        ContainerConfig config = mock(ContainerConfig.class);
        when(config.getExposedPorts()).thenReturn(
                new ExposedPort[]{new ExposedPort(8080), new ExposedPort(9090)}
        );
        InspectImageResponse image = new InspectImageResponse().withConfig(config);
        when(dockerClient.inspectImageCmd("project:latest")).thenReturn(inspect);
        when(inspect.exec()).thenReturn(image);

        assertEquals(8080, dockerService.getExposedImagePort("project:latest"));
    }

    @Test
    void shouldThrowWhenImageHasNoExposedPort() {
        InspectImageCmd inspect = mock(InspectImageCmd.class);
        ContainerConfig config = mock(ContainerConfig.class);
        when(config.getExposedPorts()).thenReturn(null);
        InspectImageResponse image = new InspectImageResponse().withConfig(config);
        when(dockerClient.inspectImageCmd("project:latest")).thenReturn(inspect);
        when(inspect.exec()).thenReturn(image);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> dockerService.getExposedImagePort("project:latest")
        );

        assertTrue(exception.getMessage().contains("exposed port"));
    }

    // ---------- tearDownDeployment ----------

    @Test
    void shouldTearDownDisconnectContainerAndNetwork() {
        InspectContainerCmd inspectTraefik = mock(InspectContainerCmd.class);
        InspectContainerResponse traefikInfo = mock(InspectContainerResponse.class);
        when(dockerClient.inspectContainerCmd("traefik")).thenReturn(inspectTraefik);
        when(inspectTraefik.exec()).thenReturn(traefikInfo);
        when(traefikInfo.getId()).thenReturn("traefik-id");

        DisconnectFromNetworkCmd disconnect = mock(DisconnectFromNetworkCmd.class);
        when(dockerClient.disconnectFromNetworkCmd()).thenReturn(disconnect);
        when(disconnect.withContainerId("traefik-id")).thenReturn(disconnect);
        when(disconnect.withNetworkId(NETWORK_NAME)).thenReturn(disconnect);

        RemoveContainerCmd remove = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("container-id")).thenReturn(remove);
        when(remove.withForce(true)).thenReturn(remove);

        RemoveNetworkCmd removeNetwork = mock(RemoveNetworkCmd.class);
        when(dockerClient.removeNetworkCmd(NETWORK_NAME)).thenReturn(removeNetwork);

        dockerService.tearDownDeployment("container-id", DEPLOYMENT_ID);

        verify(disconnect).exec();
        verify(remove).exec();
        verify(removeNetwork).exec();
    }

    @Test
    void shouldTearDownNetworkWhenContainerIdWasNeverSaved() {
        InspectContainerCmd inspectTraefik = mock(InspectContainerCmd.class);
        InspectContainerResponse traefikInfo = mock(InspectContainerResponse.class);
        when(dockerClient.inspectContainerCmd("traefik")).thenReturn(inspectTraefik);
        when(inspectTraefik.exec()).thenReturn(traefikInfo);
        when(traefikInfo.getId()).thenReturn("traefik-id");

        DisconnectFromNetworkCmd disconnect = mock(DisconnectFromNetworkCmd.class);
        when(dockerClient.disconnectFromNetworkCmd()).thenReturn(disconnect);
        when(disconnect.withContainerId("traefik-id")).thenReturn(disconnect);
        when(disconnect.withNetworkId(NETWORK_NAME)).thenReturn(disconnect);

        RemoveNetworkCmd removeNetwork = mock(RemoveNetworkCmd.class);
        when(dockerClient.removeNetworkCmd(NETWORK_NAME)).thenReturn(removeNetwork);

        dockerService.tearDownDeployment(null, DEPLOYMENT_ID);

        verify(dockerClient, never()).removeContainerCmd(any());
        verify(removeNetwork).exec();
    }

    @Test
    void shouldStillTearDownWhenTraefikIsMissing() {
        InspectContainerCmd inspectTraefik = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("traefik")).thenReturn(inspectTraefik);
        when(inspectTraefik.exec()).thenThrow(new NotFoundException("absent"));

        RemoveContainerCmd remove = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("container-id")).thenReturn(remove);
        when(remove.withForce(true)).thenReturn(remove);

        RemoveNetworkCmd removeNetwork = mock(RemoveNetworkCmd.class);
        when(dockerClient.removeNetworkCmd(NETWORK_NAME)).thenReturn(removeNetwork);

        dockerService.tearDownDeployment("container-id", DEPLOYMENT_ID);

        verify(dockerClient, never()).disconnectFromNetworkCmd();
        verify(remove).exec();
        verify(removeNetwork).exec();
    }

    // ---------- reconnectTraefik / recoverContainer ----------

    @Test
    void shouldReconnectTraefikToRunningDeployment() {
        InspectContainerCmd inspectApp = mock(InspectContainerCmd.class);
        InspectContainerResponse appInfo = mock(InspectContainerResponse.class);
        InspectContainerResponse.ContainerState appState = mock(InspectContainerResponse.ContainerState.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspectApp);
        when(inspectApp.exec()).thenReturn(appInfo);
        when(appInfo.getState()).thenReturn(appState);
        when(appState.getRunning()).thenReturn(true);

        InspectNetworkCmd inspectNetwork = mock(InspectNetworkCmd.class);
        Network network = mock(Network.class);
        when(dockerClient.inspectNetworkCmd()).thenReturn(inspectNetwork);
        when(inspectNetwork.withNetworkId(NETWORK_NAME)).thenReturn(inspectNetwork);
        when(inspectNetwork.exec()).thenReturn(network);
        when(network.getContainers()).thenReturn(Map.of());

        InspectContainerCmd inspectTraefik = mock(InspectContainerCmd.class);
        InspectContainerResponse traefikInfo = mock(InspectContainerResponse.class);
        when(dockerClient.inspectContainerCmd("traefik")).thenReturn(inspectTraefik);
        when(inspectTraefik.exec()).thenReturn(traefikInfo);
        when(traefikInfo.getId()).thenReturn("traefik-id");

        ConnectToNetworkCmd connect = mock(ConnectToNetworkCmd.class);
        when(dockerClient.connectToNetworkCmd()).thenReturn(connect);
        when(connect.withNetworkId(NETWORK_NAME)).thenReturn(connect);
        when(connect.withContainerId("traefik-id")).thenReturn(connect);

        dockerService.reconnectTraefik(DEPLOYMENT_ID, "container-id");

        verify(connect).exec();
    }

    @Test
    void shouldSkipReconnectWhenContainerIsNotRunning() {
        InspectContainerCmd inspectApp = mock(InspectContainerCmd.class);
        InspectContainerResponse appInfo = mock(InspectContainerResponse.class);
        InspectContainerResponse.ContainerState appState = mock(InspectContainerResponse.ContainerState.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspectApp);
        when(inspectApp.exec()).thenReturn(appInfo);
        when(appInfo.getState()).thenReturn(appState);
        when(appState.getRunning()).thenReturn(false);

        dockerService.reconnectTraefik(DEPLOYMENT_ID, "container-id");

        verify(dockerClient, never()).inspectNetworkCmd();
        verify(dockerClient, never()).connectToNetworkCmd();
    }

    @Test
    void shouldIgnoreMissingContainerWhenReconnectingTraefik() {
        InspectContainerCmd inspectApp = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspectApp);
        when(inspectApp.exec()).thenThrow(new NotFoundException("absent"));

        assertDoesNotThrow(
                () -> dockerService.reconnectTraefik(DEPLOYMENT_ID, "container-id")
        );
    }

    @Test
    void shouldRestartContainerDuringRecoveryWhenItIsNotRunning() {
        InspectContainerCmd inspectApp = mock(InspectContainerCmd.class);
        InspectContainerResponse appInfo = mock(InspectContainerResponse.class);
        InspectContainerResponse.ContainerState appState = mock(InspectContainerResponse.ContainerState.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspectApp);
        when(inspectApp.exec()).thenReturn(appInfo);
        when(appInfo.getState()).thenReturn(appState);
        when(appState.getRunning()).thenReturn(false);

        InspectContainerCmd inspectTraefik = mock(InspectContainerCmd.class);
        InspectContainerResponse traefikInfo = mock(InspectContainerResponse.class);
        when(dockerClient.inspectContainerCmd("traefik")).thenReturn(inspectTraefik);
        when(inspectTraefik.exec()).thenReturn(traefikInfo);
        when(traefikInfo.getId()).thenReturn("traefik-id");

        InspectNetworkCmd inspectNetwork = mock(InspectNetworkCmd.class);
        Network network = mock(Network.class);
        when(dockerClient.inspectNetworkCmd()).thenReturn(inspectNetwork);
        when(inspectNetwork.withNetworkId(NETWORK_NAME)).thenReturn(inspectNetwork);
        when(inspectNetwork.exec()).thenReturn(network);
        when(network.getContainers()).thenReturn(Map.of());

        ConnectToNetworkCmd connect = mock(ConnectToNetworkCmd.class);
        when(dockerClient.connectToNetworkCmd()).thenReturn(connect);
        when(connect.withNetworkId(NETWORK_NAME)).thenReturn(connect);
        when(connect.withContainerId("traefik-id")).thenReturn(connect);

        StartContainerCmd start = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("container-id")).thenReturn(start);

        dockerService.recoverContainer(DEPLOYMENT_ID, "container-id");

        verify(start).exec();
    }

    @Test
    void shouldIgnoreMissingContainerDuringRecovery() {
        InspectContainerCmd inspectApp = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("container-id")).thenReturn(inspectApp);
        when(inspectApp.exec()).thenThrow(new NotFoundException("absent"));

        assertDoesNotThrow(
                () -> dockerService.recoverContainer(DEPLOYMENT_ID, "container-id")
        );
    }
}
