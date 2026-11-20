// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kover) apply false
}

// Tarea personalizada para abrir el reporte Kover desde el panel de Gradle de Android Studio
tasks.register("abrirReporteKover") {
    group = "reporting"
    description = "Abre el reporte HTML de Kover en el navegador predeterminado"
    dependsOn(":app:koverHtmlReportDebug")
    doLast {
        val reportFile = file("app/build/reports/kover/htmlDebug/index.html")
        if (reportFile.exists()) {
            val os = System.getProperty("os.name").lowercase()
            when {
                os.contains("win") -> Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "", reportFile.absolutePath))
                os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", reportFile.absolutePath))
                else               -> Runtime.getRuntime().exec(arrayOf("xdg-open", reportFile.absolutePath))
            }
            println("✅ Reporte Kover abierto: ${reportFile.absolutePath}")
        } else {
            println("⚠️  El reporte no existe aún. Ejecuta primero: ./gradlew :app:koverHtmlReportDebug")
        }
    }
}