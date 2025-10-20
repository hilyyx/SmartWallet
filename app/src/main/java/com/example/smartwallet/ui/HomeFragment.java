package com.example.smartwallet.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartwallet.R;
import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.AssistantApi;
import com.example.smartwallet.network.CashbackApi;
import com.example.smartwallet.network.CardsApi;
import com.example.smartwallet.network.TransactionsApi;
import com.example.smartwallet.network.dto.BestCardResponse;
import com.example.smartwallet.network.dto.Card;
import com.example.smartwallet.network.dto.Recommendation;
import com.example.smartwallet.network.dto.TransactionRequest;
import com.example.smartwallet.utils.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView textActiveCardNumber;
    private TextView textActiveCardName;
    private TextView textActiveCardBank;
    private TextView textActiveCardCashback;
    private TextView textRecommendedCategory;
    private TextView textRecommendation;
    private Button buttonSmartChoice;
    private Button buttonPay;
    private ProgressBar progress;

    private CashbackApi cashbackApi;
    private CardsApi cardsApi;
    private TransactionsApi transactionsApi;
    private AssistantApi assistantApi;
    private TokenManager tokenManager;

    private Card activeCard;
    private BestCardResponse bestCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        initViews(view);
        setupClickListeners();
        
        cashbackApi = ApiClient.getCashbackApi();
        cardsApi = ApiClient.getCardsApi();
        transactionsApi = ApiClient.getTransactionsApi();
        assistantApi = ApiClient.getAssistantApi();
        tokenManager = TokenManager.getInstance(requireContext());
        
        loadActiveCard();
        loadRecommendations();
        
        return view;
    }
    
    private void initViews(View view) {
        textActiveCardNumber = view.findViewById(R.id.textActiveCardNumber);
        textActiveCardName = view.findViewById(R.id.textActiveCardName);
        textActiveCardBank = view.findViewById(R.id.textActiveCardBank);
        textActiveCardCashback = view.findViewById(R.id.textActiveCardCashback);
        textRecommendedCategory = view.findViewById(R.id.textRecommendedCategory);
        textRecommendation = view.findViewById(R.id.textRecommendation);
        buttonSmartChoice = view.findViewById(R.id.buttonSmartChoice);
        buttonPay = view.findViewById(R.id.buttonPay);
        progress = view.findViewById(R.id.progress);
    }
    
    private void setupClickListeners() {
        buttonSmartChoice.setOnClickListener(v -> getBestCard());
        buttonPay.setOnClickListener(v -> createTransaction());
    }
    
    private void loadActiveCard() {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Токен не найден. Войдите в систему.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        String authToken = "Bearer " + token;
        
        cardsApi.getCards(authToken).enqueue(new Callback<List<Card>>() {
            @Override
            public void onResponse(@NonNull Call<List<Card>> call, @NonNull Response<List<Card>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    activeCard = response.body().get(0); // Take first card as active
                    updateActiveCardDisplay();
                } else {
                    Toast.makeText(requireContext(), "Нет доступных карт", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Card>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadRecommendations() {
        String token = tokenManager.getToken();
        if (token == null) {
            return;
        }
        
        String authToken = "Bearer " + token;
        
        assistantApi.getRecommendations(authToken).enqueue(new Callback<List<Recommendation>>() {
            @Override
            public void onResponse(@NonNull Call<List<Recommendation>> call, @NonNull Response<List<Recommendation>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Take the first recommendation
                    Recommendation recommendation = response.body().get(0);
                    textRecommendation.setText(recommendation.message);
                } else {
                    // Fallback to default message
                    textRecommendation.setText("Рекомендую оплатить картой Тинькофф Black — 5% кэшбэк");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Recommendation>> call, @NonNull Throwable t) {
                // Fallback to default message on error
                textRecommendation.setText("Рекомендую оплатить картой Тинькофф Black — 5% кэшбэк");
            }
        });
    }
    
    private void updateActiveCardDisplay() {
        if (activeCard != null) {
            textActiveCardNumber.setText("•••• " + activeCard.last4);
            textActiveCardName.setText(activeCard.cardName);
            textActiveCardBank.setText(activeCard.bankName);
            
            // Calculate average cashback
            if (activeCard.cashbackRules != null) {
                double avgCashback = (activeCard.cashbackRules.additionalProp1 + 
                                    activeCard.cashbackRules.additionalProp2 + 
                                    activeCard.cashbackRules.additionalProp3) / 3.0;
                textActiveCardCashback.setText(String.format("%.0f%% кэшбэк", avgCashback));
            }
        }
    }
    
    private void getBestCard() {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Токен не найден. Войдите в систему.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        String authToken = "Bearer " + token;
        
        cashbackApi.getBestCard(authToken, "еда").enqueue(new Callback<BestCardResponse>() {
            @Override
            public void onResponse(@NonNull Call<BestCardResponse> call, @NonNull Response<BestCardResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    bestCard = response.body();
                    updateRecommendation();
                } else {
                    Toast.makeText(requireContext(), "Ошибка получения рекомендации", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BestCardResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateRecommendation() {
        if (bestCard != null) {
            textRecommendation.setText(String.format("Рекомендую оплатить картой %s %s — %d%% кэшбэк", 
                    bestCard.bankName, bestCard.cardName, bestCard.cashbackPercentage));
        }
    }
    
    private void createTransaction() {
        if (activeCard == null) {
            Toast.makeText(requireContext(), "Нет активной карты", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Токен не найден. Войдите в систему.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        String authToken = "Bearer " + token;
        
        // Create fake transaction
        TransactionRequest request = new TransactionRequest(1000.0, "еда", activeCard.id);
        
        transactionsApi.createTransaction(authToken, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Транзакция успешно создана!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Ошибка создания транзакции", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSmartChoice.setEnabled(!loading);
        buttonPay.setEnabled(!loading);
    }
}





