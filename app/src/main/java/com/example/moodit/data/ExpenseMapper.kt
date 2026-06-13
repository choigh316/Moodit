package com.example.moodit.data

fun ExpenseRecord.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        amount = amount,
        category = category.name,
        foodDetail = subCategory.name,
        subCategory = subCategory.name,
        mood = mood.name,
        memo = memo,
        paymentMethod = paymentMethod,
        createdAt = createdAt
    )
}

fun ExpenseEntity.toRecord(): ExpenseRecord {
    return ExpenseRecord(
        id,
        amount,
        ExpenseCategory.valueOf(category),
        subCategoryFromEntity(category, subCategory, foodDetail),
        MoodType.valueOf(mood),
        memo,
        paymentMethod,
        createdAt
    )
}

private fun subCategoryFromEntity(
    categoryName: String,
    subCategoryName: String,
    oldFoodDetailName: String
): ExpenseSubCategory {
    val parsed = runCatching { ExpenseSubCategory.valueOf(subCategoryName) }.getOrNull()
    if (parsed != null && parsed != ExpenseSubCategory.NONE) return parsed

    return when (oldFoodDetailName) {
        "MEAL" -> ExpenseSubCategory.FOOD_DINING_OUT
        "DELIVERY_NIGHT" -> ExpenseSubCategory.FOOD_DELIVERY
        "CAFE_SNACK" -> ExpenseSubCategory.FOOD_SNACK
        "GROCERY" -> ExpenseSubCategory.FOOD_GROCERY
        else -> defaultSubCategory(ExpenseCategory.valueOf(categoryName))
    }
}

fun defaultSubCategory(category: ExpenseCategory): ExpenseSubCategory {
    return ExpenseSubCategory.values()
        .firstOrNull { it.parentCategory == category && it != ExpenseSubCategory.NONE }
        ?: ExpenseSubCategory.NONE
}
