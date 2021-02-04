package ru.mv.cv.quake.model;

import java.util.Collection;

/**
 * Current state of the game.
 * Should include gameplay state (menu/loading/alive/dead), enemy positions, ammo, current weapon, health, armor,
 * spatial position etc.
 * TODO: everything from above
 */
public class GameState {

    public final Collection<EnemyData> enemyData;

    public GameState(Collection<EnemyData> enemyData) {
        this.enemyData = enemyData;
    }
}
