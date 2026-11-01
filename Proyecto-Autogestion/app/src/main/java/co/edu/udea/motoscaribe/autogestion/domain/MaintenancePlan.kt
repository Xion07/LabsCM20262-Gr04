package co.edu.udea.motoscaribe.autogestion.domain

enum class MaintenanceStatus { OK, DUE_SOON, OVERDUE }

/** Plan de cambio de aceite a partir del kilometraje acumulado de la moto. */
data class MaintenancePlan(
    val currentKm: Int,
    val lastServiceKm: Int,
    val intervalKm: Int = DEFAULT_INTERVAL_KM,
) {
    val nextServiceKm: Int get() = lastServiceKm + intervalKm
    val kmLeft: Int get() = nextServiceKm - currentKm
    val progress: Float
        get() = ((currentKm - lastServiceKm).toFloat() / intervalKm).coerceIn(0f, 1f)
    val status: MaintenanceStatus
        get() = when {
            kmLeft < 0 -> MaintenanceStatus.OVERDUE
            kmLeft <= WARNING_KM -> MaintenanceStatus.DUE_SOON
            else -> MaintenanceStatus.OK
        }

    companion object {
        const val DEFAULT_INTERVAL_KM = 3_000
        const val WARNING_KM = 200
    }
}
