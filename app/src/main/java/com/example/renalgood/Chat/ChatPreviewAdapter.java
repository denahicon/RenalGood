package com.example.renalgood.Chat;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatPreviewAdapter extends RecyclerView.Adapter<ChatPreviewAdapter.ViewHolder> {
    private List<ChatPreview> chats;
    private final OnChatClickListener listener;
    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat dateFormat;

    public interface OnChatClickListener {
        void onChatClick(ChatPreview chat);
    }

    public ChatPreviewAdapter(OnChatClickListener listener) {
        this.chats = new ArrayList<>();
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatPreview chat = chats.get(position);
        holder.bind(chat, listener);
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public void updateChats(List<ChatPreview> newChats) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ChatPreviewDiffCallback(chats, newChats));
        this.chats = new ArrayList<>(newChats);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivProfilePic;
        private final TextView tvContactName;
        private final TextView tvLastMessage;
        private final TextView tvTimestamp;

        ViewHolder(View view) {
            super(view);
            ivProfilePic = view.findViewById(R.id.ivProfilePic);
            tvContactName = view.findViewById(R.id.tvContactName);
            tvLastMessage = view.findViewById(R.id.tvLastMessage);
            tvTimestamp = view.findViewById(R.id.tvTimestamp);
        }

        void bind(ChatPreview chat, OnChatClickListener listener) {
            tvContactName.setText(chat.getParticipantName());
            tvLastMessage.setText(chat.getLastMessage());

            // Cargar imagen de perfil
            if (chat.getProfilePicUrl() != null && !chat.getProfilePicUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(chat.getProfilePicUrl())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(ivProfilePic);
            } else {
                ivProfilePic.setImageResource(R.drawable.default_profile);
            }

            // Configurar timestamp
            tvTimestamp.setText(formatTime(chat.getLastMessageTime()));

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(chat);
                }
            });
        }

        private String formatTime(long timestamp) {
            if (timestamp == 0) return "";

            Date messageDate = new Date(timestamp);
            Date now = new Date();

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            Date weekAgo = calendar.getTime();

            if (isSameDay(messageDate, now)) {
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate);
            } else if (messageDate.after(weekAgo)) {
                return new SimpleDateFormat("EEEE", Locale.getDefault()).format(messageDate);
            } else {
                return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate);
            }
        }

        private boolean isSameDay(Date date1, Date date2) {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(date1);
            cal2.setTime(date2);
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
    }
}