plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // ... other configurations remain unchanged ...
    buildFeatures { compose = true }
    // Add back this block, it is required for Kotlin 1.9.x:
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

// dependencies remain unchanged