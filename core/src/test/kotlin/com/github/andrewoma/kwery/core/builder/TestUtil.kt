package com.github.andrewoma.kwery.core.builder

fun String.trimQuery() = this
    .trimIndent()
    .split("\n")
    .fold("") { acc, r -> "$acc\n${r.trim()}" }
