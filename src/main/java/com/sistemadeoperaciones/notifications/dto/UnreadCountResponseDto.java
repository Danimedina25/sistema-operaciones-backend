package com.sistemadeoperaciones.notifications.dto;

public class UnreadCountResponseDto {

    private long unreadCount;

    public UnreadCountResponseDto() {
    }

    public UnreadCountResponseDto(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}