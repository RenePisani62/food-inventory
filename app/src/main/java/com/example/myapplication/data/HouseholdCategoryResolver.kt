package com.example.myapplication.data

object HouseholdCategoryResolver {

    fun resolve(
        productName: String
    ): HouseholdCategory {

        val name =
            productName
                .trim()
                .lowercase()

        // Compact representation handles receipt extraction such as:
        // "Emery Board", "EmeryBoard", "emery-board", etc.
        val compactName =
            name.replace(
                Regex("[^a-z0-9]"),
                ""
            )

        fun containsAny(
            vararg terms: String
        ): Boolean {

            return terms.any { term ->

                val compactTerm =
                    term
                        .lowercase()
                        .replace(
                            Regex("[^a-z0-9]"),
                            ""
                        )

                compactName.contains(compactTerm)
            }
        }

        return when {

            // Bathroom / personal care
            containsAny(
                "emery board",
                "toothbrush",
                "toothpaste",
                "mouthwash",
                "shampoo",
                "conditioner",
                "deodorant",
                "razor",
                "shaving cream",
                "body wash",
                "soap"
            ) ->
                HouseholdCategory.BATHROOM

            // Laundry
            containsAny(
                "laundry detergent",
                "laundry liquid",
                "laundry powder",
                "fabric softener",
                "fabric conditioner",
                "stain remover",
                "laundry sanitiser"
            ) ->
                HouseholdCategory.LAUNDRY

            // Cleaning
            containsAny(
                "bleach",
                "surface cleaner",
                "glass cleaner",
                "floor cleaner",
                "disinfectant",
                "dishwashing liquid",
                "dishwasher tablet",
                "dishwasher powder",
                "cleaning spray"
            ) ->
                HouseholdCategory.CLEANING

            // Pet
            containsAny(
                "pet food",
                "pet mince",
                "pet chicken",
                "premiyum pet",
                "dog food",
                "cat food",
                "dog treats",
                "cat treats",
                "kitty litter",
                "cat litter",
                "bow wow"
            ) ->
                HouseholdCategory.PET

            // General household
            containsAny(
                "garbage bag",
                "bin liner",
                "baking paper",
                "aluminium foil",
                "paper towel",
                "toilet paper",
                "tissue",
                "battery",
                "batteries",
                "glad k/t",
                "glad"
            ) ->
                HouseholdCategory.HOUSEHOLD

            // Common food terms found in abbreviated receipt descriptions
            containsAny(
                "grn goddess",
                "sld kit",
                "rana",
                "jordan crunchy",
                "jordans crunchy",
                "schweppes",
                "baked beans",
                "wrigley extra",
                "mix nuts",
                "whittakers",
                "tim tam"
            ) ->
                HouseholdCategory.FOOD

            // Existing food knowledge gets the final say on known food.
            ProductKnowledgeResolver
                .resolve(productName)
                .foodCategory != "General" ->

                HouseholdCategory.FOOD

            else ->
                HouseholdCategory.UNKNOWN
        }
    }
}