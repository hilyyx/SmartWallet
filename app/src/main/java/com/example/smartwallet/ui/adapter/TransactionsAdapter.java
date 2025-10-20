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
import com.example.smartwallet.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

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
    
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        notifyItemInserted(transactions.size() - 1);
    }
    
    public void removeTransaction(int position) {
        if (position >= 0 && position < transactions.size()) {
            transactions.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void updateTransaction(int position, Transaction transaction) {
        if (position >= 0 && position < transactions.size()) {
            transactions.set(position, transaction);
            notifyItemChanged(position);
        }
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
            String formattedDate = DateUtils.formatApiDate(transaction.createdAt);
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
    }
}
