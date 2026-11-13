package com.example.myapplication.data

/** Deterministic, offline product-type visual fallback. Never pretends to be a product photo. */
object ProductVisualClassifier {
    private fun tokens(name: String): String = " " + name.lowercase(java.util.Locale.ROOT)
        .replace(Regex("([a-z])([0-9])"), "$1 $2")
        .replace(Regex("([0-9])([a-z])"), "$1 $2")
        .replace(Regex("[^a-z0-9]+"), " ").trim() + " "

    fun symbol(name: String): String {
        val t = tokens(name)
        fun has(vararg words: String): Boolean = words.any { " " + it + " " in t }
        // Type-specific signals override ingredient or flavour mentions.
        return when {
            has("water", "soda", "cordial", "lemonade", "cola", "kombucha", "juice", "smoothie", "milkshake") ||
                (has("soft") && has("drink")) || has("schweppes") -> "🥤"
            has("yoghurt", "yogurt") -> "🥣"
            has("jam", "marmalade") -> "🫙"
            has("syrup") -> "🍯"
            has("butter", "margarine") -> "🧈"
            has("cheese", "cheddar", "brie") -> "🧀"
            has("milk") -> "🥛"
            has("eggs", "egg") -> "🥚"
            has("chicken", "turkey") -> "🍗"
            has("beef", "steak", "mince", "pies") -> "🥩"
            has("fish", "salmon", "tuna") -> "🐟"
            has("potato", "potatoes") -> "🥔"
            has("mushroom", "mushrooms", "portobello") -> "🍄"
            has("onion", "onions") -> "🧅"
            has("garlic") -> "🧄"
            has("carrot", "carrots") -> "🥕"
            has("broccoli") -> "🥦"
            has("tomato", "tomatoes") -> "🍅"
            has("avocado", "avocados") -> "🥑"
            has("lettuce", "salad") -> "🥬"
            has("mandarin", "mandarins", "murcott", "orange", "oranges") -> "🍊"
            has("lemon", "lemons") -> "🍋"
            has("lime", "limes") -> "🍋"
            has("banana", "bananas") -> "🍌"
            has("kiwi", "kiwifruit") -> "🥝"
            has("apple", "apples") -> "🍎"
            has("mango", "mangoes") -> "🥭"
            has("strawberry", "strawberries") -> "🍓"
            has("blueberry", "blueberries", "blueberr", "blueberrs") -> "🫐"
            // No dedicated emoji for these fruits; generic produce is more honest than blueberries.
            has("blackberry", "blackberries", "blackberr", "blackberrs",
                "raspberry", "raspberries", "raspberr", "raspberrs") -> "🧺"
            has("grape", "grapes") -> "🍇"
            has("bread", "toast") -> "🍞"
            has("rice", "quinoa", "oats") -> "🌾"
            has("pasta", "spaghetti", "noodles") -> "🍝"
            has("coffee") -> "☕"
            has("tea") -> "🍵"
            has("chocolate", "cocoa") -> "🍫"
            has("honey") -> "🍯"
            has("toothpaste", "toothbrush") -> "🪥"
            has("shampoo", "conditioner", "soap", "lotion") -> "🧴"
            has("detergent", "cleaner", "bleach") -> "🧽"
            has("tissue", "tissues") || (has("toilet") && has("paper")) -> "🧻"
            else -> "📦"
        }
    }
}
