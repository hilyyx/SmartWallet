package com.example.smartwallet.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.TransactionsApi;
import com.example.smartwallet.network.dto.Transaction;
import com.example.smartwallet.utils.ErrorHandler;
import com.example.smartwallet.utils.Logger;
import com.example.smartwallet.utils.NetworkUtils;
import com.example.smartwallet.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryViewModel extends ViewModel {
    
    private MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    
    private TransactionsApi transactionsApi;
    private TokenManager tokenManager;
    private List<Transaction> transactionList = new ArrayList<>();
    
    public HistoryViewModel() {
        transactionsApi = ApiClient.getTransactionsApi();
        transactions.setValue(transactionList);
        isLoading.setValue(false);
    }
    
    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public void loadTransactions() {
        String token = tokenManager.getToken();
        if (token == null) {
            Logger.w("HistoryViewModel", "Token not found");
            error.setValue("Токен не найден. Войдите в систему.");
            return;
        }
        
        // Check network connectivity (optional - will not block if permission is missing)
        try {
            if (!NetworkUtils.isNetworkAvailable(tokenManager.getContext())) {
                Logger.w("HistoryViewModel", "No network connection");
                error.setValue("Нет подключения к интернету. Проверьте соединение и попробуйте снова.");
                return;
            }
        } catch (Exception e) {
            Logger.w("HistoryViewModel", "Could not check network state, proceeding anyway");
            // Continue with the request even if we can't check network state
        }
        
        Logger.d("HistoryViewModel", "Loading transactions");
        isLoading.setValue(true);
        String authToken = "Bearer " + token;
        
        transactionsApi.getTransactions(authToken).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    Logger.i("HistoryViewModel", "Transactions loaded successfully: " + response.body().size() + " items");
                    transactionList = response.body();
                    transactions.setValue(new ArrayList<>(transactionList));
                } else {
                    String errorMsg = "Ошибка загрузки истории транзакций. Код: " + response.code();
                    Logger.e("HistoryViewModel", errorMsg);
                    error.setValue(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                isLoading.setValue(false);
                Logger.e("HistoryViewModel", "Failed to load transactions", t);
                error.setValue(ErrorHandler.getErrorMessage(t));
            }
        });
    }
    
    public void clearError() {
        error.setValue(null);
    }
}
