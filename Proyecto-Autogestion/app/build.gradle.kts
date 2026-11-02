import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firma del APK de release. Si existe keystore.properties en la raiz del proyecto (no versionado,
// igual que el keystore) el release sale firmado; si no, queda sin firmar. Ver README.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

/** API de la demo en Azure (plan gratis). Cualquier build, tambien al clonar el repo, apunta aqui. */
val azureApiUrl = "https://autogestion-api-jc.azurewebsites.net"

/**
 * URL de la API segun las propiedades de Gradle:
 *  - sin propiedades: Azure;
 *  - -PapiBaseUrl=http://10.0.2.2:8080: API local (el emulador ve el computador en 10.0.2.2);
 *  - -PuseMock=true: datos simulados, sin API.
 */
fun Project.apiBaseUrl(): String = when {
    findProperty("useMock") == "true" -> ""
    else -> (findProperty("apiBaseUrl") as String?) ?: azureApiUrl
}

android {
    namespace = "co.edu.udea.motoscaribe.autogestion"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.edu.udea.motoscaribe.autogestion"
        minSdk = 26 // java.time disponible sin desugaring
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl()}\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl()}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    // Splash nativa (API 21+ con la misma apariencia): fondo de marca + moto, sin corte blanco al abrir.
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    // Iconografia completa (Rounded/Outlined) para la barra de navegacion y las tarjetas; R8 descarta lo no usado.
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
