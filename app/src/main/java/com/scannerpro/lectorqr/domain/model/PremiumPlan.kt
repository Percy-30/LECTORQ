package com.scannerpro.lectorqr.domain.model

import com.scannerpro.lectorqr.R

enum class PremiumPlan(
    val id: String,
    val titleResId: Int,
    val price: String,
    val periodResId: Int,
    val isSubscription: Boolean
) {
    WEEKLY(
        id = "premium_weekly",
        titleResId = R.string.plan_weekly,
        price = "0.49 USD",
        periodResId = R.string.period_weekly,
        isSubscription = true
    ),
    MONTHLY(
        id = "premium_monthly",
        titleResId = R.string.plan_monthly,
        price = "1.99 USD",
        periodResId = R.string.period_monthly,
        isSubscription = true
    ),
    YEARLY(
        id = "premium_yearly",
        titleResId = R.string.plan_yearly,
        price = "4.99 USD",
        periodResId = R.string.period_yearly,
        isSubscription = true
    ),
    LIFETIME(
        id = "premium_lifetime",
        titleResId = R.string.plan_lifetime,
        price = "9.99 USD",
        periodResId = R.string.period_lifetime,
        isSubscription = false
    );

    companion object {
        fun fromId(id: String): PremiumPlan? = values().find { it.id == id }
    }
}
