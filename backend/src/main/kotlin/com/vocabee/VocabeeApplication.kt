package com.vocabee

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VocabeeApplication

fun main(args: Array<String>) {
    runApplication<VocabeeApplication>(*args)
}
