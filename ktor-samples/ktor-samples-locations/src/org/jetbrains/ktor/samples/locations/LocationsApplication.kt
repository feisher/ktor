package org.jetbrains.ktor.samples.locations

import kotlinx.html.*
import kotlinx.html.stream.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.locations.*
import java.util.*

at("/") data class index()
at("/number") data class number(val value: Int)

class LocationsApplication(config: ApplicationConfig) : Application(config) {
    init {
        locations {
            get<index>() {
                contentType(ContentType.Text.Html)
                contentStream {
                    appendHTML().html {
                        head {
                            title { +"Numbers" }
                        }
                        body {
                            h1 {
                                +"Choose a Number"
                            }
                            ul {
                                val rnd = Random()
                                (0..5).forEach {
                                    li {
                                        val number = number(rnd.nextInt(1000))
                                        a(href = Locations.href(number)) {
                                            +"Number #${number.value}"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            get<number>() { number ->
                contentType(ContentType.Text.Html)
                contentStream {
                    appendHTML().html {
                        head {
                            title { +"Numbers" }
                        }
                        body {
                            h1 {
                                +"Number is ${number.value}"
                            }
                        }
                    }
                }
            }
        }
    }
}