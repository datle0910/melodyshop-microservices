package com.melodyshop.ai.domain.model;

public enum IntentType {
    /** User wants to browse/search/list products */
    PRODUCT_LIST,
    /** User wants stock/inventory information */
    STOCK_CHECK,
    /** User wants product details */
    PRODUCT_DETAIL,
    /** User wants to add to cart or buy */
    ADD_TO_CART,
    /** General conversation, questions, greetings */
    GENERAL,
    /** User wants to know about categories */
    CATEGORY_LIST,
    /** User wants to search for something specific */
    PRODUCT_SEARCH,
    /** Fallback for unclassified messages */
    UNKNOWN
}
