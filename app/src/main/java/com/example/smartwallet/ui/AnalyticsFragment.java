package com.example.smartwallet.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartwallet.R;
import com.example.smartwallet.chart.PercentBubblePieChartRenderer;
import com.example.smartwallet.network.dto.AnalyticsData;
import com.example.smartwallet.utils.TokenManager;
import com.example.smartwallet.utils.TransactionCategoryIcons;
import com.example.smartwallet.viewmodel.AnalyticsViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnalyticsFragment extends Fragment {

    private static final int[] ANALYTICS_SLICE_PALETTE = new int[]{
            Color.parseColor("#6AD314"),
            Color.parseColor("#4CDCE4"),
            Color.parseColor("#CCFB55"),
            Color.parseColor("#8AE85C"),
            Color.parseColor("#7DEBEC"),
            Color.parseColor("#E2FB7A"),
            Color.parseColor("#5BC41A"),
            Color.parseColor("#9AE66F"),
    };

    private PieChart pieChart;
    private FlexboxLayout legendCategories;
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
        legendCategories = view.findViewById(R.id.legendCategories);
        textTotalCashback = view.findViewById(R.id.textTotalCashback);
        textTrends = view.findViewById(R.id.textTrends);
        progress = view.findViewById(R.id.progress);
    }

    private void setupPieChart() {
        pieChart.setRenderer(new PercentBubblePieChartRenderer(
                pieChart, pieChart.getAnimator(), pieChart.getViewPortHandler()));

        pieChart.setUsePercentValues(true);

        pieChart.getDescription().setEnabled(false);
        float padL = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, getResources().getDisplayMetrics());
        float padR = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, getResources().getDisplayMetrics());
        float padT = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7f, getResources().getDisplayMetrics());
        float padB = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, getResources().getDisplayMetrics());
        pieChart.setExtraOffsets(padL, padT, padR, padB);
        pieChart.setMinOffset(0f);
        pieChart.setClipToPadding(false);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.brand_background));
        pieChart.setTransparentCircleColor(ContextCompat.getColor(requireContext(), R.color.brand_background));
        pieChart.setTransparentCircleAlpha(40);
        pieChart.setHoleRadius(76f);
        pieChart.setTransparentCircleRadius(79f);
        pieChart.setDrawCenterText(false);

        pieChart.setDrawEntryLabels(false);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);

        pieChart.setRotationEnabled(false);
        pieChart.setHighlightPerTapEnabled(true);
    }

    private void setupViewModel() {
        analyticsViewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        analyticsViewModel.setTokenManager(TokenManager.getInstance(requireContext()));

        analyticsViewModel.getAnalyticsData().observe(getViewLifecycleOwner(), analyticsData -> {
            if (analyticsData != null) {
                updateAnalytics(analyticsData);
            }
        });

        analyticsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            setLoading(isLoading);
        });

        analyticsViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                analyticsViewModel.clearError();
            }
        });

        analyticsViewModel.loadAnalytics();
    }

    private void updateAnalytics(AnalyticsData analyticsData) {
        textTotalCashback.setText(String.format("%.0f ₽", analyticsData.totalCashback));

        if (analyticsData.spendingTrends != null) {
            String trendText = String.format("Ты тратишь на еду на %.0f%% %s, чем месяц назад.",
                    analyticsData.spendingTrends.trendPercentage,
                    analyticsData.spendingTrends.foodTrend);
            textTrends.setText(trendText);
        }

        updatePieChart(analyticsData.categorySpending);
    }

    private void updatePieChart(AnalyticsData.CategorySpending[] categorySpending) {
        legendCategories.removeAllViews();

        if (categorySpending == null || categorySpending.length == 0) {
            pieChart.setData(null);
            pieChart.invalidate();
            legendCategories.setVisibility(View.GONE);
            return;
        }

        List<AnalyticsData.CategorySpending> sorted = new ArrayList<>();
        for (AnalyticsData.CategorySpending c : categorySpending) {
            if (c != null && c.amount > 0) {
                sorted.add(c);
            }
        }

        if (sorted.isEmpty()) {
            pieChart.setData(null);
            pieChart.invalidate();
            legendCategories.setVisibility(View.GONE);
            return;
        }

        sorted.sort((a, b) -> {
            int cmp = Double.compare(b.amount, a.amount);
            if (cmp != 0) {
                return cmp;
            }
            String na = a.category != null ? a.category : "";
            String nb = b.category != null ? b.category : "";
            return na.compareToIgnoreCase(nb);
        });

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> sliceColors = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {
            AnalyticsData.CategorySpending c = sorted.get(i);
            entries.add(new PieEntry((float) c.amount, ""));
            sliceColors.add(ANALYTICS_SLICE_PALETTE[i % ANALYTICS_SLICE_PALETTE.length]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(sliceColors);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(4f);
        dataSet.setValueLineColor(Color.TRANSPARENT);
        dataSet.setValueLineWidth(0f);
        // Меньше — подписи процентов ближе к кольцу (толщину бублика задаёт holeRadius)
        dataSet.setValueLinePart1Length(0.07f);
        dataSet.setValueLinePart2Length(0.05f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        pieChart.invalidate();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < sorted.size(); i++) {
            AnalyticsData.CategorySpending c = sorted.get(i);
            View chipRoot = inflater.inflate(
                    R.layout.item_analytics_category_legend, legendCategories, false);
            MaterialCardView row = chipRoot.findViewById(R.id.analyticsCategoryChip);
            int slice = sliceColors.get(i);
            row.setCardBackgroundColor(analyticsChipTrackColor(slice));
            View iconHost = chipRoot.findViewById(R.id.iconCircleHost);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(slice);
            iconHost.setBackground(circle);
            ImageView icon = chipRoot.findViewById(R.id.imageCategory);
            icon.setImageResource(TransactionCategoryIcons.getIconResId(c.category));
            TextView name = chipRoot.findViewById(R.id.textCategory);
            name.setText(formatCategoryLabel(c.category));
            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            lp.setFlexShrink(0f);
            legendCategories.addView(chipRoot, lp);
        }
        legendCategories.setVisibility(View.VISIBLE);
    }

    /** Светлая «дорожка» капсулы как на макете: смесь белого с цветом сектора. */
    private static int analyticsChipTrackColor(int sliceColor) {
        return ColorUtils.blendARGB(Color.WHITE, sliceColor, 0.26f);
    }

    @NonNull
    private static String formatCategoryLabel(@Nullable String categoryRaw) {
        if (categoryRaw == null || categoryRaw.isEmpty()) {
            return "";
        }
        String t = categoryRaw.trim();
        if (t.isEmpty()) {
            return "";
        }
        int firstCpEnd = t.offsetByCodePoints(0, 1);
        return t.substring(0, firstCpEnd).toUpperCase(Locale.getDefault()) + t.substring(firstCpEnd);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            pieChart.setVisibility(View.INVISIBLE);
            legendCategories.setVisibility(View.INVISIBLE);
        } else {
            pieChart.setVisibility(View.VISIBLE);
            legendCategories.setVisibility(legendCategories.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
    }
}
