package com.example.renalgood.Chat;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ChatViewHolder extends RecyclerView.ViewHolder {
    protected TextView messageText;
    protected TextView timeText;

    public ChatViewHolder(@NonNull View itemView) {
        super(itemView);
        initializeViews(itemView);
    }

    protected abstract void initializeViews(View itemView);
    public abstract void bind(ChatMessage message);
}