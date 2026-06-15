pluginManagement {
    repositories {
        val isCi = System.getenv("GITHUB_ACTIONS") == "true"
        if (!isCi) {
            maven { url = uri("https://maven.aliyun.com/repository/public/") }
            maven { url = uri("https://maven.aliyun.com/repository/google/") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val isCi = System.getenv("GITHUB_ACTIONS") == "true"
        if (!isCi) {
            maven { url = uri("https://maven.aliyun.com/repository/public/") }
            maven { url = uri("https://maven.aliyun.com/repository/google/") }
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "JapaneseGrammarApp"
include(":app")
