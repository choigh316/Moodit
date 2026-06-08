package com.example.moodit.data;

public enum ExpenseSubCategory {
    NONE(ExpenseCategory.ETC, "미분류", "❔", 0),

    FOOD_DINING_OUT(ExpenseCategory.FOOD, "외식", "🍽️", 18),
    FOOD_DELIVERY(ExpenseCategory.FOOD, "배달", "🛵", 32),
    FOOD_GROCERY(ExpenseCategory.FOOD, "장보기", "🛒", -15),
    FOOD_SNACK(ExpenseCategory.FOOD, "간식", "🍪", 18),
    FOOD_LATE_NIGHT(ExpenseCategory.FOOD, "야식", "🌙", 45),

    CAFE_COFFEE(ExpenseCategory.CAFE, "커피", "☕", 12),
    CAFE_DRINK(ExpenseCategory.CAFE, "음료", "🥤", 12),
    CAFE_DESSERT(ExpenseCategory.CAFE, "디저트", "🍰", 20),

    TRANSPORT_PUBLIC(ExpenseCategory.TRANSPORT, "대중교통", "🚇", -10),
    TRANSPORT_TAXI(ExpenseCategory.TRANSPORT, "택시", "🚕", 25),
    TRANSPORT_FUEL(ExpenseCategory.TRANSPORT, "주유", "⛽", 0),
    TRANSPORT_PARKING(ExpenseCategory.TRANSPORT, "주차", "🅿️", 5),
    TRANSPORT_TOLL(ExpenseCategory.TRANSPORT, "통행료", "🛣️", 0),
    TRANSPORT_ETC(ExpenseCategory.TRANSPORT, "기타교통", "🚦", 5),

    SHOPPING_CLOTHES(ExpenseCategory.SHOPPING, "의류", "👕", 20),
    SHOPPING_ELECTRONICS(ExpenseCategory.SHOPPING, "전자기기", "💻", 30),
    SHOPPING_BEAUTY(ExpenseCategory.SHOPPING, "화장품", "💄", 18),
    SHOPPING_HOBBY_GOODS(ExpenseCategory.SHOPPING, "취미용품", "🧩", 25),
    SHOPPING_DAILY_GOODS(ExpenseCategory.SHOPPING, "생활용품", "🧻", -5),

    GAME_GAME(ExpenseCategory.GAME, "게임", "🎮", 35),
    GAME_EXERCISE(ExpenseCategory.GAME, "운동", "🏃", -5),
    GAME_READING(ExpenseCategory.GAME, "독서", "📚", -10),
    GAME_CRAFT(ExpenseCategory.GAME, "공예", "🧵", 5),
    GAME_COLLECTING(ExpenseCategory.GAME, "수집", "🧸", 30),
    GAME_ETC(ExpenseCategory.GAME, "기타취미", "✨", 10),

    CULTURE_MOVIE(ExpenseCategory.CULTURE, "영화", "🎬", 5),
    CULTURE_PERFORMANCE(ExpenseCategory.CULTURE, "공연", "🎭", 10),
    CULTURE_EXHIBITION(ExpenseCategory.CULTURE, "전시", "🖼️", 5),
    CULTURE_CONCERT(ExpenseCategory.CULTURE, "콘서트", "🎤", 18),
    CULTURE_OTT(ExpenseCategory.CULTURE, "OTT", "📺", 12),

    LIVING_RENT(ExpenseCategory.LIVING, "월세", "🏢", -25),
    LIVING_MEDICAL(ExpenseCategory.LIVING, "병원", "🏥", -20),
    LIVING_PHONE(ExpenseCategory.LIVING, "통신비", "📱", -10),
    LIVING_SUBSCRIPTION(ExpenseCategory.LIVING, "구독서비스", "🔁", 15),
    LIVING_EDUCATION(ExpenseCategory.LIVING, "교육", "✏️", -15),
    LIVING_BEAUTY(ExpenseCategory.LIVING, "미용", "💇", 15),
    LIVING_FURNITURE_APPLIANCE(ExpenseCategory.LIVING, "가구/가전", "🛋️", 10),

    ETC_FAMILY_EVENT(ExpenseCategory.ETC, "경조사", "💌", -5),
    ETC_TAX(ExpenseCategory.ETC, "세금", "🧾", -20),
    ETC_UNCLASSIFIED(ExpenseCategory.ETC, "미분류", "📦", 0);

    private final ExpenseCategory parentCategory;
    private final String label;
    private final String emoji;
    private final int baseRiskWeight;

    ExpenseSubCategory(ExpenseCategory parentCategory, String label, String emoji, int baseRiskWeight) {
        this.parentCategory = parentCategory;
        this.label = label;
        this.emoji = emoji;
        this.baseRiskWeight = baseRiskWeight;
    }

    public ExpenseCategory getParentCategory() {
        return parentCategory;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getBaseRiskWeight() {
        return baseRiskWeight;
    }
}
