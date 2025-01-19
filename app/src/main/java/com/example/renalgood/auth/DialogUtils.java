package com.example.renalgood.auth;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogUtils {
    public static ProgressDialog showProgressDialog(Context context, String message) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
        return progressDialog;
    }

    public static void dismissProgressSafely(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception ignored) {}
        }
    }

    public static void showSuccessDialog(Context context, String title, String message,
                                         DialogInterface.OnClickListener onClickListener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Entendido", onClickListener)
                .setCancelable(false)
                .show();
    }

    public static void showErrorDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Intentar de nuevo", null)
                .show();
    }
}