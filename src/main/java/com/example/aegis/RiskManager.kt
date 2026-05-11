package com.example.aegis

class RiskManager {
    fun calculateRisk(hour: Int, battery: Int, isMoving: Boolean, isUnsafeLocation: Boolean): Int{
        var risk = 0
        // Time
        if (hour >= 22 || hour <= 5) {
            risk += 5
        } else {
            risk += 1
        }
        // Battery
        if (battery < 20) {
            risk += 4
        }
        // Movement
        if (isMoving) {
            risk += 3
        }
        if (isUnsafeLocation) {
            if (hour >= 22 || hour <= 5) {
                risk += 3   // Night + unsafe location
            } else {
                risk += 1   // Day + unsafe location
            }
        }
        return risk
    }
}






