package com.example.smartwallet.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.TransactionsApi;
import com.example.smartwallet.network.dto.Transaction;
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
            error.setValue("Токен не найден. Войдите в систему.");
            return;
        }
        
        isLoading.setValue(true);
        String authToken = "Bearer " + token;
        
        transactionsApi.getTransactions(authToken).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    transactionList = response.body();
                    transactions.setValue(new ArrayList<>(transactionList));
                } else {
                    error.setValue("Ошибка загрузки истории транзакций");
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }
    
    public void clearError() {
        error.setValue(null);
    }
}
