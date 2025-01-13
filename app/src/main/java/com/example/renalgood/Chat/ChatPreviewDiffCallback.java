package com.example.renalgood.Chat;

import androidx.recyclerview.widget.DiffUtil;
import java.util.List;

public class ChatPreviewDiffCallback extends DiffUtil.Callback {
    private final List<ChatPreview> oldList;
    private final List<ChatPreview> newList;

    public ChatPreviewDiffCallback(List<ChatPreview> oldList, List<ChatPreview> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getChatId()
                .equals(newList.get(newItemPosition).getChatId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ChatPreview oldItem = oldList.get(oldItemPosition);
        ChatPreview newItem = newList.get(newItemPosition);

        return oldItem.getLastMessage().equals(newItem.getLastMessage())
                && oldItem.getLastMessageTime() == newItem.getLastMessageTime()
                && oldItem.getUnreadCount() == newItem.getUnreadCount()
                && oldItem.isOnline() == newItem.isOnline();
    }
}