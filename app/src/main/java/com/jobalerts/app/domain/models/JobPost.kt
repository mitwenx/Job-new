package com.jobalerts.app.domain.models
data class JobPost(
val id: String,
val category: String,
val title: String,
val qualificationTag: String,
val totalVacancies: String,
val qualification: String,
val ageLimit: String,
val fee: String,
val startDate: String,
val lastDate: String,
val examDate: String,
val applyLink: String,
val pdfLink: String,
val description: String
)