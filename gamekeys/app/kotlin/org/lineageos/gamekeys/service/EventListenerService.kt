package org.lineageos.gamekeys.service

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.Intent
import android.os.IBinder
import android.os.ServiceManager
import androidx.core.content.ContextCompat
import org.lineageos.gamekeys.Application
import vendor.lineage.gamekeys.GameKeyEvent
import vendor.lineage.gamekeys.GameKeyEventType
import vendor.lineage.gamekeys.IGameKeys
import vendor.lineage.gamekeys.IGameKeysEventListener
import vendor.lineage.touchinjector.ITouchInjector
import vendor.lineage.touchinjector.ITouchInjectorClient
import vendor.lineage.touchinjector.TouchPosition
import kotlin.math.abs

class EventListenerService : Service() {
    private val activityTaskManager by lazy { ActivityTaskManager.getInstance() }

    private val gameKeysService by lazy {
        val binder = ServiceManager.getServiceOrThrow("vendor.lineage.gamekeys.IGameKeys/default")
        IGameKeys.Stub.asInterface(binder)
    }

    private val touchInjectorService by lazy {
        val binder = ServiceManager.getServiceOrThrow("vendor.lineage.touchinjector.ITouchInjector/default")
        ITouchInjector.Stub.asInterface(binder)
    }

    private val taskStackListener = object : TaskStackListener() {
        override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo?) {
            val pkg = taskInfo?.topActivity?.packageName ?: return
            this@EventListenerService.onTaskFocused(pkg)
        }

        override fun onTaskFocusChanged(taskId: Int, focused: Boolean) {
            if (!focused)
                return

            val top = activityTaskManager.getTasks(1, true)

            val pkg = top.firstOrNull()?.topActivity?.packageName ?: return
            this@EventListenerService.onTaskFocused(pkg)
        }
    }

    private val gameKeyEventListener = object : IGameKeysEventListener.Stub() {
        private var openTimestamps =
            mutableListOf(System.currentTimeMillis(), System.currentTimeMillis())

        private var closeTimestamps =
            mutableListOf(System.currentTimeMillis(), System.currentTimeMillis())

        override fun onGameKeyEvent(event: GameKeyEvent?) {
            if (event == null)
                return

            if (event.id > 1)
                return

            when (event.type) {
                GameKeyEventType.OPEN -> {
                    openTimestamps[event.id] = System.currentTimeMillis()

                    if (abs(openTimestamps[0] - openTimestamps[1]) > 750)
                        return

                    if (OverlayService.isRunning())
                        return

                    val svc = Intent().apply {
                        setClassName(
                            "org.lineageos.gamekeys",
                            "org.lineageos.gamekeys.service.OverlayService"
                        )
                        action = "org.lineageos.gamekeys.intent.BOTH_OPEN"
                    }

                    ContextCompat.startForegroundService(this@EventListenerService, svc)
                }

                GameKeyEventType.CLOSE -> {
                    closeTimestamps[event.id] = System.currentTimeMillis()

                    if (abs(closeTimestamps[0] - closeTimestamps[1]) > 750)
                        return

                    if (!OverlayService.isRunning())
                        return

                    val svc = Intent().apply {
                        setClassName(
                            "org.lineageos.gamekeys",
                            "org.lineageos.gamekeys.service.OverlayService"
                        )
                        action = "org.lineageos.gamekeys.intent.BOTH_CLOSE"
                    }

                    ContextCompat.startForegroundService(this@EventListenerService, svc)
                }

                GameKeyEventType.DOWN,
                GameKeyEventType.UP -> {
                    val viewModelState = Application.INSTANCE.viewModel.state.value
                    val gameKeySettings = when (event.id) {
                        0 -> viewModelState.currentAppSettings.upper
                        1 -> viewModelState.currentAppSettings.lower
                        else -> throw IllegalArgumentException("Unknown game key with index ${event.id}")
                    }

                    if (!gameKeySettings.enabled)
                        return

                    if (event.type == GameKeyEventType.UP) {
                        touchInjectorService.endTouch(touchInjectorClient, event.id)
                        return
                    }

                    val touchPosition = TouchPosition()
                    touchPosition.x = gameKeySettings.pos.x
                    touchPosition.y = gameKeySettings.pos.y

                    touchInjectorService.beginTouch(touchInjectorClient, event.id, touchPosition)
                }
            }
        }

        override fun getInterfaceVersion(): Int {
            return VERSION
        }

        override fun getInterfaceHash(): String {
            return HASH
        }
    }

    private val touchInjectorClient = object : ITouchInjectorClient.Stub() {
        override fun getSlotCount(): Int = 2
        override fun getInterfaceVersion(): Int = VERSION
        override fun getInterfaceHash(): String = HASH
    }

    private fun onTaskFocused(packageName: String) {
        (application as Application).viewModel.setPackageName(packageName)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        activityTaskManager.registerTaskStackListener(taskStackListener)
        touchInjectorService.registerClient(touchInjectorClient)
        gameKeysService.registerEventListener(gameKeyEventListener)
    }

    override fun onDestroy() {
        gameKeysService.unregisterEventListener(gameKeyEventListener)
        touchInjectorService.unregisterClient(touchInjectorClient)
        activityTaskManager.unregisterTaskStackListener(taskStackListener)

        super.onDestroy()
    }
}
