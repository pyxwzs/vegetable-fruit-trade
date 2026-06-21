package com.trade.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MenuKeyConstants {

    public static final String DASHBOARD = "dashboard";
    public static final String PURCHASE = "purchase";
    public static final String SALES = "sales";
    public static final String PRODUCTS = "products";
    public static final String INVENTORY = "inventory";
    public static final String SUPPLIERS = "suppliers";
    public static final String SUPPLIER_PRODUCT_METRICS = "supplier-product-metrics";
    public static final String CUSTOMERS = "customers";
    public static final String EXPENSES = "expenses";
    public static final String MONTHLY_REPORT = "monthly-report";
    public static final String PARTNER_PRODUCT_STATS = "partner-product-stats";

    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            DASHBOARD,
            PURCHASE,
            SALES,
            PRODUCTS,
            INVENTORY,
            SUPPLIERS,
            SUPPLIER_PRODUCT_METRICS,
            CUSTOMERS,
            EXPENSES,
            MONTHLY_REPORT,
            PARTNER_PRODUCT_STATS
    ));

    private MenuKeyConstants() {
    }
}
