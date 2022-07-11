package com.nonameprovided.cordova.WebViewChecker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

public class WebViewDialogFragment extends DialogFragment {
  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    TextView message = new TextView(getActivity());
    message.setMovementMethod(LinkMovementMethod.getInstance());
    String webViewHtml = "Your WebView version is too small to open the application. Update <a href=\"https://play.google.com/store/apps/details?id=com.google.android.webview\">WebView</a> or <a href=\"https://play.google.com/store/apps/details?id=com.android.chrome\">Chrome</a> to run the application";
    message.setText(HtmlCompat.fromHtml(webViewHtml, HtmlCompat.FROM_HTML_MODE_LEGACY));
    float dpi = getActivity().getResources().getDisplayMetrics().density;
    int xPadding = (int) (24 * dpi);
    int yPadding = (int) (8 * dpi);
    message.setPadding(xPadding, yPadding, xPadding, 0);
    builder.setView(message)
      .setTitle("WebView version problem")
      .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          getActivity().finishAffinity();
        }
      });
    final Dialog dialog = builder.create();
    dialog.setCanceledOnTouchOutside(false);
    dialog.setCancelable(false);
    return dialog;
  }
}
