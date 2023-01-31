package com.example.flutter_braintree;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.braintreepayments.api.DropInClient;
import com.braintreepayments.api.DropInListener;
import com.braintreepayments.api.DropInRequest;
import com.braintreepayments.api.DropInResult;
import com.braintreepayments.api.GooglePayRequest;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.UserCanceledException;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import java.util.HashMap;

public class BraintreeDropIn extends AppCompatActivity implements DropInListener {

    private DropInClient dropInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
            String authorization = intent.getStringExtra("clientToken") != null ? intent.getStringExtra("clientToken")
                    : intent.getStringExtra("tokenizationKey");
            dropInClient = new DropInClient(new Fragment(), authorization);
            dropInClient.setListener(this);
            launchDropIn();
        } catch (Exception e) {
            onException(e);
        }
    }

    private void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void onException(Exception e) {
        Intent result = new Intent();
        result.putExtra("error", e);
        setResult(2, result);
        finish();
    }

    private void onSuccess(PaymentMethodNonce nonce) {
        HashMap<String, Object> nonceMap = new HashMap<String, Object>();

        nonceMap.put("nonce", nonce.getString());
        nonceMap.put("isDefault", nonce.isDefault());

        if (nonce instanceof PayPalAccountNonce) {
            nonceMap.put("paypalPayerId", ((PayPalAccountNonce) nonce).getPayerId());
        }

        Intent result = new Intent();
        result.putExtra("type", "paymentMethodNonce");
        result.putExtra("paymentMethodNonce", nonceMap);
        setResult(RESULT_OK, result);
        finish();
    }

    private void launchDropIn() {
        Intent intent = getIntent();
        DropInRequest dropInRequest = new DropInRequest();

        dropInRequest.setMaskCardNumber(intent.getBooleanExtra("maskCardNumber", true));
        dropInRequest.setMaskSecurityCode(intent.getBooleanExtra("maskSecurityCode", true));

        dropInRequest.setVaultManagerEnabled(intent.getBooleanExtra("vaultManagerEnabled", true));
        dropInRequest.setCardDisabled(intent.getBooleanExtra("cardDisabled", false));
        dropInRequest.setVenmoDisabled(intent.getBooleanExtra("venmoDisabled", false));
        dropInRequest.setPayPalDisabled(intent.getBooleanExtra("paypalDisabled", false));
        dropInRequest.setGooglePayDisabled(intent.getBooleanExtra("googlePayDisabled", false));

        dropInRequest.setGooglePayRequest(
                readGooglePaymentParameters(
                        (HashMap<String, Object>) intent.getSerializableExtra("googlePaymentRequest")));
        dropInRequest.setPayPalRequest(readPayPalParameters(
                (HashMap<String, Object>) intent.getSerializableExtra("paypalRequest")));

        dropInClient.launchDropIn(dropInRequest);
    }

    @Override
    public void onDropInSuccess(@NonNull DropInResult dropInResult) {
        String paymentMethodNonce = dropInResult.getPaymentMethodNonce().getString();
        // use the result to update your UI and send the payment method nonce to your
        // server
    }

    @Override
    public void onDropInFailure(@NonNull Exception error) {

        if (error instanceof UserCanceledException) {
            // the user canceled
        } else {
            // handle error
        }
    }

    private GooglePayRequest readGooglePaymentParameters(HashMap<String, Object> request) {
        GooglePayRequest googlePayRequest = new GooglePayRequest();

        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice((String) request.get("totalPrice"))
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode((String) request.get("currencyCode"))
                .build());
        googlePayRequest.setBillingAddressRequired((Boolean) request.get("billingAddressRequired"));
        return googlePayRequest;
    }

    private PayPalRequest readPayPalParameters(HashMap<String, Object> request) {
        if (request.get("amount") == null) {
            PayPalVaultRequest vaultRequest = new PayPalVaultRequest();
            vaultRequest.setDisplayName((String) request.get("displayName"));
            vaultRequest.setBillingAgreementDescription("Your agreement description");
            return vaultRequest;
        } else {
            PayPalCheckoutRequest checkoutRequest = new PayPalCheckoutRequest((String) request.get("amount"));
            checkoutRequest.setCurrencyCode((String) request.get("currencyCode"));
            checkoutRequest.setDisplayName((String) request.get("displayName"));

            switch ((String) request.get("payPalPaymentIntent")) {
                case PayPalPaymentIntent.ORDER:
                    checkoutRequest.setIntent(PayPalPaymentIntent.ORDER);
                    break;
                case PayPalPaymentIntent.SALE:
                    checkoutRequest.setIntent(PayPalPaymentIntent.SALE);
                    break;
                default:
                    checkoutRequest.setIntent(PayPalPaymentIntent.AUTHORIZE);
            }

            checkoutRequest.setUserAction(
                    PayPalCheckoutRequest.USER_ACTION_COMMIT.equals(request.get("payPalPaymentUserAction"))
                            ? PayPalCheckoutRequest.USER_ACTION_COMMIT
                            : PayPalCheckoutRequest.USER_ACTION_DEFAULT);

            return checkoutRequest;
        }
    }
}
