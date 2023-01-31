package com.example.flutter_braintree;

import android.app.Activity;
import android.content.Intent;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.DropInClient;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

public class FlutterBraintreePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, ActivityResultListener {
  private static final int CUSTOM_ACTIVITY_REQUEST_CODE = 0x420;

  private Activity activity;
  private Result activeResult;

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    final MethodChannel channel = new MethodChannel(binding.getBinaryMessenger(), "flutter_braintree.custom");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding binding) {
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (activeResult != null) {
      result.error("already_running", "Cannot launch another custom activity while one is already running.", null);
      return;
    }
    activeResult = result;

    if (call.method.equals("tokenizeCreditCard")) {
      Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
      intent.putExtra("type", "tokenizeCreditCard");
      intent.putExtra("authorization", (String) call.argument("authorization"));
      assert (call.argument("request") instanceof Map);
      Map request = (Map) call.argument("request");
      intent.putExtra("cardNumber", (String) request.get("cardNumber"));
      intent.putExtra("expirationMonth", (String) request.get("expirationMonth"));
      intent.putExtra("expirationYear", (String) request.get("expirationYear"));
      intent.putExtra("cvv", (String) request.get("cvv"));
      intent.putExtra("cardholderName", (String) request.get("cardholderName"));
      activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
    } else if (call.method.equals("requestPaypalNonce")) {
      Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
      intent.putExtra("type", "requestPaypalNonce");
      intent.putExtra("authorization", (String) call.argument("authorization"));
      assert (call.argument("request") instanceof Map);
      Map request = (Map) call.argument("request");
      intent.putExtra("amount", (String) request.get("amount"));
      intent.putExtra("currencyCode", (String) request.get("currencyCode"));
      intent.putExtra("displayName", (String) request.get("displayName"));
      intent.putExtra("payPalPaymentIntent", (String) request.get("payPalPaymentIntent"));
      intent.putExtra("payPalPaymentUserAction", (String) request.get("payPalPaymentUserAction"));
      intent.putExtra("billingAgreementDescription", (String) request.get("billingAgreementDescription"));
      activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
    } else if (call.method.equals("launchDropIn")) {
      Intent intent = new Intent(activity, BraintreeDropIn.class);
      intent.putExtra("clientToken", (String) call.argument("clientToken"));
      intent.putExtra("tokenizationKey", (String) call.argument("tokenizationKey"));

      intent.putExtra("googlePaymentRequest", (HashMap<String, Object>) call.argument("googlePaymentRequest"));
      intent.putExtra("paypalRequest", (HashMap<String, Object>) call.argument("paypalRequest"));
      intent.putExtra("venmoRequest", (HashMap<String, Object>) call.argument("venmoRequest"));

      intent.putExtra("maskCardNumber", (Boolean) call.argument("maskCardNumber"));
      intent.putExtra("maskSecurityCode", (Boolean) call.argument("maskSecurityCode"));

      intent.putExtra("vaultManagerEnabled", (Boolean) call.argument("vaultManagerEnabled"));
      intent.putExtra("cardDisabled", (Boolean) call.argument("cardDisabled"));
      intent.putExtra("venmoDisabled", (Boolean) call.argument("venmoDisabled"));
      intent.putExtra("paypalDisabled", (Boolean) call.argument("paypalDisabled"));
      intent.putExtra("googlePayDisabled", (Boolean) call.argument("googlePayDisabled"));
      activity.startActivityForResult(intent, 0x440);
    } else {
      result.notImplemented();
      activeResult = null;
    }
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (activeResult == null)
      return false;

    switch (requestCode) {
      case CUSTOM_ACTIVITY_REQUEST_CODE:
        if (resultCode == Activity.RESULT_OK) {
          String type = data.getStringExtra("type");
          if (type.equals("paymentMethodNonce")) {
            activeResult.success(data.getSerializableExtra("paymentMethodNonce"));
          } else {
            Exception error = new Exception("Invalid activity result type.");
            activeResult.error("error", error.getMessage(), null);
          }
        } else if (resultCode == Activity.RESULT_CANCELED) {
          activeResult.success(null);
        } else {
          Exception error = (Exception) data.getSerializableExtra("error");
          activeResult.error("error", error.getMessage(), null);
        }
        activeResult = null;
        return true;
      default:
        return false;
    }
  }
}
