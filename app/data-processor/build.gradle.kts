configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-reload4j")
}

dependencies {
    implementation(project(":model"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.apache.parquet:parquet-avro:1.14.4")
    implementation("org.apache.hadoop:hadoop-common:3.3.6")
    implementation("org.apache.hadoop:hadoop-mapreduce-client-core:3.3.6")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
