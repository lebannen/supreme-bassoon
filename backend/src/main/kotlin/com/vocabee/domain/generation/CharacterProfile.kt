package com.vocabee.domain.generation

data class CharacterProfile(
    val name: String,
    val description: String,
    val referenceImageUrl: String,
    val appearanceDetails: String // Additional details for image consistency
)
