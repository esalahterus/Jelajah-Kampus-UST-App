plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.jelajahkampusust"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.ac.ustjogja.informatika.JelajahKampusUST"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // Untuk Laragon di komputer saat memakai emulator Android Studio.
            manifestPlaceholders["usesCleartextTraffic"] = "true"

            buildConfigField(
                "String",
                "BASE_URL",
                "\"http://10.0.2.2/jelajah-kampus-ust/\""
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.8.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}
