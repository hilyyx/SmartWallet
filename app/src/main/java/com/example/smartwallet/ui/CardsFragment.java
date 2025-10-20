package com.example.smartwallet.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.CardsApi;
import com.example.smartwallet.network.dto.Card;
import com.example.smartwallet.network.dto.CardRequest;
import com.example.smartwallet.network.dto.CashbackRules;
import com.example.smartwallet.ui.adapter.CardsAdapter;
import com.example.smartwallet.utils.TokenManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardsFragment extends Fragment implements CardsAdapter.OnCardClickListener {

    private RecyclerView recyclerCards;
    private LinearLayout emptyState;
    private ProgressBar progress;
    private FloatingActionButton fabAddCard;
    private CardsAdapter cardsAdapter;
    private CardsApi cardsApi;
    private TokenManager tokenManager;
    
    // Statistics views
    private TextView textTotalCards;
    private TextView textTotalLimit;
    private TextView textAvgCashback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        
        cardsApi = ApiClient.getCardsApi();
        tokenManager = TokenManager.getInstance(requireContext());
        
        loadCards();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerCards = view.findViewById(R.id.recyclerCards);
        emptyState = view.findViewById(R.id.emptyState);
        progress = view.findViewById(R.id.progress);
        fabAddCard = view.findViewById(R.id.fabAddCard);
        
        // Statistics views
        textTotalCards = view.findViewById(R.id.textTotalCards);
        textTotalLimit = view.findViewById(R.id.textTotalLimit);
        textAvgCashback = view.findViewById(R.id.textAvgCashback);
        
    }
    
    private void setupRecyclerView() {
        cardsAdapter = new CardsAdapter();
        cardsAdapter.setOnCardClickListener(this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), 
                LinearLayoutManager.HORIZONTAL, false);
        recyclerCards.setLayoutManager(layoutManager);
        recyclerCards.setAdapter(cardsAdapter);
        
        // Add snap effect for smooth card centering
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerCards);
    }
    
    private void setupClickListeners() {
        fabAddCard.setOnClickListener(v -> showAddCardDialog());
    }
    
    private void loadCards() {
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
                if (response.isSuccessful() && response.body() != null) {
                    List<Card> cards = response.body();
                    cardsAdapter.setCards(cards);
                    updateEmptyState(cards.isEmpty());
                    updateStatistics(cards);
                } else if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Сессия истекла. Войдите в систему.", Toast.LENGTH_SHORT).show();
                    tokenManager.clearToken();
                } else {
                    Toast.makeText(requireContext(), "Ошибка загрузки карт", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Card>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showAddCardDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_card);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        EditText editBankName = dialog.findViewById(R.id.editBankName);
        EditText editCardName = dialog.findViewById(R.id.editCardName);
        EditText editLast4 = dialog.findViewById(R.id.editLast4);
        EditText editLimitMonthly = dialog.findViewById(R.id.editLimitMonthly);
        
        dialog.findViewById(R.id.buttonCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.buttonSave).setOnClickListener(v -> {
            if (validateCardInput(editBankName, editCardName, editLast4, editLimitMonthly)) {
                saveCard(editBankName.getText().toString().trim(),
                        editCardName.getText().toString().trim(),
                        editLast4.getText().toString().trim(),
                        editLimitMonthly.getText().toString().trim(),
                        dialog);
            }
        });
        
        dialog.show();
    }
    
    private boolean validateCardInput(EditText bankName, EditText cardName, EditText last4, EditText limit) {
        if (TextUtils.isEmpty(bankName.getText())) {
            bankName.setError("Введите название банка");
            return false;
        }
        if (TextUtils.isEmpty(cardName.getText())) {
            cardName.setError("Введите название карты");
            return false;
        }
        if (TextUtils.isEmpty(last4.getText()) || last4.getText().length() != 4) {
            last4.setError("Введите 4 последние цифры");
            return false;
        }
        if (TextUtils.isEmpty(limit.getText())) {
            limit.setError("Введите месячный лимит");
            return false;
        }
        return true;
    }
    
    private void saveCard(String bankName, String cardName, String last4, String limitStr, Dialog dialog) {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Токен не найден. Войдите в систему.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double limit = Double.parseDouble(limitStr);
            CashbackRules cashbackRules = new CashbackRules(1, 2, 3); // Default values (1%, 2%, 3%)
            CardRequest request = new CardRequest(bankName, cardName, last4, cashbackRules, limit);
            
            String authToken = "Bearer " + token;
            cardsApi.createCard(authToken, request).enqueue(new Callback<Card>() {
                @Override
                public void onResponse(@NonNull Call<Card> call, @NonNull Response<Card> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Карта добавлена успешно", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadCards(); // Refresh the list
                } else {
                    String errorMessage = "Ошибка добавления карты";
                    if (response.code() == 401) {
                        errorMessage = "Сессия истекла. Войдите в систему.";
                        tokenManager.clearToken();
                    } else if (response.code() == 422) {
                        errorMessage = "Ошибка валидации данных. Проверьте правильность заполнения полей";
                    } else if (response.code() == 400) {
                        errorMessage = "Неверные данные карты";
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
                }

                @Override
                public void onFailure(@NonNull Call<Card> call, @NonNull Throwable t) {
                    Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Неверный формат лимита", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerCards.setVisibility(loading ? View.GONE : View.VISIBLE);
    }
    
    private void updateEmptyState(boolean isEmpty) {
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerCards.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
    
    @Override
    public void onCardClick(Card card) {
        Toast.makeText(requireContext(), "Карта: " + card.cardName, Toast.LENGTH_SHORT).show();
        // TODO: Implement card details or edit functionality
    }
    
    private void updateStatistics(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            textTotalCards.setText("0");
            textTotalLimit.setText("0 ₽");
            textAvgCashback.setText("0%");
            return;
        }
        
        // Total cards count
        textTotalCards.setText(String.valueOf(cards.size()));
        
        // Total limit
        double totalLimit = 0;
        double totalCashback = 0;
        
        for (Card card : cards) {
            totalLimit += card.limitMonthly;
            
            // Calculate average cashback from cashback rules
            if (card.cashbackRules != null) {
                double avgCardCashback = (card.cashbackRules.additionalProp1 + 
                                        card.cashbackRules.additionalProp2 + 
                                        card.cashbackRules.additionalProp3) / 3.0;
                totalCashback += avgCardCashback;
            }
        }
        
        textTotalLimit.setText(String.format("%.0f ₽", totalLimit));
        
        // Average cashback
        double avgCashback = cards.size() > 0 ? totalCashback / cards.size() : 0;
        textAvgCashback.setText(String.format("%.1f%%", avgCashback));
    }
}




