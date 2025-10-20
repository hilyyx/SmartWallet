package com.example.smartwallet.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.TransactionsApi;
import com.example.smartwallet.network.dto.AnalyticsData;
import com.example.smartwallet.network.dto.Transaction;
import com.example.smartwallet.utils.ErrorHandler;
import com.example.smartwallet.utils.TokenManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsViewModel extends ViewModel {
    
    private MutableLiveData<AnalyticsData> analyticsData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    
    private TransactionsApi transactionsApi;
    private TokenManager tokenManager;
    
    public AnalyticsViewModel() {
        transactionsApi = ApiClient.getTransactionsApi();
        isLoading.setValue(false);
    }
    
    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    public LiveData<AnalyticsData> getAnalyticsData() {
        return analyticsData;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public void loadAnalytics() {
        String token = tokenManager.getToken();
        if (token == null) {
            error.setValue("Токен не найден. Войдите в систему.");
            return;
        }
        
        isLoading.setValue(true);
        String authToken = "Bearer " + token;
        
        // Загружаем историю транзакций
        transactionsApi.getTransactions(authToken).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    // Рассчитываем аналитику на основе транзакций
                    AnalyticsData calculatedAnalytics = calculateAnalytics(response.body());
                    analyticsData.setValue(calculatedAnalytics);
                } else {
                    error.setValue("Ошибка загрузки истории транзакций");
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(ErrorHandler.getErrorMessage(t));
            }
        });
    }
    
    private AnalyticsData calculateAnalytics(List<Transaction> transactions) {
        AnalyticsData analytics = new AnalyticsData();
        
        // Рассчитываем общий кэшбэк
        double totalCashback = 0;
        for (Transaction transaction : transactions) {
            totalCashback += transaction.cashbackEarned;
        }
        analytics.totalCashback = totalCashback;
        
        // Рассчитываем траты по категориям
        Map<String, Double> categoryAmounts = new HashMap<>();
        double totalSpending = 0;
        
        for (Transaction transaction : transactions) {
            String category = transaction.category != null ? transaction.category : "Прочее";
            double amount = transaction.amount;
            
            categoryAmounts.put(category, categoryAmounts.getOrDefault(category, 0.0) + amount);
            totalSpending += amount;
        }
        
        // Создаем массив трат по категориям
        List<AnalyticsData.CategorySpending> categorySpendingList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryAmounts.entrySet()) {
            AnalyticsData.CategorySpending categorySpending = new AnalyticsData.CategorySpending();
            categorySpending.category = entry.getKey();
            categorySpending.amount = entry.getValue();
            categorySpending.percentage = totalSpending > 0 ? (entry.getValue() / totalSpending) * 100 : 0;
            categorySpendingList.add(categorySpending);
        }
        
        analytics.categorySpending = categorySpendingList.toArray(new AnalyticsData.CategorySpending[0]);
        
        // Рассчитываем тренды (упрощенная версия)
        analytics.spendingTrends = calculateTrends(transactions);
        
        return analytics;
    }
    
    private AnalyticsData.SpendingTrends calculateTrends(List<Transaction> transactions) {
        AnalyticsData.SpendingTrends trends = new AnalyticsData.SpendingTrends();
        
        // Простая логика: сравниваем траты на еду за последние 30 дней с предыдущими 30 днями
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Date sixtyDaysAgo = calendar.getTime();
        
        double foodSpendingLast30Days = 0;
        double foodSpendingPrevious30Days = 0;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        
        for (Transaction transaction : transactions) {
            try {
                Date transactionDate = dateFormat.parse(transaction.createdAt);
                if (transaction.category != null && transaction.category.toLowerCase().contains("еда")) {
                    if (transactionDate.after(thirtyDaysAgo) && transactionDate.before(now)) {
                        foodSpendingLast30Days += transaction.amount;
                    } else if (transactionDate.after(sixtyDaysAgo) && transactionDate.before(thirtyDaysAgo)) {
                        foodSpendingPrevious30Days += transaction.amount;
                    }
                }
            } catch (ParseException e) {
                // Игнорируем ошибки парсинга даты
            }
        }
        
        if (foodSpendingPrevious30Days > 0) {
            double percentageChange = ((foodSpendingLast30Days - foodSpendingPrevious30Days) / foodSpendingPrevious30Days) * 100;
            trends.trendPercentage = Math.abs(percentageChange);
            trends.foodTrend = percentageChange < 0 ? "меньше" : "больше";
        } else {
            trends.trendPercentage = 0;
            trends.foodTrend = "меньше";
        }
        
        return trends;
    }
    
    public void clearError() {
        error.setValue(null);
    }
}
