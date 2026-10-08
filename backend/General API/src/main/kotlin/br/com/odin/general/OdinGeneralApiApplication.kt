package br.com.odin.general

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OdinGeneralApiApplication

fun main(args: Array<String>) {
    runApplication<OdinGeneralApiApplication>(*args)
}
