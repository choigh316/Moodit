package com.example.moodit.data;

public enum ExpenseCategory {
    FOOD("식비", "🍚"),
    SHOPPING("쇼핑", "🛍️"),
    TRANSPORT("교통", "🚌"),
    CAFE("카페", "☕"),
    GAME("취미", "🎮"),
    CULTURE("문화", "🎬"),
    LIVING("생활", "🏠"),
    ETC("기타", "📦");

    private final String label;
    private final String emoji;

    ExpenseCategory(String label, String emoji) {
        this.label = label;
        this.emoji = emoji;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }
}
