package ru.tushkanov.rec

interface PermissionManager {

    enum class PermissionState {
        DENIED, BLACKLISTED, ALLOWED
    }

    fun requestPermission(permission: String)
    fun checkPermission(permission: String) : PermissionManager.PermissionState
}