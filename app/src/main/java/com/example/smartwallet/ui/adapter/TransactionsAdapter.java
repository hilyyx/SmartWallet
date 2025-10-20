package com.example.smartwallet.ui.adapter;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.network.dto.Transaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder> {
    
    private List<Transaction> transactions = new ArrayList<>();
    private OnTransactionClickListener listener;
    
    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }
    
    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }
    
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return transactions.size();
    }
    
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView textDate;
        private TextView textAmount;
        private TextView textCategory;
        private TextView textCard;
        private TextView textCashback;
        
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textAmount = itemView.findViewById(R.id.textAmount);
            textCategory = itemView.findViewById(R.id.textCategory);
            textCard = itemView.findViewById(R.id.textCard);
            textCashback = itemView.findViewById(R.id.textCashback);
        }
        
        public void bind(Transaction transaction) {
            // Форматирование даты
            String formattedDate = formatDate(transaction.createdAt);
            textDate.setText(formattedDate);
            
            // Сумма с знаком
            textAmount.setText(String.format("%.2f ₽", transaction.amount));
            
            // Категория
            textCategory.setText(transaction.category);
            
            // Карта (показываем только ID, можно улучшить)
            textCard.setText("Карта #" + transaction.cardId);
            
            // Кэшбэк
            textCashback.setText(String.format("%.2f ₽", transaction.cashbackEarned));
        }
        
        private String formatDate(String dateString) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return dateString;
            }
        }
    }
}
