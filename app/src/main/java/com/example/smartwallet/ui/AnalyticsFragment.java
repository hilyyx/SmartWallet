package com.example.smartwallet.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartwallet.R;
import com.example.smartwallet.network.dto.AnalyticsData;
import com.example.smartwallet.utils.TokenManager;
import com.example.smartwallet.viewmodel.AnalyticsViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private PieChart pieChart;
    private TextView textTotalCashback;
    private TextView textTrends;
    private ProgressBar progress;
    private AnalyticsViewModel analyticsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);
        
        initViews(view);
        setupPieChart();
        setupViewModel();
        
        return view;
    }
    
    private void initViews(View view) {
        pieChart = view.findViewById(R.id.pieChart);
        textTotalCashback = view.findViewById(R.id.textTotalCashback);
        textTrends = view.findViewById(R.id.textTrends);
        progress = view.findViewById(R.id.progress);
    }
    
    private void setupPieChart() {
        // Настройка внешнего вида диаграммы
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Траты");
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.parseColor("#757575"));
        
        // Настройка легенды
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(0f);
        legend.setYOffset(0f);
        legend.setTextSize(12f);
        legend.setTextColor(Color.parseColor("#757575"));
        
        // Отключение вращения
        pieChart.setRotationEnabled(false);
        pieChart.setHighlightPerTapEnabled(true);
    }
    
    private void setupViewModel() {
        analyticsViewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        analyticsViewModel.setTokenManager(TokenManager.getInstance(requireContext()));
        
        // Observe analytics data
        analyticsViewModel.getAnalyticsData().observe(getViewLifecycleOwner(), analyticsData -> {
            if (analyticsData != null) {
                updateAnalytics(analyticsData);
            }
        });
        
        // Observe loading state
        analyticsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            setLoading(isLoading);
        });
        
        // Observe errors
        analyticsViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                analyticsViewModel.clearError();
            }
        });
        
        // Load analytics
        analyticsViewModel.loadAnalytics();
    }
    
    private void updateAnalytics(AnalyticsData analyticsData) {
        // Обновить общую сумму кэшбэка
        textTotalCashback.setText(String.format("%.0f ₽", analyticsData.totalCashback));
        
        // Обновить тренды
        if (analyticsData.spendingTrends != null) {
            String trendText = String.format("Ты тратишь на еду на %.0f%% %s, чем месяц назад.",
                    analyticsData.spendingTrends.trendPercentage,
                    analyticsData.spendingTrends.foodTrend);
            textTrends.setText(trendText);
        }
        
        // Обновить круговую диаграмму
        updatePieChart(analyticsData.categorySpending);
    }
    
    private void updatePieChart(AnalyticsData.CategorySpending[] categorySpending) {
        if (categorySpending == null || categorySpending.length == 0) {
            // Показать пустую диаграмму
            pieChart.setData(null);
            pieChart.invalidate();
            return;
        }
        
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        
        for (AnalyticsData.CategorySpending category : categorySpending) {
            if (category.amount > 0) {
                entries.add(new PieEntry((float) category.amount, category.category));
                colors.add(getCategoryColor(category.category));
            }
        }
        
        if (entries.isEmpty()) {
            pieChart.setData(null);
            pieChart.invalidate();
            return;
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);
        
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        
        pieChart.setData(data);
        pieChart.invalidate();
    }
    
    private int getCategoryColor(String category) {
        if (category == null) return Color.parseColor("#DDA0DD");
        
        switch (category.toLowerCase()) {
            case "еда":
            case "food":
                return getResources().getColor(R.color.category_food, null);
            case "транспорт":
            case "transport":
                return getResources().getColor(R.color.category_transport, null);
            case "покупки":
            case "shopping":
                return getResources().getColor(R.color.category_shopping, null);
            case "развлечения":
            case "entertainment":
                return getResources().getColor(R.color.category_entertainment, null);
            case "здоровье":
            case "health":
                return getResources().getColor(R.color.category_health, null);
            default:
                return getResources().getColor(R.color.category_other, null);
        }
    }
    
    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        pieChart.setVisibility(loading ? View.GONE : View.VISIBLE);
    }
}





