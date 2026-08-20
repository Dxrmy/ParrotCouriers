package com.parrotcouriers.model;

/**
 * Lifecycle states of a Parrot Courier.
 */
public enum CourierState {
    /**
     * Parrot has been registered/named, waiting for owner to load payload.
     */
    AWAITING_PAYLOAD,

    /**
     * Payload has been loaded, waiting for owner to set payment requirement.
     */
    AWAITING_PAYMENT,

    /**
     * Trade is locked and courier is flying/pathfinding to target destination.
     */
    IN_TRANSIT_TO_DESTINATION,

    /**
     * Arrived at destination, waiting for recipient to sneak and accept trade.
     */
    WAITING_FOR_RECIPIENT,

    /**
     * Trade completed, courier is flying back to original owner.
     */
    IN_TRANSIT_TO_OWNER,

    /**
     * Returned to owner, waiting for owner to collect payment.
     */
    WAITING_FOR_OWNER,

    /**
     * Finalized and reverted to normal tamed parrot.
     */
    COMPLETED
}
