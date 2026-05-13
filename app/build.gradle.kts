import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    kotlin("plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.tuempresa.chapacollectionapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tuempresa.chapacollectionapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 1. Cargamos las propiedades (usando el nombre 'props')
        val props = Properties()
        val propsFile = project.rootProject.file("local.properties")

        if (propsFile.exists()) {
            propsFile.inputStream().use { stream ->
                props.load(stream)
            }
        }

        // 2. IMPORTANTE: Usamos 'props' que es como definiste la variable arriba
        // Añadimos ?: "" para que no falle si el archivo está vacío
        buildConfigField("String", "SUPABASE_URL", "\"${props.getProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${props.getProperty("SUPABASE_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
    }
}

dependencies {
    // 1. LIFECYCLE & CORE
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // 2. COMPOSE (Centralizado con BOM para evitar versiones mezcladas)
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")

    // 3. NAVIGATION & HILT
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    //implementation(libs.firebase.firestore.ktx)

    // 5. IMAGES (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // 6. MAPS & GEODATA (Lo que necesitas para el GeoRepository)
    implementation(libs.play.services.maps)
    implementation("org.maplibre.gl:android-sdk:11.5.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // 7. OTRAS LIBS DE TU PROYECTO (Verifica que estas existan en libs.versions.toml)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.volley)
    implementation(libs.androidx.graphics.shapes.android)

    // DEBUG & TESTS
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Supabase
    val supabaseVersion = "2.5.0"
    //implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.0") // Base de datos
    //implementation("io.github.jan-tennert.supabase:storage-kt:2.5.0")   // Fotos
    implementation("io.ktor:ktor-client-android:2.3.11")               //
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")// Motor de red
    //implementation("io.github.jan-tennert.supabase:gotrue-kt:2.5.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabaseVersion")
    implementation("io.ktor:ktor-client-android:2.3.11")
}
