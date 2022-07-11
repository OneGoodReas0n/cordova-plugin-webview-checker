package com.nonameprovided.cordova.WebViewChecker;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

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

/**
 * This class echoes a string called from JavaScript.
 */
public class WebViewChecker extends CordovaPlugin {

  WebViewDialogFragment dialog;
  int MINIMAL_STABLE_VERSION = 72;
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
    dialog = new WebViewDialogFragment();
    int currentWebViewVersion = getCurrentWebViewVersion(cordova);
    if (currentWebViewVersion < MINIMAL_STABLE_VERSION && !isDialogVisible) {
      openWebViewWarning();
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("getCurrentWebViewPackageInfo")) {
      this.getCurrentWebViewPackageInfo(callbackContext);
      return true;
    }

    callbackContext.error("Command not found. (" + action.toString() + ")");
    return false;
  }

  public void getCurrentWebViewVersion(CordovaInterface cordova, CallbackContext callback) {
    PackageInfo info = this.getCurrentWebViewPackageInfo();
    if (info != null) {
      callback.success(parseVersionFromPackage(info));
    }
    callback.success(getWebViewVersionFromUserAgent(cordova));
  }

  /**
   * Returns information about the currently selected WebView engine.
   */
  public PackageInfo getCurrentWebViewPackageInfo() throws org.json.JSONException {
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
        } catch (Exception e2) {
          System.out.println("getCurrentWebViewPackageInfo for M+ (2) ex=" + e2);
        }
      }
      if (pInfo == null) {
        try {
          System.out.println("getCurrentWebViewPackageInfo for M+ (3)");
          Class webViewFactory = Class.forName("com.android.webview.WebViewFactory");
          Method method = webViewFactory.getMethod("getLoadedPackageInfo");
          pInfo = (PackageInfo) method.invoke(null);
        } catch (Exception e2) {
          System.out.println("getCurrentWebViewPackageInfo for M+ (3) ex=" + e2);
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
