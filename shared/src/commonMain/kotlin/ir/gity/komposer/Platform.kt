package ir.gity.komposer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform