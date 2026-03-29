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
import com.example.smartwallet.network.dto.BestCardResponse;
import com.example.smartwallet.utils.ErrorHandler;
import com.example.smartwallet.utils.PayQrPayloadHelper;
import com.example.smartwallet.utils.TokenManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

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

        String raw = getIntent().getStringExtra(EXTRA_QR_RAW);
        if (raw == null || raw.isEmpty()) {
            Toast.makeText(this, R.string.pay_confirm_no_qr, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textPaySummary.setText(PayQrPayloadHelper.summaryLine(raw));
        String preview = raw.length() > 400 ? raw.substring(0, 397) + "…" : raw;
        textPayQrRaw.setText(preview);

        loadBestCard(raw);
    }

    private void loadBestCard(@NonNull String raw) {
        String token = TokenManager.getInstance(this).getToken();
        if (token == null) {
            progressBestCard.setVisibility(View.GONE);
            textBestCardRecommendation.setText(R.string.pay_confirm_need_login);
            return;
        }

        String category = PayQrPayloadHelper.detectCategory(raw);
        progressBestCard.setVisibility(View.VISIBLE);
        textBestCardRecommendation.setText("");

        CashbackApi api = ApiClient.getCashbackApi();
        api.getBestCard("Bearer " + token, category).enqueue(new Callback<BestCardResponse>() {
            @Override
            public void onResponse(@NonNull Call<BestCardResponse> call, @NonNull Response<BestCardResponse> response) {
                progressBestCard.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    BestCardResponse b = response.body();
                    String line = String.format(Locale.forLanguageTag("ru"),
                            "%s %s — %d%% кэшбэк (категория: %s)",
                            b.bankName != null ? b.bankName : "",
                            b.cardName != null ? b.cardName : "",
                            b.cashbackPercentage,
                            category);
                    textBestCardRecommendation.setText(line.trim());
                } else {
                    textBestCardRecommendation.setText(R.string.pay_confirm_best_card_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BestCardResponse> call, @NonNull Throwable t) {
                progressBestCard.setVisibility(View.GONE);
                textBestCardRecommendation.setText(R.string.pay_confirm_best_card_error);
                ErrorHandler.showError(PayConfirmActivity.this, t);
            }
        });
    }
}
