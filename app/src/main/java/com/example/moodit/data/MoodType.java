package com.example.moodit.data;

public enum MoodType {
    HAPPY("행복", "😊", 2),
    NORMAL("보통", "😐", 0),
    SAD("우울", "😢", -1),
    STRESS("스트레스", "😠", -2);

    private final String label;
    private final String emoji;
    private final int score;

    MoodType(String label, String emoji, int score) {
        this.label = label;
        this.emoji = emoji;
        this.score = score;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getScore() {
        return score;
    }
}
