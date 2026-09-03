package com.example.myapplication.data

object ProductKnowledgeResolver {

    fun resolve(productName: String): ProductKnowledge {

        val name =
            productName
                .trim()
                .lowercase()

        return when {

            // ---------------------------------------------------------
            // Frozen products
            // Must be checked before general food categories.
            // ---------------------------------------------------------
            "frozen" in name ||
                    "ice cream" in name ->

                ProductKnowledge(
                    storageLocation = "Freezer",
                    foodCategory = "Frozen",
                    shoppingCategory = "Frozen",
                    suggestedShelfLifeDays = 180
                )

            // ---------------------------------------------------------
            // Long-life / shelf-stable milk and dairy
            // Must be checked before ordinary milk.
            // ---------------------------------------------------------
            "long life" in name ||
                    "long-life" in name ||
                    "uht" in name ->

                ProductKnowledge(
                    storageLocation = "Pantry",
                    foodCategory = "Dairy",
                    shoppingCategory = "Long Life",
                    suggestedShelfLifeDays = 180
                )

            // ---------------------------------------------------------
            // Fresh refrigerated dairy
            // ---------------------------------------------------------
            "milk" in name ||
                    "yoghurt" in name ||
                    "yogurt" in name ||
                    "cheese" in name ||
                    "cream" in name ||
                    "zymil" in name ||
                    "yakult" in name ->

                ProductKnowledge(
                    storageLocation = "Fridge",
                    foodCategory = "Dairy",
                    shoppingCategory = "Refrigerated",
                    suggestedShelfLifeDays = 10
                )

            // ---------------------------------------------------------
            // Refrigerated juices / smoothies
            // Brand/product clues seen on receipts.
            // ---------------------------------------------------------
            "veg blend" in name ||
                    "nudie" in name ||
                    "smoothie" in name ->

                ProductKnowledge(
                    storageLocation = "Fridge",
                    foodCategory = "Beverage",
                    shoppingCategory = "Refrigerated",
                    suggestedShelfLifeDays = 10
                )

            // ---------------------------------------------------------
            // Eggs
            // ---------------------------------------------------------
            "egg" in name ||
                    "eggs" in name ||
                    "sunny queen" in name ->

                ProductKnowledge(
                    storageLocation = "Fridge",
                    foodCategory = "Eggs",
                    shoppingCategory = "Refrigerated",
                    suggestedShelfLifeDays = 28
                )

            // ---------------------------------------------------------
            // Bakery
            //
            // Pantry is our DEFAULT. The user may later override
            // individual products to Fridge or Freezer.
            // ---------------------------------------------------------
            "bread" in name ||
                    "muffin" in name ||
                    "wrap" in name ||
                    "roll" in name ||
                    "croissant" in name ||
                    "sourdough" in name ||
                    "helga's" in name ||
                    "loaf" in name ->

                ProductKnowledge(
                    storageLocation = "Pantry",
                    foodCategory = "Bakery",
                    shoppingCategory = "Bakery",
                    suggestedShelfLifeDays = 7
                )

            // ---------------------------------------------------------
            // Fruit
            // ---------------------------------------------------------
            "banana" in name ||
                    "apple" in name ||
                    "orange" in name ||
                    "kiwifruit" in name ||
                    "blueberry" in name ||
                    "blueberries" in name ||
                    "mango" in name ||
                    "grape" in name ||
                    "pomegranate" in name ||
                    "pear" in name ->

                ProductKnowledge(
                    storageLocation = "Fridge",
                    foodCategory = "Fruit",
                    shoppingCategory = "Fresh Produce",
                    suggestedShelfLifeDays = 7
                )

            // ---------------------------------------------------------
            // Vegetables / fresh produce
            // ---------------------------------------------------------
            "carrot" in name ||
                    "broccoli" in name ||
                    "cauliflower" in name ||
                    "spinach" in name ||
                    "lettuce" in name ||
                    "capsicum" in name ||
                    "tomato" in name ||
                    "potato" in name ||
                    "onion" in name ||
                    "garlic" in name ||
                    "pumpkin" in name ||
                    "b'nut" in name ||
                    "salad" in name ||
                    "vitasoy" in name ||
                    "sld kit" in name ->

                ProductKnowledge(
                    storageLocation = "Fridge",
                    foodCategory = "Vegetables",
                    shoppingCategory = "Fresh Produce",
                    suggestedShelfLifeDays = 10
                )

            // ---------------------------------------------------------
            // Fresh meat / poultry
            // ---------------------------------------------------------
            "chicken" in name ||
                    "beef" in name ||
                    "pork" in name ||
                    "lamb" in name ||
                    "mince" in name ||
                    "steak" in name ->

                ProductKnowledge(
                    storageLocation = "Fridge",
                    foodCategory = "Meat",
                    shoppingCategory = "Meat",
                    suggestedShelfLifeDays = 3
                )

            // ---------------------------------------------------------
            // Pantry staples
            // ---------------------------------------------------------
            "oats" in name ||
                    "cereal" in name ||
                    "nutri grain" in name ||
                    "weet-bix" in name ||
                    "rice" in name ||
                    "pasta" in name ||
                    "flour" in name ||
                    "sugar" in name ||
                    "honey" in name ||
                    "peanut butter" in name ||
                    "pnt btr" in name ||
                    "oil" in name ||
                    "vinegar" in name ||
                    "baked beans" in name ||
                    "spaghetti" in name ||
                    "mix nuts" in name ||
                    "crunchy maple" in name ||
                    "crunchy nuts" in name ||
                    "carman" in name ||
                    "mayvers" in name ||
                    "chocola" in name ||
                    "tim tam" in name ||
                    "pesto" in name ->

                ProductKnowledge(
                    storageLocation = "Pantry",
                    foodCategory = "Pantry",
                    shoppingCategory = "Pantry",
                    suggestedShelfLifeDays = 180
                )

            // ---------------------------------------------------------
            // Shelf-stable drinks
            // ---------------------------------------------------------
            "juice" in name ||
                    "ginger beer" in name ||
                    "soft drink" in name ||
                    "schweppes" in name ||
                    "water" in name ->

                ProductKnowledge(
                    storageLocation = "Pantry",
                    foodCategory = "Beverage",
                    shoppingCategory = "Drinks",
                    suggestedShelfLifeDays = 90
                )

            // ---------------------------------------------------------
            // Unknown / fallback
            //
            // These values are defaults only. HouseholdCategoryResolver
            // can still classify the product as UNKNOWN.
            // ---------------------------------------------------------
            else ->

                ProductKnowledge(
                    storageLocation = "Pantry",
                    foodCategory = "General",
                    shoppingCategory = "General",
                    suggestedShelfLifeDays = 30
                )
        }
    }
}