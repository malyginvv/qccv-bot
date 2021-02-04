package ru.mv.cv.quake.processor;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ForkJoinPool;

public class GameCommander {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CLASS_NAME = "Lovecraft";
    private static final String WINDOW_NAME = "Quake Champions CLIENT 0.1.DEV.51552/53841";

    private static final int MOUSEEVENTF_MOVE = 0x0001;
    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;
    private static final float SPEED_DUMPER = 1.5f;

    private final User32 user32;
    private final WinDef.HWND windowHandle;

    public GameCommander() {
        user32 = User32.INSTANCE;
        windowHandle = user32.FindWindow(CLASS_NAME, WINDOW_NAME);
        if (windowHandle == null) {
            throw new RuntimeException("QC window not found");
        }
    }

    public void sendClick() {
        if (isTargetWindowActive()) {
            sendEvent(createMouseEvent(MOUSEEVENTF_LEFTDOWN, 0, 0));
            ForkJoinPool.commonPool().execute(() -> sendEvent(createMouseEvent(MOUSEEVENTF_LEFTUP, 0, 0)));
        }
    }

    public void moveCursor(int deltaX, int deltaY) {
        if (isTargetWindowActive()) {
            sendEvent(createMouseEvent(MOUSEEVENTF_MOVE, Math.round(deltaX / SPEED_DUMPER), Math.round(deltaY / SPEED_DUMPER)));
        }
    }

    private boolean isTargetWindowActive() {
        return windowHandle.equals(user32.GetForegroundWindow());
    }

    private WinUser.INPUT createMouseEvent(int eventId, int x, int y) {
        var input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input = new WinUser.INPUT.INPUT_UNION();
        input.input.setType("mi");
        input.input.mi.dx = new WinDef.LONG(x);
        input.input.mi.dy = new WinDef.LONG(y);
        input.input.mi.mouseData = new WinDef.DWORD(0);
        input.input.mi.dwFlags = new WinDef.DWORD(eventId);
        input.input.mi.time = new WinDef.DWORD(0);
        input.input.mi.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        return input;
    }

    private void sendEvent(WinUser.INPUT input) {
        try {
            user32.SendInput(new WinDef.DWORD(1), new WinUser.INPUT[]{input}, input.size());
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }
}
