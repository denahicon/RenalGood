package com.example.renalgood.Chat;

import android.icu.text.SimpleDateFormat;
import android.view.View;
import androidx.annotation.NonNull;
import com.example.renalgood.R;
import java.util.Date;
import java.util.Locale;

public class ReceivedMessageViewHolder extends ChatViewHolder {
    public ReceivedMessageViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    protected void initializeViews(View itemView) {
        messageText = itemView.findViewById(R.id.texto_mensaje_recibido);
        timeText = itemView.findViewById(R.id.texto_hora_recibido);
    }

    @Override
    public void bind(ChatMessage message) {
        messageText.setText(message.getMensaje());  // Usando getMensaje() en lugar de getMessage()
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeText.setText(sdf.format(new Date(message.getTimestamp())));
    }
}