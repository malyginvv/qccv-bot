package ru.mv.cv.quake.processor;

import ru.mv.cv.quake.image.ScanMatcher;
import ru.mv.cv.quake.model.DebugData;
import ru.mv.cv.quake.model.EnemyData;
import ru.mv.cv.quake.model.FrameData;
import ru.mv.cv.quake.model.GameState;

import java.util.Collection;

public class StateRecognizer {

    private final ScanMatcher scanMatcher;

    public StateRecognizer() {
        scanMatcher = new ScanMatcher();
    }

    /**
     * Analyzes frame data using image recognition and produces a snapshot of game state.
     *
     * @param frameData current frame
     * @return game state based on current frame only
     */
    public GameState recognize(FrameData frameData) {
        // template matching and cascade classifiers are too slow = 50-70 ms for 720p frame, so we use cheesy custom scanner
        DebugData debugData = new DebugData();
        Collection<EnemyData> enemies = scanMatcher.findEnemies(frameData, debugData);
        //TODO: recognize gameplay state, ammo, weapon etc. Can be done in separate threads
        return new GameState(enemies, debugData);
    }
}
