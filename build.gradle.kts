tasks.register("Delete", Delete::class) {
    description = "Deletes the build directory."
    delete(rootProject.layout.buildDirectory)
}