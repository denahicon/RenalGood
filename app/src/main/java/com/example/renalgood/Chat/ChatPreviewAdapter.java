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
                .inflate(R.layout.item_mensaje, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatPreview chat = chats.get(position);

        // Configurar nombre y foto de perfil
        holder.tvName.setText(chat.getParticipantName());
        if (chat.getProfilePicUrl() != null && !chat.getProfilePicUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(chat.getProfilePicUrl())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.ivProfilePic);
        }

        // Configurar último mensaje
        holder.tvLastMessage.setText(chat.getLastMessage());

        // Configurar tiempo
        holder.tvTime.setText(formatTime(chat.getLastMessageTime()));

        // Si hay mensajes no leídos, mostrar contador
        if (chat.getUnreadCount() > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        // Mostrar indicador "en línea" si corresponde
        holder.ivOnlineStatus.setVisibility(chat.isOnline() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "";

        Date messageDate = new Date(timestamp);
        Date now = new Date();

        // Si es hoy, mostrar hora
        if (isSameDay(messageDate, now)) {
            return timeFormat.format(messageDate);
        }

        // Si es ayer
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(messageDate, calendar.getTime())) {
            return "Ayer";
        }

        // Si es de esta semana, mostrar día
        if (isThisWeek(messageDate)) {
            return new SimpleDateFormat("EEEE", Locale.getDefault())
                    .format(messageDate);
        }

        // Si es más antiguo, mostrar fecha
        return dateFormat.format(messageDate);
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isThisWeek(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTime(date);
        return then.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                && then.get(Calendar.YEAR) == now.get(Calendar.YEAR);
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
        CircleImageView ivProfilePic;
        TextView tvName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadCount;
        View ivOnlineStatus;

        ViewHolder(View view) {
            super(view);
            ivProfilePic = view.findViewById(R.id.ivProfilePic);
            tvName = view.findViewById(R.id.tvName);
            tvLastMessage = view.findViewById(R.id.tvLastMessage);
            tvTime = view.findViewById(R.id.tvTime);
            tvUnreadCount = view.findViewById(R.id.tvUnreadCount);
            ivOnlineStatus = view.findViewById(R.id.ivOnlineStatus);
        }
    }
}