package com.example.smartwallet.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.model.ChatMessage;
import com.example.smartwallet.ui.adapter.ChatAdapter;
import com.example.smartwallet.utils.TokenManager;
import com.example.smartwallet.viewmodel.ChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class AssistantActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private Button buttonSend;
    private ChatAdapter chatAdapter;
    private ChatViewModel chatViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        setupToolbar();
        initViews();
        setupChat();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Ассистент");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        recyclerChat = findViewById(R.id.recyclerChat);
        editMessage = findViewById(R.id.editMessage);
        buttonSend = findViewById(R.id.buttonSend);
    }

    private void setupChat() {
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        chatViewModel.setTokenManager(TokenManager.getInstance(this));
        
        chatAdapter = new ChatAdapter();
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);
        
        // Observe messages
        chatViewModel.getMessages().observe(this, messages -> {
            chatAdapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recyclerChat.scrollToPosition(messages.size() - 1);
            }
        });
        
        // Observe loading state
        chatViewModel.getIsLoading().observe(this, isLoading -> {
            buttonSend.setEnabled(!isLoading);
            if (isLoading) {
                buttonSend.setText("Отправка...");
            } else {
                buttonSend.setText("Отправить");
            }
        });
        
        // Observe errors
        chatViewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                chatViewModel.clearError();
            }
        });
        
        // Send button click
        buttonSend.setOnClickListener(v -> sendMessage());
        
        // Add welcome message
        addWelcomeMessage();
    }

    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
            "Привет! Я ваш AI ассистент по финансовой грамотности. " +
            "Могу помочь с вопросами о бюджетировании, инвестициях, кредитах и других финансовых темах. " +
            "Чем могу помочь?",
            ChatMessage.TYPE_ASSISTANT
        );
        chatAdapter.addMessage(welcomeMessage);
    }

    private void sendMessage() {
        String message = editMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(message)) {
            chatViewModel.sendMessage(message);
            editMessage.setText("");
        }
    }
}
