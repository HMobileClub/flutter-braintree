package com.example.flutter_braintree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalListener;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.UserCanceledException;

import java.util.HashMap;

public class FlutterBraintreeCustom extends AppCompatActivity implements PayPalListener {
    private BraintreeClient braintreeClient;
    private PayPalClient payPalClient;
    private CardClient cardClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
            String returnUrlScheme = getApplicationContext().getPackageName() + ".paypal";
            braintreeClient = new BraintreeClient(this, intent.getStringExtra("authorization"), returnUrlScheme);
            String type = intent.getStringExtra("type");
            if (type.equals("tokenizeCreditCard")) {
                cardClient = new CardClient(braintreeClient);
                tokenizeCreditCard();
            } else if (type.equals("requestPaypalNonce")) {
                payPalClient = new PayPalClient(this, braintreeClient);
                payPalClient.setListener(this);
                requestPaypalNonce();
            } else {
                throw new Exception("Invalid request type: " + type);
            }
        } catch (Exception e) {
            onException(e);
        }
    }

    private void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void onException(Exception e) {
        if (e instanceof UserCanceledException) {
            onCancel();
        } else {
            Intent result = new Intent();
            result.putExtra("error", e);
            setResult(2, result);
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);
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

    protected void tokenizeCreditCard() {
        Intent intent = getIntent();
        Card card = new Card();
        card.setNumber(intent.getStringExtra("cardNumber"));
        card.setExpirationMonth(intent.getStringExtra("expirationMonth"));
        card.setExpirationYear(intent.getStringExtra("expirationYear"));
        card.setCvv(intent.getStringExtra("cvv"));
        card.setShouldValidate(false);
        card.setCardholderName(intent.getStringExtra("cardholderName"));

        cardClient.tokenize(card, (cardNonce, error) -> {
            if (cardNonce != null) {
                onSuccess(cardNonce);
            } else {
                onException(error);// handle error
            }
        });
    }

    private void payPalVaultFlow() {
        Intent intent = getIntent();
        PayPalVaultRequest request = new PayPalVaultRequest();
        request.setDisplayName(intent.getStringExtra("displayName"));
        request.setBillingAgreementDescription("Your agreement description");
        payPalClient.tokenizePayPalAccount(this, request);
    }

    private void payPalCheckoutFlow() {
        Intent intent = getIntent();
        String paypalIntent;

        switch (intent.getStringExtra("payPalPaymentIntent")) {
            case PayPalPaymentIntent.ORDER:
                paypalIntent = PayPalPaymentIntent.ORDER;
                break;
            case PayPalPaymentIntent.SALE:
                paypalIntent = PayPalPaymentIntent.SALE;
                break;
            default:
                paypalIntent = PayPalPaymentIntent.AUTHORIZE;
        }

        String payPalUserAction = PayPalCheckoutRequest.USER_ACTION_DEFAULT;
        if (PayPalCheckoutRequest.USER_ACTION_COMMIT.equals(intent.getStringExtra("payPalPaymentUserAction"))) {
            payPalUserAction = PayPalCheckoutRequest.USER_ACTION_COMMIT;
        }

        PayPalCheckoutRequest request = new PayPalCheckoutRequest(intent.getStringExtra("amount"));
        request.setCurrencyCode(intent.getStringExtra("currencyCode"));
        request.setIntent(paypalIntent);
        request.setUserAction(payPalUserAction);
        request.setDisplayName(intent.getStringExtra("displayName"));

        payPalClient.tokenizePayPalAccount(this, request);
    }

    protected void requestPaypalNonce() {
        Intent intent = getIntent();

        if (intent.getStringExtra("amount") == null) {
            payPalVaultFlow();
        } else {
            payPalCheckoutFlow();
        }
    }

    @Override
    public void onPayPalSuccess(@NonNull PayPalAccountNonce payPalAccountNonce) {
        onSuccess(payPalAccountNonce);
    }

    @Override
    public void onPayPalFailure(@NonNull Exception error) {
        onException(error);
    }
}
