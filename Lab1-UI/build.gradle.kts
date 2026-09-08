// Archivo de build de nivel superior. Las versiones de los plugins se declaran
// aqui una sola vez y los modulos las aplican sin version.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
