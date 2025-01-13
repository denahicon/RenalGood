package com.example.renalgood.Chat;

import androidx.recyclerview.widget.DiffUtil;
import java.util.List;

public class MessageDiffCallback extends DiffUtil.Callback {
    private List<ChatMessage> oldMessages;
    private List<ChatMessage> newMessages;

    public MessageDiffCallback(List<ChatMessage> oldMessages, List<ChatMessage> newMessages) {
        this.oldMessages = oldMessages;
        this.newMessages = newMessages;
    }

    @Override
    public int getOldListSize() { return oldMessages.size(); }

    @Override
    public int getNewListSize() { return newMessages.size(); }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldMessages.get(oldItemPosition).getMessageId()
                .equals(newMessages.get(newItemPosition).getMessageId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ChatMessage oldMessage = oldMessages.get(oldItemPosition);
        ChatMessage newMessage = newMessages.get(newItemPosition);
        return oldMessage.getMessage().equals(newMessage.getMessage())
                && oldMessage.getTimestamp() == newMessage.getTimestamp();
    }
}