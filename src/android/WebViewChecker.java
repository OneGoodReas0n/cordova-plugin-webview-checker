package com.nonameprovided.cordova.WebViewChecker;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class echoes a string called from JavaScript.
 */
public class WebViewChecker extends CordovaPlugin {

  com.nonameprovided.cordova.WebViewChecker.WebViewDialogFragment dialog;
  int MINIMAL_STABLE_VERSION = 60;
  boolean isDialogVisible = false;

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    if (isDialogVisible) {
      closeWebViewWarning();
    }
  }

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    MINIMAL_STABLE_VERSION = preferences.getInteger("WebViewMinVersion", MINIMAL_STABLE_VERSION);
    dialog = new com.nonameprovided.cordova.WebViewChecker.WebViewDialogFragment();
    int currentWebViewVersion = getCurrentWebViewVersion(cordova);
    if (currentWebViewVersion < MINIMAL_STABLE_VERSION && !isDialogVisible) {
      openWebViewWarning();
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("isAppEnabled")) {
      String packageName = args.getString(0);
      this.isAppEnabled(packageName, callbackContext);

      return true;
    }

    if (action.equals("getAppPackageInfo")) {
      String packageName = args.getString(0);
      this.getAppPackageInfo(packageName, callbackContext);

      return true;
    }

    if (action.equals("openGooglePlayPage")) {
      String packageName = args.getString(0);
      this.openGooglePlayPage(packageName, callbackContext);

      return true;
    }

    if (action.equals("getCurrentWebViewPackageInfo")) {
      this.getCurrentWebViewPackageInfo(callbackContext);

      return true;
    }

    callbackContext.error("Command not found. (" + action.toString() + ")");
    return false;
  }

  public int getCurrentWebViewVersion(CordovaInterface cordova) {
    PackageInfo info = this.getCurrentWebViewPackage();
    if (info != null) {
      return parseVersionFromPackage(info);
    }
    return getWebViewVersionFromUserAgent(cordova);
  }

  public PackageInfo getCurrentWebViewPackage() {
    PackageInfo pInfo = null;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      System.out.println("getCurrentWebViewPackageInfo for O+");
      pInfo = WebView.getCurrentWebViewPackage();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        System.out.println("getCurrentWebViewPackageInfo for M+");
        Class webViewFactory = Class.forName("android.webkit.WebViewFactory");
        Method method = webViewFactory.getMethod("getLoadedPackageInfo");
        pInfo = (PackageInfo) method.invoke(null);
      } catch (Exception e) {
        System.out.println("getCurrentWebViewPackageInfo for M+ ex=" + e);
      }
      if (pInfo == null) {
        try {
          System.out.println("getCurrentWebViewPackageInfo for M+ (2)");
          Class webViewFactory = Class.forName("com.google.android.webview.WebViewFactory");
          Method method = webViewFactory.getMethod("getLoadedPackageInfo");
          pInfo = (PackageInfo) method.invoke(null);
          if (pInfo == null) {
            try {
              System.out.println("getCurrentWebViewPackageInfo for M+ (3)");
              Class anotherWebViewFactory = Class.forName("com.android.webview.WebViewFactory");
              Method anotherMethod = anotherWebViewFactory.getMethod("getLoadedPackageInfo");
              pInfo = (PackageInfo) anotherMethod.invoke(null);
            } catch (Exception e2) {
              System.out.println("getCurrentWebViewPackageInfo for M+ (3) ex=" + e2);
            }
          }
        } catch (Exception e2) {
          System.out.println("getCurrentWebViewPackageInfo for M+ (2) ex=" + e2);
        }
      }
    } else {
      System.out.println("No info before Lollipop");
    }
    if (pInfo != null) {
      System.out.println("getCurrentWebViewPackageInfo pInfo set");
    }
    return pInfo;
  }

  public void getCurrentWebViewPackageInfo(CallbackContext callbackContext) throws org.json.JSONException {
    PackageInfo pInfo = null;
    JSONObject responseObject = new JSONObject();

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        /* Starting with Android O (API 26) they added a new method specific for this */
        pInfo = WebView.getCurrentWebViewPackage();
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        /**
         * With Android Lollipop (API 21) they started to update the WebView
         * as a separate APK with the PlayStore and they added the
         * getLoadedPackageInfo() method to the WebViewFactory class and this
         * should handle the Android 7.0 behaviour changes too.
         */
        Class webViewFactory = Class.forName("android.webkit.WebViewFactory");
        Method method = webViewFactory.getMethod("getLoadedPackageInfo");
        pInfo = (PackageInfo) method.invoke(null);
      } else {
        /* Before Lollipop the WebView was bundled with the OS. */
        this.getAppPackageInfo("com.google.android.webview", callbackContext);

        /* The getAppPackageInfo function resolves the callbackContext
         * and returns the same response, so we need to return here.
         */
        return;
      }

      responseObject.put("packageName", pInfo.packageName);
      responseObject.put("versionName", pInfo.versionName);
      responseObject.put("versionCode", pInfo.versionCode);

      callbackContext.success(responseObject);
    } catch (Exception e) {
      callbackContext.error("Cannot determine current WebView engine. (" + e.getMessage() + ")");
      return;
    }
  }

  /**
   * Returns partial package information about the specified package.
   */
  public void getAppPackageInfo(String packagename, CallbackContext callbackContext) throws org.json.JSONException {
    PackageInfo pInfo = null;
    JSONObject responseObject = new JSONObject();
    PackageManager packageManager = this.cordova.getActivity().getPackageManager();

    try {
      pInfo = packageManager.getPackageInfo(packagename, 0);
    } catch (PackageManager.NameNotFoundException e) {
      callbackContext.error("Package not found");
    }

    responseObject.put("packageName", pInfo.packageName);
    responseObject.put("versionName", pInfo.versionName);
    responseObject.put("versionCode", pInfo.versionCode);

    callbackContext.success(responseObject);
  }

  public void isAppEnabled(String packagename, CallbackContext callbackContext) {
    PackageManager packageManager = this.cordova.getActivity().getPackageManager();

    try {
      Boolean enabled = packageManager.getApplicationInfo(packagename, 0).enabled;
      callbackContext.success(enabled.toString());
    } catch (PackageManager.NameNotFoundException e) {
      callbackContext.error("Package not found");
    }
  }

  private void openGooglePlayPage(String packagename, CallbackContext callbackContext) throws android.content.ActivityNotFoundException {
    Context context = this.cordova.getActivity().getApplicationContext();
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packagename));

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    try {
      context.startActivity(intent);
      callbackContext.success();
    } catch (Exception e) {
      callbackContext.error("Cannot open Google Play. (" + e.getMessage() + ")");
    }
  }

  private void openWebViewWarning() {
    if (dialog != null) {
      dialog.show(cordova.getActivity().getSupportFragmentManager(), "Alert");
      isDialogVisible = true;
    }
  }

  private void closeWebViewWarning() {
    if (dialog != null) {
      dialog.dismiss();
      isDialogVisible = false;
    }
  }

  private int parseVersionFromPackage(PackageInfo pInfo) {
    String version = pInfo.versionName;
    return Integer.parseInt(version.substring(0, version.indexOf('.')));
  }

  private int getWebViewVersionFromUserAgent(CordovaInterface cordova) {
    int result = 0;
    WebView webView = new WebView(cordova.getContext());
    String webViewInfo = webView.getSettings().getUserAgentString();
    Pattern pattern = Pattern.compile("Chrome\\/([0-9]+).");
    Matcher matcher = pattern.matcher(webViewInfo);
    if (matcher.find()) {
      result = Integer.parseInt(matcher.group(1));
    }
    return result;
  }
}
