package com.example.smartwallet.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartwallet.R;
import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.CashbackApi;
import com.example.smartwallet.network.TransactionsApi;
import com.example.smartwallet.network.dto.BestCardResponse;
import com.example.smartwallet.network.dto.TransactionRequest;
import com.example.smartwallet.utils.ErrorHandler;
import com.example.smartwallet.utils.PayQrPayloadHelper;
import com.example.smartwallet.utils.TokenManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран после сканирования QR: кратко данные платежа и лучшая карта по категории.
 */
public final class PayConfirmActivity extends AppCompatActivity {

    public static final String EXTRA_QR_RAW = "qr_raw";

    private TextView textPaySummary;
    private TextView textPayQrRaw;
    private TextView textBestCardRecommendation;
    private CircularProgressIndicator progressBestCard;
    private MaterialButton buttonConfirmPurchase;
    private LinearProgressIndicator progressConfirmPay;

    @Nullable
    private String qrRaw;
    @NonNull
    private String payCategory = "прочее";
    private int recommendedCardId = -1;
    private boolean payInProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_confirm);

        MaterialToolbar toolbar = findViewById(R.id.toolbarPayConfirm);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        textPaySummary = findViewById(R.id.textPaySummary);
        textPayQrRaw = findViewById(R.id.textPayQrRaw);
        textBestCardRecommendation = findViewById(R.id.textBestCardRecommendation);
        progressBestCard = findViewById(R.id.progressBestCard);
        buttonConfirmPurchase = findViewById(R.id.buttonConfirmPurchase);
        progressConfirmPay = findViewById(R.id.progressConfirmPay);

        String raw = getIntent().getStringExtra(EXTRA_QR_RAW);
        if (raw == null || raw.isEmpty()) {
            Toast.makeText(this, R.string.pay_confirm_no_qr, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        qrRaw = raw;
        payCategory = PayQrPayloadHelper.detectCategory(raw);

        textPaySummary.setText(PayQrPayloadHelper.summaryLine(raw));
        String preview = raw.length() > 400 ? raw.substring(0, 397) + "…" : raw;
        textPayQrRaw.setText(preview);

        buttonConfirmPurchase.setOnClickListener(v -> confirmPurchase());

        loadBestCard(raw);
    }

    private void updateConfirmButtonState() {
        if (buttonConfirmPurchase == null) return;
        String token = TokenManager.getInstance(this).getToken();
        boolean ok = token != null && recommendedCardId > 0 && !payInProgress;
        buttonConfirmPurchase.setEnabled(ok);
    }

    private void confirmPurchase() {
        if (payInProgress || qrRaw == null) return;
        if (recommendedCardId <= 0) {
            Toast.makeText(this, R.string.pay_confirm_need_card, Toast.LENGTH_SHORT).show();
            return;
        }
        String token = TokenManager.getInstance(this).getToken();
        if (token == null) {
            Toast.makeText(this, R.string.pay_confirm_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        Double parsedAmount = PayQrPayloadHelper.parseAmountRub(qrRaw);
        double amount = parsedAmount != null ? parsedAmount : 1.0;
        if (parsedAmount == null) {
            Toast.makeText(this, R.string.pay_confirm_amount_default_hint, Toast.LENGTH_LONG).show();
        }

        payInProgress = true;
        progressConfirmPay.setVisibility(View.VISIBLE);
        updateConfirmButtonState();

        TransactionRequest request = new TransactionRequest(amount, payCategory, recommendedCardId);
        TransactionsApi api = ApiClient.getTransactionsApi();
        api.createTransaction("Bearer " + token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                payInProgress = false;
                progressConfirmPay.setVisibility(View.GONE);
                updateConfirmButtonState();
                if (response.isSuccessful()) {
                    Toast.makeText(PayConfirmActivity.this, R.string.pay_confirm_success, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (response.code() == 401) {
                    TokenManager.getInstance(PayConfirmActivity.this).clearToken();
                    Toast.makeText(PayConfirmActivity.this, "Сессия истекла. Войдите снова.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                Toast.makeText(PayConfirmActivity.this, R.string.pay_confirm_pay_error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                payInProgress = false;
                progressConfirmPay.setVisibility(View.GONE);
                updateConfirmButtonState();
                Toast.makeText(PayConfirmActivity.this, R.string.pay_confirm_pay_error, Toast.LENGTH_SHORT).show();
                ErrorHandler.showError(PayConfirmActivity.this, t);
            }
        });
    }

    private void loadBestCard(@NonNull String raw) {
        String token = TokenManager.getInstance(this).getToken();
        if (token == null) {
            progressBestCard.setVisibility(View.GONE);
            textBestCardRecommendation.setText(R.string.pay_confirm_need_login);
            recommendedCardId = -1;
            updateConfirmButtonState();
            return;
        }

        String category = PayQrPayloadHelper.detectCategory(raw);
        payCategory = category;
        progressBestCard.setVisibility(View.VISIBLE);
        textBestCardRecommendation.setText("");
        recommendedCardId = -1;
        updateConfirmButtonState();

        CashbackApi api = ApiClient.getCashbackApi();
        api.getBestCard("Bearer " + token, category).enqueue(new Callback<BestCardResponse>() {
            @Override
            public void onResponse(@NonNull Call<BestCardResponse> call, @NonNull Response<BestCardResponse> response) {
                progressBestCard.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    BestCardResponse b = response.body();
                    if (b.cardId > 0) {
                        recommendedCardId = b.cardId;
                    } else {
                        recommendedCardId = -1;
                    }
                    String line = String.format(Locale.forLanguageTag("ru"),
                            "%s %s — %d%% кэшбэк (категория: %s)",
                            b.bankName != null ? b.bankName : "",
                            b.cardName != null ? b.cardName : "",
                            b.cashbackPercentage,
                            category);
                    textBestCardRecommendation.setText(line.trim());
                } else {
                    recommendedCardId = -1;
                    textBestCardRecommendation.setText(R.string.pay_confirm_best_card_error);
                }
                updateConfirmButtonState();
            }

            @Override
            public void onFailure(@NonNull Call<BestCardResponse> call, @NonNull Throwable t) {
                progressBestCard.setVisibility(View.GONE);
                recommendedCardId = -1;
                textBestCardRecommendation.setText(R.string.pay_confirm_best_card_error);
                ErrorHandler.showError(PayConfirmActivity.this, t);
                updateConfirmButtonState();
            }
        });
    }
}
