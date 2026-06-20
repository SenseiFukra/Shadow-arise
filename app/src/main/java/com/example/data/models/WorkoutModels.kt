package com.example.data.models

data class Exercise(
    val id: Int,
    val name: String,
    val category: String, // STRENGTH, CORE, CARDIO, FLEXIBILITY
    val musclesWorked: List<String>,
    val steps: List<String>
)

data class GeneratedExerciseState(
    val exercise: Exercise,
    val sets: Int,
    val repsOrDuration: String,
    val restSeconds: Int,
    val difficultyLabel: String
)
