/*
 * Copyright 2024 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
      namespace = "com.example.campaign_push_notification_test_app"

    defaultConfig {
        applicationId = "com.example.campaign_push_notification_test_app"
        compileSdk = BuildConstants.Versions.COMPILE_SDK_VERSION
        minSdk = BuildConstants.Versions.MIN_SDK_VERSION
        targetSdk = BuildConstants.Versions.TARGET_SDK_VERSION
        versionCode = BuildConstants.Versions.VERSION_CODE
        versionName = BuildConstants.Versions.VERSION_NAME
    }

    kotlinOptions {
        jvmTarget = BuildConstants.Versions.KOTLIN_JVM_TARGET
        languageVersion = BuildConstants.Versions.KOTLIN_LANGUAGE_VERSION
        apiVersion = BuildConstants.Versions.KOTLIN_API_VERSION
    }

    buildTypes {
        getByName(BuildConstants.BuildTypes.RELEASE)  {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        dataBinding = true
    }
}

apply(plugin = "com.google.gms.google-services")

dependencies {
    implementation(project(":campaign"))
    implementation("com.adobe.marketing.mobile:core:2.+")
    implementation("com.adobe.marketing.mobile:assurance:2.+")

    implementation(fileTree("libs").matching { include("*.jar") })
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.6.1")

    implementation("com.google.firebase:firebase-messaging:23.4.1")

}