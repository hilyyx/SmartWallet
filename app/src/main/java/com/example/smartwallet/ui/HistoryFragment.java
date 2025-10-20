package com.example.smartwallet.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.smartwallet.R;
import com.example.smartwallet.network.dto.Transaction;
import com.example.smartwallet.ui.adapter.TransactionsAdapter;
import com.example.smartwallet.utils.TokenManager;
import com.example.smartwallet.viewmodel.HistoryViewModel;

public class HistoryFragment extends Fragment implements TransactionsAdapter.OnTransactionClickListener {

    private RecyclerView recyclerHistory;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private ProgressBar progress;
    private TransactionsAdapter transactionsAdapter;
    private HistoryViewModel historyViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupViewModel();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerHistory = view.findViewById(R.id.recyclerHistory);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyState = view.findViewById(R.id.emptyState);
        progress = view.findViewById(R.id.progress);
        
        // Настройка SwipeRefreshLayout
        swipeRefresh.setColorSchemeResources(R.color.brand_primary);
        swipeRefresh.setOnRefreshListener(() -> {
            historyViewModel.loadTransactions();
        });
    }
    
    private void setupRecyclerView() {
        transactionsAdapter = new TransactionsAdapter();
        transactionsAdapter.setOnTransactionClickListener(this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerHistory.setLayoutManager(layoutManager);
        recyclerHistory.setAdapter(transactionsAdapter);
    }
    
    private void setupViewModel() {
        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        historyViewModel.setTokenManager(TokenManager.getInstance(requireContext()));
        
        // Observe transactions
        historyViewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            transactionsAdapter.setTransactions(transactions);
            updateEmptyState(transactions.isEmpty());
        });
        
        // Observe loading state
        historyViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            setLoading(isLoading);
            swipeRefresh.setRefreshing(isLoading);
        });
        
        // Observe errors
        historyViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                historyViewModel.clearError();
            }
        });
        
        // Load transactions
        historyViewModel.loadTransactions();
    }
    
    private void updateEmptyState(boolean isEmpty) {
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
    
    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerHistory.setVisibility(loading ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(loading ? View.GONE : View.GONE);
    }
    
    @Override
    public void onTransactionClick(Transaction transaction) {
        showTransactionDetailsDialog(transaction);
    }
    
    private void showTransactionDetailsDialog(Transaction transaction) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_transaction_details);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Найти элементы диалога
        TextView textAmount = dialog.findViewById(R.id.textAmount);
        TextView textCategory = dialog.findViewById(R.id.textCategory);
        TextView textDate = dialog.findViewById(R.id.textDate);
        TextView textCard = dialog.findViewById(R.id.textCard);
        TextView textMcc = dialog.findViewById(R.id.textMcc);
        TextView textCashback = dialog.findViewById(R.id.textCashback);
        
        // Заполнить данные
        textAmount.setText(String.format("%.2f ₽", transaction.amount));
        textCategory.setText(transaction.category);
        textDate.setText(formatDate(transaction.createdAt));
        textCard.setText("Карта #" + transaction.cardId);
        textMcc.setText("5812"); // MCC для категории "Еда"
        textCashback.setText(String.format("%.2f ₽", transaction.cashbackEarned));
        
        // Кнопка закрытия
        dialog.findViewById(R.id.buttonClose).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private String formatDate(String dateString) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (java.text.ParseException e) {
            return dateString;
        }
    }
}





