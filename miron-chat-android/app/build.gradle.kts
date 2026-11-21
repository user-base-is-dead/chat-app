plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val apiUrl = providers.gradleProperty("mironApiUrl").getOrElse("http://10.0.2.2:8080")
val webUrl = providers.gradleProperty("mironWebUrl").getOrElse(apiUrl)
val appVersionCode = providers.gradleProperty("mironVersionCode").getOrElse("2").toInt()
val appVersionName = providers.gradleProperty("mironVersionName").getOrElse("1.1")

val firebaseFile = file("google-services.json")
val firebase: Map<String, String> = if (firebaseFile.exists()) {
    val root = groovy.json.JsonSlurper().parse(firebaseFile) as Map<*, *>
    val project = root["project_info"] as Map<*, *>
    val client = (root["client"] as List<*>).map { it as Map<*, *> }.first {
        val info = it["client_info"] as Map<*, *>
        (info["android_client_info"] as Map<*, *>)["package_name"] == "com.mironchat.app"
    }
    mapOf(
        "FCM_APP_ID" to (client["client_info"] as Map<*, *>)["mobilesdk_app_id"].toString(),
        "FCM_API_KEY" to ((client["api_key"] as List<*>).first() as Map<*, *>)["current_key"].toString(),
        "FCM_PROJECT_ID" to project["project_id"].toString(),
        "FCM_SENDER_ID" to project["project_number"].toString(),
    )
} else {
    emptyMap()
}

android {
    namespace = "com.mironchat.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mironchat.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "API_URL", "\"$apiUrl\"")
        buildConfigField("String", "WEB_URL", "\"$webUrl\"")
        listOf("FCM_APP_ID", "FCM_API_KEY", "FCM_PROJECT_ID", "FCM_SENDER_ID").forEach { name ->
            buildConfigField("String", name, "\"${firebase[name].orEmpty()}\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("preview") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    debugImplementation(libs.compose.tooling)
}
