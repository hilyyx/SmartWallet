package com.example.smartwallet.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.HttpException;

public class ErrorHandler {
    
    public static String getErrorMessage(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            int code = httpException.code();
            
            switch (code) {
                case 400:
                    return "Неверный запрос. Проверьте введенные данные.";
                case 401:
                    return "Необходимо войти в систему.";
                case 403:
                    return "Доступ запрещен.";
                case 404:
                    return "Ресурс не найден.";
                case 422:
                    return "Ошибка валидации данных.";
                case 500:
                    return "Внутренняя ошибка сервера.";
                case 502:
                    return "Сервер временно недоступен.";
                case 503:
                    return "Сервис временно недоступен.";
                default:
                    return "Ошибка сервера: " + code;
            }
        } else if (throwable instanceof ConnectException) {
            return "Нет подключения к серверу. Проверьте интернет-соединение.";
        } else if (throwable instanceof SocketTimeoutException) {
            return "Превышено время ожидания. Попробуйте еще раз.";
        } else if (throwable instanceof UnknownHostException) {
            return "Сервер недоступен. Проверьте подключение к интернету.";
        } else if (throwable instanceof IOException) {
            return "Ошибка сети. Проверьте подключение к интернету.";
        } else if (throwable instanceof SecurityException) {
            return "Ошибка доступа. Проверьте разрешения приложения.";
        } else {
            String message = throwable.getMessage();
            if (message != null && message.contains("ACCESS_NETWORK_STATE")) {
                return "Ошибка доступа к состоянию сети. Приложение будет работать в ограниченном режиме.";
            }
            return "Неизвестная ошибка: " + message;
        }
    }
    
    public static void showError(Context context, Throwable throwable) {
        String message = getErrorMessage(throwable);
        Logger.e("ErrorHandler", "Showing error: " + message, throwable);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    public static void showError(Context context, String message) {
        Logger.e("ErrorHandler", "Showing error: " + message);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    public static boolean isNetworkError(Throwable throwable) {
        return throwable instanceof ConnectException ||
               throwable instanceof SocketTimeoutException ||
               throwable instanceof UnknownHostException ||
               throwable instanceof IOException;
    }
    
    public static boolean isAuthError(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            int code = httpException.code();
            return code == 401 || code == 403;
        }
        return false;
    }
}
