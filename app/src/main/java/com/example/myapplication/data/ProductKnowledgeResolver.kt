package com.example.myapplication.data

object ProductKnowledgeResolver {

    fun resolve(productName: String): ProductKnowledge {

        val name =
            productName.lowercase()

        return when {

            "long life" in name ||
                    "uht" in name ->

                ProductKnowledge(

                    storageLocation = "Pantry",

                    foodCategory = "Dairy",

                    shoppingCategory = "Long Life",

                    suggestedShelfLifeDays = 180
                )

            "milk" in name ->

                ProductKnowledge(

                    storageLocation = "Fridge",

                    foodCategory = "Dairy",

                    shoppingCategory = "Refrigerated",

                    suggestedShelfLifeDays = 14
                )

            "bread" in name ->

                ProductKnowledge(

                    storageLocation = "Pantry",

                    foodCategory = "Bakery",

                    shoppingCategory = "Bakery",

                    suggestedShelfLifeDays = 5
                )

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