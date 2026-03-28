package com.example.smartwallet.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.example.smartwallet.utils.ErrorHandler;
import com.example.smartwallet.utils.TokenManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardsFragment extends Fragment implements CardsAdapter.OnCardClickListener {

    private RecyclerView recyclerCards;
    private LinearLayout emptyState;
    private View errorState;
    private ProgressBar progress;
    private FloatingActionButton fabAddCard;
    private MaterialCardView cardSelectedCardInfo;
    private TextView textSelectedCardTitle;
    private TextView textSelectedCardDetails;
    private CardsAdapter cardsAdapter;
    private CardsApi cardsApi;
    private TokenManager tokenManager;
    private final PagerSnapHelper snapHelper = new PagerSnapHelper();
    
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
        errorState = view.findViewById(R.id.errorState);
        progress = view.findViewById(R.id.progress);
        fabAddCard = view.findViewById(R.id.fabAddCard);
        cardSelectedCardInfo = view.findViewById(R.id.cardSelectedCardInfo);
        textSelectedCardTitle = view.findViewById(R.id.textSelectedCardTitle);
        textSelectedCardDetails = view.findViewById(R.id.textSelectedCardDetails);
        
        // Statistics views
        textTotalCards = view.findViewById(R.id.textTotalCards);
        textTotalLimit = view.findViewById(R.id.textTotalLimit);
        textAvgCashback = view.findViewById(R.id.textAvgCashback);
        
        fabAddCard.setOnClickListener(v -> showAddCardDialog());
        
        // Setup error state retry button
        setupErrorState();
    }
    
    private void setupRecyclerView() {
        cardsAdapter = new CardsAdapter();
        cardsAdapter.setOnCardClickListener(this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), 
                LinearLayoutManager.HORIZONTAL, false);
        recyclerCards.setLayoutManager(layoutManager);
        recyclerCards.setAdapter(cardsAdapter);
        
        snapHelper.attachToRecyclerView(recyclerCards);
        recyclerCards.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateSelectedCardInfo();
                }
            }
        });
    }
    
    private void setupClickListeners() {
        fabAddCard.setOnClickListener(v -> showAddCardDialog());
    }
    
    private void setupErrorState() {
        if (errorState != null) {
            Button retryButton = errorState.findViewById(R.id.buttonRetry);
            if (retryButton != null) {
                retryButton.setOnClickListener(v -> {
                    hideErrorState();
                    loadCards();
                });
            }
        }
    }
    
    private void showErrorState(String message) {
        if (errorState != null) {
            TextView errorMessage = errorState.findViewById(R.id.textErrorMessage);
            TextView errorDescription = errorState.findViewById(R.id.textErrorDescription);
            
            if (errorMessage != null) {
                errorMessage.setText("Ошибка загрузки карт");
            }
            if (errorDescription != null) {
                errorDescription.setText(message);
            }
            
            errorState.setVisibility(View.VISIBLE);
            recyclerCards.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }
    }
    
    private void hideErrorState() {
        if (errorState != null) {
            errorState.setVisibility(View.GONE);
        }
    }
    
    /** После демо-сидинга: обновить список, если вкладка «Карты» на экране. */
    public void reloadFromServer() {
        loadCards();
    }

    private void loadCards() {
        String token = tokenManager.getToken();
        if (token == null) {
            showErrorState("Токен не найден. Войдите в систему.");
            return;
        }
        
        hideErrorState();
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
                    showErrorState("Сессия истекла. Войдите в систему.");
                    tokenManager.clearToken();
                } else {
                    showErrorState("Ошибка загрузки карт. Код: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Card>> call, @NonNull Throwable t) {
                setLoading(false);
                showErrorState(ErrorHandler.getErrorMessage(t));
            }
        });
    }
    
    private void showAddCardDialog() {
        showAddCardDialog(null);
    }

    public void showAddCardDialog(@Nullable String suggestedCardName) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_card);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        EditText editBankName = dialog.findViewById(R.id.editBankName);
        EditText editCardName = dialog.findViewById(R.id.editCardName);
        EditText editLast4 = dialog.findViewById(R.id.editLast4);
        EditText editLimitMonthly = dialog.findViewById(R.id.editLimitMonthly);

        if (!TextUtils.isEmpty(suggestedCardName) && editCardName != null) {
            editCardName.setText(suggestedCardName);
        }
        
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
            // Уходит на бэкенд; в списке карт отображаются значения из ответа GET /cards (не подставляются на клиенте).
            CashbackRules cashbackRules = new CashbackRules(1, 2, 3);
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
                    ErrorHandler.showError(requireContext(), t);
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
        hideErrorState();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerCards.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (cardSelectedCardInfo != null) {
            cardSelectedCardInfo.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (!isEmpty) {
            recyclerCards.post(() -> updateSelectedCardInfo());
        }
    }

    private void updateSelectedCardInfo() {
        if (textSelectedCardTitle == null || textSelectedCardDetails == null || cardsAdapter == null) return;
        if (cardsAdapter.getCardCount() == 0) return;
        LinearLayoutManager lm = (LinearLayoutManager) recyclerCards.getLayoutManager();
        if (lm == null) return;
        View snap = snapHelper.findSnapView(lm);
        int pos = snap != null ? lm.getPosition(snap) : 0;
        if (pos < 0) pos = 0;
        Card c = cardsAdapter.getCardAt(pos);
        if (c == null) return;
        String title = !TextUtils.isEmpty(c.cardName) ? c.cardName : (c.bankName != null ? c.bankName : "Карта");
        textSelectedCardTitle.setText(title);
        textSelectedCardDetails.setText("•••• " + (c.last4 != null ? c.last4 : "—"));
    }

    @NonNull
    private String formatCardSummaryMessage(@NonNull Card c) {
        String name = !TextUtils.isEmpty(c.cardName) ? c.cardName : (c.bankName != null ? c.bankName : "Карта");
        return name + "\n•••• " + (c.last4 != null ? c.last4 : "—");
    }

    private void showCardDetailsDialog(@NonNull Card card) {
        String dialogTitle = !TextUtils.isEmpty(card.bankName) ? card.bankName : "Карта";
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_SmartWallet_MaterialAlertDialog_Rounded)
                .setTitle(dialogTitle)
                .setMessage(formatCardSummaryMessage(card))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    @Override
    public void onCardClick(Card card) {
        showCardDetailsDialog(card);
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




