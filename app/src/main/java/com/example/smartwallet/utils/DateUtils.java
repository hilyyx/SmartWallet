package com.example.smartwallet.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final String API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String DISPLAY_DATE_FORMAT = "dd.MM.yyyy HH:mm";
    private static final String DISPLAY_DATE_ONLY_FORMAT = "dd.MM.yyyy";
    private static final String DISPLAY_TIME_ONLY_FORMAT = "HH:mm";
    
    public static String formatApiDate(String apiDate) {
        return formatApiDate(apiDate, DISPLAY_DATE_FORMAT);
    }
    
    public static String formatApiDate(String apiDate, String outputFormat) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
            SimpleDateFormat outputDateFormat = new SimpleDateFormat(outputFormat, Locale.getDefault());
            Date date = inputFormat.parse(apiDate);
            return outputDateFormat.format(date);
        } catch (ParseException e) {
            Logger.e("DateUtils", "Error parsing date: " + apiDate, e);
            return apiDate; // Return original string if parsing fails
        }
    }
    
    public static String formatDateOnly(String apiDate) {
        return formatApiDate(apiDate, DISPLAY_DATE_ONLY_FORMAT);
    }
    
    public static String formatTimeOnly(String apiDate) {
        return formatApiDate(apiDate, DISPLAY_TIME_ONLY_FORMAT);
    }
    
    public static boolean isToday(String apiDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
            Date transactionDate = inputFormat.parse(apiDate);
            Date today = new Date();
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return dateFormat.format(transactionDate).equals(dateFormat.format(today));
        } catch (ParseException e) {
            Logger.e("DateUtils", "Error checking if date is today: " + apiDate, e);
            return false;
        }
    }
    
    public static boolean isYesterday(String apiDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
            Date transactionDate = inputFormat.parse(apiDate);
            Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return dateFormat.format(transactionDate).equals(dateFormat.format(yesterday));
        } catch (ParseException e) {
            Logger.e("DateUtils", "Error checking if date is yesterday: " + apiDate, e);
            return false;
        }
    }
}
