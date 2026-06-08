package com.example.moodit.analysis;

import com.example.moodit.data.ExpenseCategory;
import com.example.moodit.data.ExpenseRecord;
import com.example.moodit.data.ExpenseSubCategory;
import com.example.moodit.data.MoodType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MoodAnalysisEngine {
    private static final int MONTHLY_GOAL_AMOUNT = 400000;

    public static MoodAnalysisResult analyzeMonthly(
            List<ExpenseRecord> records,
            int daysInMonth
    ) {
        int totalAmount = calculateTotalAmount(records);
        int moodAnalysisAmount = calculateNonFixedTotalAmount(records);
        int averagePerDay = daysInMonth > 0 ? totalAmount / daysInMonth : 0;

        Map<MoodType, Integer> moodTotal = calculateMoodTotal(records);
        Map<ExpenseCategory, Integer> categoryTotal = calculateCategoryTotal(records);
        Map<MoodType, Float> moodRatio = calculateMoodRatio(moodTotal, moodAnalysisAmount);

        int sadAmount = moodTotal.get(MoodType.SAD);
        int stressAmount = moodTotal.get(MoodType.STRESS);
        int emotionalAmount = sadAmount + stressAmount;

        int emotionalSpendingRate = moodAnalysisAmount == 0
                ? 0
                : (int) (((float) emotionalAmount / moodAnalysisAmount) * 100);

        List<String> reportMessages = createReportMessages(
                records,
                totalAmount,
                categoryTotal,
                moodTotal,
                emotionalSpendingRate
        );

        return new MoodAnalysisResult(
                totalAmount,
                averagePerDay,
                moodRatio,
                categoryTotal,
                moodTotal,
                emotionalSpendingRate,
                reportMessages
        );
    }

    private static int calculateTotalAmount(List<ExpenseRecord> records) {
        int total = 0;

        for (ExpenseRecord record : records) {
            total += record.getAmount();
        }

        return total;
    }

    private static int calculateNonFixedTotalAmount(List<ExpenseRecord> records) {
        int total = 0;

        for (ExpenseRecord record : records) {
            if (!isFixedExpense(record)) {
                total += record.getAmount();
            }
        }

        return total;
    }

    private static Map<MoodType, Integer> calculateMoodTotal(List<ExpenseRecord> records) {
        Map<MoodType, Integer> result = new EnumMap<>(MoodType.class);

        for (MoodType mood : MoodType.values()) {
            result.put(mood, 0);
        }

        for (ExpenseRecord record : records) {
            if (isFixedExpense(record)) {
                continue;
            }

            MoodType mood = record.getMood();
            int currentAmount = result.get(mood);
            result.put(mood, currentAmount + record.getAmount());
        }

        return result;
    }

    private static Map<ExpenseCategory, Integer> calculateCategoryTotal(List<ExpenseRecord> records) {
        Map<ExpenseCategory, Integer> result = new EnumMap<>(ExpenseCategory.class);

        for (ExpenseCategory category : ExpenseCategory.values()) {
            result.put(category, 0);
        }

        for (ExpenseRecord record : records) {
            ExpenseCategory category = record.getCategory();
            int currentAmount = result.get(category);
            result.put(category, currentAmount + record.getAmount());
        }

        return result;
    }

    private static Map<MoodType, Float> calculateMoodRatio(
            Map<MoodType, Integer> moodTotal,
            int totalAmount
    ) {
        Map<MoodType, Float> result = new EnumMap<>(MoodType.class);

        for (MoodType mood : MoodType.values()) {
            if (totalAmount == 0) {
                result.put(mood, 0f);
            } else {
                int amount = moodTotal.get(mood);
                result.put(mood, (float) amount / totalAmount);
            }
        }

        return result;
    }

    private static List<String> createReportMessages(
            List<ExpenseRecord> records,
            int totalAmount,
            Map<ExpenseCategory, Integer> categoryTotal,
            Map<MoodType, Integer> moodTotal,
            int emotionalSpendingRate
    ) {
        List<String> messages = new ArrayList<>();

        if (records.isEmpty()) {
            messages.add("아직 분석할 소비 기록이 부족해요.");
            return messages;
        }

        ExpenseCategory topCategory = findTopCategory(categoryTotal);
        if (topCategory != null && categoryTotal.get(topCategory) > 0) {
            messages.add("이번 달에는 " + topCategory.getLabel() + " 지출이 가장 많아요.");
        }

        MoodType topMood = findTopMood(moodTotal);
        if (topMood != null && moodTotal.get(topMood) > 0) {
            messages.add(topMood.getLabel() + " 감정일 때 소비가 가장 많이 발생했어요.");
        }

        if (emotionalSpendingRate >= 60) {
            messages.add("후회 소비 비율이 높아요. 감정이 강할 때는 결제 전 잠깐 멈춰보세요.");
        } else if (emotionalSpendingRate >= 30) {
            messages.add("후회 소비가 조금 보여요. 스트레스가 큰 날에는 소비 한도를 정해보세요.");
        } else {
            messages.add("감정 소비 비율이 안정적인 편이에요.");
        }

        int riskScore = calculateRegretRiskScore(records);
        if (riskScore >= 80) {
            messages.add("밤 10시 이후 식비 지출 위험도가 높아요. 늦은 시간 배달이나 야식은 다음 날 다시 결정해보세요.");
        } else if (riskScore >= 40) {
            messages.add("늦은 시간 식비 소비가 조금 보여요. 저녁 이후에는 미리 정한 예산 안에서 써보세요.");
        }

        ExpenseSubCategory topSubCategory = findTopSubCategory(records);
        if (topSubCategory != null && topSubCategory != ExpenseSubCategory.NONE) {
            messages.add("세부 카테고리 중에서는 " + topSubCategory.getLabel() + " 소비가 가장 많이 보여요.");
        }

        int stressFoodAmount = calculateStressFoodAmount(records);

        if (totalAmount > 0) {
            float stressFoodRate = (float) stressFoodAmount / totalAmount;

            if (stressFoodRate >= 0.2f) {
                messages.add("스트레스 상태에서 식비 지출이 많은 편이에요. 야식이나 배달 소비를 조심해보세요.");
            }
        }

        return messages;
    }

    private static int calculateRegretRiskScore(List<ExpenseRecord> records) {
        int scoreTotal = 0;
        int scoreCount = 0;

        for (ExpenseRecord record : records) {
            if (!isFixedExpense(record)) {
                scoreTotal += calculateRecordRiskScore(record);
                scoreCount++;
            }
        }

        if (scoreCount == 0) {
            return 0;
        }

        int averageRecordRisk = scoreTotal / scoreCount;
        int totalRisk = averageRecordRisk
                + calculateGoalOveruseBonus(records)
                + calculateLateNightFoodRepeatBonus(records)
                + calculateMaxTimeConcentrationBonus(records);

        return Math.max(0, Math.min(100, totalRisk));
    }

    private static int calculateRecordRiskScore(ExpenseRecord record) {
        if (isFixedExpense(record)) {
            return 0;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(record.getCreatedAt());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        boolean lateNightFood = hour >= 22 && record.getCategory() == ExpenseCategory.FOOD;

        int score = 0;

        if (record.getMood() == MoodType.SAD || record.getMood() == MoodType.STRESS) {
            score += 25;
        }

        if (lateNightFood) {
            score += 20;
        }

        score += record.getSubCategory().getBaseRiskWeight();

        return Math.max(0, Math.min(100, score));
    }

    private static int calculateLateNightFoodRepeatBonus(List<ExpenseRecord> records) {
        int count = 0;
        Calendar calendar = Calendar.getInstance();

        for (ExpenseRecord record : records) {
            calendar.setTimeInMillis(record.getCreatedAt());

            if (!isFixedExpense(record)
                    && record.getCategory() == ExpenseCategory.FOOD
                    && calendar.get(Calendar.HOUR_OF_DAY) >= 22) {
                count++;
            }
        }

        return count <= 4 ? 0 : Math.min(32, (count - 4) * 8);
    }

    private static int calculateGoalOveruseBonus(List<ExpenseRecord> records) {
        int totalAmount = 0;

        for (ExpenseRecord record : records) {
            if (!isFixedExpense(record)) {
                totalAmount += record.getAmount();
            }
        }

        if (totalAmount <= MONTHLY_GOAL_AMOUNT) {
            return 0;
        }

        int overRate = (totalAmount - MONTHLY_GOAL_AMOUNT) * 100 / MONTHLY_GOAL_AMOUNT;
        return Math.min(35, 15 + overRate / 2);
    }

    private static int calculateMaxTimeConcentrationBonus(List<ExpenseRecord> records) {
        int totalAmount = 0;
        int[] timeAmounts = new int[4];
        Calendar calendar = Calendar.getInstance();

        for (ExpenseRecord record : records) {
            if (isFixedExpense(record)) {
                continue;
            }

            totalAmount += record.getAmount();
            calendar.setTimeInMillis(record.getCreatedAt());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int index;

            if (hour <= 5) {
                index = 0;
            } else if (hour <= 11) {
                index = 1;
            } else if (hour <= 17) {
                index = 2;
            } else {
                index = 3;
            }

            timeAmounts[index] += record.getAmount();
        }

        if (totalAmount <= 0) {
            return 0;
        }

        int maxBonus = 0;

        for (int amount : timeAmounts) {
            int share = amount * 100 / totalAmount;
            int bonus;

            if (share >= 60) {
                bonus = 25;
            } else if (share >= 45) {
                bonus = 15;
            } else if (share >= 35) {
                bonus = 8;
            } else {
                bonus = 0;
            }

            maxBonus = Math.max(maxBonus, bonus);
        }

        return maxBonus;
    }

    private static boolean isFixedExpense(ExpenseRecord record) {
        String memo = record.getMemo() == null ? "" : record.getMemo();

        return record.getSubCategory() == ExpenseSubCategory.LIVING_RENT
                || record.getSubCategory() == ExpenseSubCategory.LIVING_PHONE
                || record.getSubCategory() == ExpenseSubCategory.LIVING_SUBSCRIPTION
                || record.getSubCategory() == ExpenseSubCategory.CULTURE_OTT
                || record.getSubCategory() == ExpenseSubCategory.ETC_TAX
                || (record.getCategory() == ExpenseCategory.LIVING
                    && record.getSubCategory() == ExpenseSubCategory.NONE)
                || memo.contains("월세")
                || memo.contains("통신비")
                || memo.contains("세금")
                || memo.contains("구독");
    }

    private static ExpenseSubCategory findTopSubCategory(List<ExpenseRecord> records) {
        Map<ExpenseSubCategory, Integer> totals = new EnumMap<>(ExpenseSubCategory.class);

        for (ExpenseSubCategory subCategory : ExpenseSubCategory.values()) {
            totals.put(subCategory, 0);
        }

        for (ExpenseRecord record : records) {
            ExpenseSubCategory subCategory = record.getSubCategory();
            totals.put(subCategory, totals.get(subCategory) + record.getAmount());
        }

        ExpenseSubCategory topSubCategory = null;
        int maxAmount = 0;

        for (ExpenseSubCategory subCategory : ExpenseSubCategory.values()) {
            int amount = totals.get(subCategory);

            if (subCategory != ExpenseSubCategory.NONE && amount > maxAmount) {
                maxAmount = amount;
                topSubCategory = subCategory;
            }
        }

        return topSubCategory;
    }

    private static ExpenseCategory findTopCategory(Map<ExpenseCategory, Integer> categoryTotal) {
        ExpenseCategory topCategory = null;
        int maxAmount = 0;

        for (ExpenseCategory category : ExpenseCategory.values()) {
            int amount = categoryTotal.get(category);

            if (amount > maxAmount) {
                maxAmount = amount;
                topCategory = category;
            }
        }

        return topCategory;
    }

    private static MoodType findTopMood(Map<MoodType, Integer> moodTotal) {
        MoodType topMood = null;
        int maxAmount = 0;

        for (MoodType mood : MoodType.values()) {
            int amount = moodTotal.get(mood);

            if (amount > maxAmount) {
                maxAmount = amount;
                topMood = mood;
            }
        }

        return topMood;
    }

    private static int calculateStressFoodAmount(List<ExpenseRecord> records) {
        int total = 0;

        for (ExpenseRecord record : records) {
            if (record.getMood() == MoodType.STRESS
                    && record.getCategory() == ExpenseCategory.FOOD) {
                total += record.getAmount();
            }
        }

        return total;
    }
}
