package org.kowboy.task;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kowboy.task.LightLevelTask;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class LightLevelTaskTest {
    @Mock private JavaPlugin pluginMock;
    @Mock private Player playerMock;
    @Mock private Runnable onCancelMock;
    @Mock private BukkitTask taskMock;
    private static BukkitScheduler schedulerMock;
    private static Logger loggerMock;

    private LightLevelTask task;

    @BeforeClass
    public static void beforeClass() {
        Server serverMock = mock(Server.class);
        loggerMock = mock(Logger.class);
        schedulerMock = mock(BukkitScheduler.class);

        when(serverMock.getScheduler()).thenReturn(schedulerMock);
        when(serverMock.getLogger()).thenReturn(loggerMock);

        Bukkit.setServer(serverMock);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(taskMock.getTaskId()).thenReturn(1);

        task = new LightLevelTask(2, playerMock, onCancelMock);

        reset(schedulerMock);
        when(schedulerMock.runTask(pluginMock, (Runnable) task)).thenReturn(taskMock);
        task.runTask(pluginMock);
    }


    @Test
    public void testPlayerOffline() {
        when(playerMock.isOnline()).thenReturn(false);
        task.run();
        verify(onCancelMock).run();
    }

    @Test
    public void testPlayerNotSneaking() {
        when(playerMock.isOnline()).thenReturn(true);
        when(playerMock.isSneaking()).thenReturn(false);
        task.run();
        verify(onCancelMock).run();
    }
}
