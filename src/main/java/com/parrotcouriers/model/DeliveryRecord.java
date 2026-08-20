package com.parrotcouriers.model;

/**
 * Record representing a completed delivery entry in the history ledger.
 */
public record DeliveryRecord(
        long timestamp,
        String senderName,
        String recipientName,
        String payloadSummary,
        String paymentSummary,
        boolean wasGift
) {
    public String getFormattedDate() {
        return new java.text.SimpleDateFormat("MMM dd, HH:mm").format(new java.util.Date(timestamp));
    }
}
