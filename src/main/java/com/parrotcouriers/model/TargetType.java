package com.parrotcouriers.model;

/**
 * Type of target destination for a courier parrot.
 */
public enum TargetType {
    /**
     * Target is a specific player by username.
     */
    PLAYER,

    /**
     * Target is a specific coordinate location (X, Y, Z).
     */
    COORDINATES
}
