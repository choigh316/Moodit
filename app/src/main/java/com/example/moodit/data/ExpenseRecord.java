package com.example.moodit.data;

public class ExpenseRecord {
    private final long id;
    private final int amount;
    private final ExpenseCategory category;
    private final ExpenseSubCategory subCategory;
    private final MoodType mood;
    private final String memo;
    private final String paymentMethod;
    private final long createdAt;

    public ExpenseRecord(
            long id,
            int amount,
            ExpenseCategory category,
            MoodType mood,
            String memo,
            long createdAt
    ) {
        this(id, amount, category, ExpenseSubCategory.NONE, mood, memo, createdAt);
    }

    public ExpenseRecord(
            long id,
            int amount,
            ExpenseCategory category,
            ExpenseSubCategory subCategory,
            MoodType mood,
            String memo,
            long createdAt
    ) {
        this(id, amount, category, subCategory, mood, memo, "카드", createdAt);
    }

    public ExpenseRecord(
            long id,
            int amount,
            ExpenseCategory category,
            ExpenseSubCategory subCategory,
            MoodType mood,
            String memo,
            String paymentMethod,
            long createdAt
    ) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.subCategory = subCategory;
        this.mood = mood;
        this.memo = memo;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public ExpenseSubCategory getSubCategory() {
        return subCategory;
    }

    public MoodType getMood() {
        return mood;
    }

    public String getMemo() {
        return memo;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
