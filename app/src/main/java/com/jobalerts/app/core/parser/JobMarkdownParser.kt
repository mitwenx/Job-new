package com.jobalerts.app.core.parser
import com.jobalerts.app.domain.models.JobPost
object JobMarkdownParser {
fun parse(markdown: String, id: String, category: String, title: String, qualTag: String, lastDate: String): JobPost {
var tv = ""; var qu = ""; var age = ""; var fee = ""
var sd = ""; var ld = lastDate; var ed = ""
var link = ""; var pdf = ""
val descriptionBuilder = StringBuilder()
var parsingMetadata = true
markdown.lines().forEach { line ->
if (parsingMetadata) {
when {
line.startsWith("TV ") -> tv = line.removePrefix("TV ").trim()
line.startsWith("QU ") -> qu = line.removePrefix("QU ").trim()
line.startsWith("AGE ") -> age = line.removePrefix("AGE ").trim()
line.startsWith("FEE ") -> fee = line.removePrefix("FEE ").trim()
line.startsWith("SD ") -> sd = line.removePrefix("SD ").trim()
line.startsWith("LD ") -> ld = line.removePrefix("LD ").trim()
line.startsWith("ED ") -> ed = line.removePrefix("ED ").trim()
line.startsWith("LINK ") -> link = line.removePrefix("LINK ").trim()
line.startsWith("PDF ") -> pdf = line.removePrefix("PDF ").trim()
line.isBlank() -> parsingMetadata = false
}
} else {
descriptionBuilder.append(line).append("\n")
}
}
return JobPost(
id = id, category = category, title = title, qualificationTag = qualTag,
totalVacancies = tv, qualification = qu, ageLimit = age, fee = fee,
startDate = sd, lastDate = ld, examDate = ed, applyLink = link,
pdfLink = pdf, description = descriptionBuilder.toString().trim()
)
}
}