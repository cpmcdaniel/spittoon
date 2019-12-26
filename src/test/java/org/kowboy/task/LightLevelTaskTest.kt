package org.kowboy.task

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.logging.Logger

class LightLevelTaskTest {
    @MockK lateinit var playerMock: Player
    @MockK lateinit var pluginMock: JavaPlugin
    @MockK lateinit var onCancelMock: Runnable
    @MockK lateinit var taskMock: BukkitTask

    private lateinit var sut: LightLevelTask

    companion object {
        private lateinit var schedulerMock: BukkitScheduler
        private lateinit var loggerMock: Logger

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val serverMock = mockk<Server>(relaxed = true)
            loggerMock = mockk(relaxed = true)
            schedulerMock = mockk(relaxed = true)
            every { serverMock.scheduler } returns schedulerMock
            every { serverMock.logger } returns loggerMock
            every { serverMock.name } returns "Test Server"
            Bukkit.setServer(serverMock)
        }
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { taskMock.taskId } returns 1
        sut= LightLevelTask(2, playerMock, onCancelMock)

        clearMocks(schedulerMock)
        every { schedulerMock.runTask(pluginMock, sut as Runnable) } returns taskMock
        sut.runTask(pluginMock)
    }

    @Test
    fun testPlayerOffline() {
        every { playerMock.isOnline } returns false
        sut.run()
        verify { onCancelMock.run() }
    }

    @Test
    fun testPlayerNotSneaking() {
        every { playerMock.isOnline } returns true
        every { playerMock.isSneaking } returns false

        sut.run()
        verify { onCancelMock.run() }
    }

}