dependencies {
    implementation(project(":model"))
    implementation(project(":library:generator"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.97.Final")
}
