package com.example.renalgood.admin;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class EmailUtils {
    public static void enviarCorreo(Context context, String correoDestinatario, String asunto, String mensaje) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + correoDestinatario));
        intent.putExtra(Intent.EXTRA_SUBJECT, asunto);
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);

        try {
            context.startActivity(Intent.createChooser(intent, "Enviar correo"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No hay aplicaciones de correo instaladas", Toast.LENGTH_SHORT).show();
        }
    }

    public static void enviarCorreoSolicitud(Context context, String correoDestinatario, boolean aprobada) {
        String asunto = aprobada ? "¡Bienvenido a RenalGood - Cuenta Aprobada!" : "Solicitud Rechazada - RenalGood";
        String mensaje = aprobada ?
                "Tu cuenta ha sido aprobada. Recibirás un correo adicional para establecer tu contraseña.\n\n" +
                        "Una vez que hayas establecido tu contraseña, podrás iniciar sesión en la aplicación.\n\n" +
                        "Gracias por unirte a RenalGood." :
                "Lo sentimos, tu solicitud ha sido rechazada. Puedes intentar registrarte nuevamente corrigiendo la información proporcionada.";

        enviarCorreo(context, correoDestinatario, asunto, mensaje);
    }
}