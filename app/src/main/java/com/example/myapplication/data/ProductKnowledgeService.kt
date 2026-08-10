package com.example.myapplication.data

class ProductKnowledgeService {

    fun resolve(
        productName: String
    ): ProductKnowledge {

        return ProductKnowledgeResolver
            .resolve(productName)
    }
}