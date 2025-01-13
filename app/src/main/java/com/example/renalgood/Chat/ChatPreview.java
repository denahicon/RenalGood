package com.example.renalgood.Chat;

public class ChatPreview {
    private String chatId;
    private String participantId;
    private String participantName;
    private String lastMessage;
    private long lastMessageTime;
    private String profilePicUrl;
    private int unreadCount;
    private boolean isOnline;

    public ChatPreview(String chatId, String participantId, String participantName,
                       String lastMessage, long lastMessageTime, String profilePicUrl) {
        this.chatId = chatId;
        this.participantId = participantId;
        this.participantName = participantName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.profilePicUrl = profilePicUrl;
        this.unreadCount = 0;
        this.isOnline = false;
    }

    public String getChatId() { return chatId; }
    public String getParticipantId() { return participantId; }
    public String getParticipantName() { return participantName; }
    public String getLastMessage() { return lastMessage; }
    public long getLastMessageTime() { return lastMessageTime; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}