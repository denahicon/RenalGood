/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const functions = require('firebase-functions');
const nodemailer = require('nodemailer');

// Obtener las credenciales desde las variables de entorno
const gmailEmail = functions.config().gmail.email;
const gmailPassword = functions.config().gmail.password;
const adminEmail = functions.config().admin.email;

// Configurar el transportador de Nodemailer
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: gmailEmail,
    pass: gmailPassword // Debe ser una contraseña de aplicación de Gmail
  }
});

exports.enviarCorreoAdmin = functions.firestore
  .document('notificaciones_admin/{docId}')
  .onCreate(async (snap, context) => {
    try {
      const notificacion = snap.data();
      
      // Crear un mensaje más informativo
      const mensaje = `
        Nueva solicitud de verificación de nutriólogo
        
        Nombre: ${notificacion.nombre}
        ID: ${notificacion.nutriologoId}
        Mensaje: ${notificacion.mensaje}
        
        Por favor, revisa la solicitud en el panel de administración.
      `;

      const mailOptions = {
        from: gmailEmail,
        to: adminEmail,
        subject: `Nueva solicitud de verificación: ${notificacion.nombre}`,
        text: mensaje
      };

      // Enviar el correo y esperar la respuesta
      const info = await transporter.sendMail(mailOptions);
      console.log('Correo enviado exitosamente:', info.messageId);
      
      // Actualizar el documento para marcar que se envió el correo
      await snap.ref.update({
        correoEnviado: true,
        fechaEnvioCorreo: admin.firestore.FieldValue.serverTimestamp()
      });

      return { success: true, messageId: info.messageId };

    } catch (error) {
      console.error('Error al enviar el correo:', error);
      
      // Registrar el error en el documento
      await snap.ref.update({
        error: error.message,
        fechaError: admin.firestore.FieldValue.serverTimestamp()
      });

      throw new functions.https.HttpsError('internal', 'Error al enviar el correo', error);
    }
  });


// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
