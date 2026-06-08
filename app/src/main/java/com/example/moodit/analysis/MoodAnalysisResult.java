package com.example.moodit.analysis;

import com.example.moodit.data.ExpenseCategory;
import com.example.moodit.data.MoodType;

import java.util.List;
import java.util.Map;

public class MoodAnalysisResult {
    private final int totalAmount;
    private final int averagePerDay;
    private final Map<MoodType, Float> moodRatio;
    private final Map<ExpenseCategory, Integer> categoryTotal;
    private final Map<MoodType, Integer> moodTotal;
    private final int emotionalSpendingRate;
    private final List<String> reportMessages;

    public MoodAnalysisResult(
            int totalAmount,
            int averagePerDay,
            Map<MoodType, Float> moodRatio,
            Map<ExpenseCategory, Integer> categoryTotal,
            Map<MoodType, Integer> moodTotal,
            int emotionalSpendingRate,
            List<String> reportMessages
    ) {
        this.totalAmount = totalAmount;
        this.averagePerDay = averagePerDay;
        this.moodRatio = moodRatio;
        this.categoryTotal = categoryTotal;
        this.moodTotal = moodTotal;
        this.emotionalSpendingRate = emotionalSpendingRate;
        this.reportMessages = reportMessages;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getAveragePerDay() {
        return averagePerDay;
    }

    public Map<MoodType, Float> getMoodRatio() {
        return moodRatio;
    }

    public Map<ExpenseCategory, Integer> getCategoryTotal() {
        return categoryTotal;
    }

    public Map<MoodType, Integer> getMoodTotal() {
        return moodTotal;
    }

    public int getEmotionalSpendingRate() {
        return emotionalSpendingRate;
    }

    public List<String> getReportMessages() {
        return reportMessages;
    }
}