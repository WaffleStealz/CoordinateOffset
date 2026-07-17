group = "com.jtprince.coordinateoffset.example"
version = "0.0.1"

dependencies {

    compileOnly(libs.paper.api)
    compileOnly(project(":api"))
}

tasks {
    jar {
        archiveBaseName.set("CoordinateOffsetAPIExamplePlugin")
    }
}
