package com.pdv.data

data class User(
    val id: Long = 0,
    val username: String,
    val password: String, // Em produção, use hash (BCrypt)
    val fullName: String,
    val role: UserRole,
    val active: Boolean = true,
    val createdAt: String = ""
)

enum class UserRole(
    val displayName: String,
    val permissions: Set<Permission>
) {
    ADMIN(
        "Administrador",
        setOf(
            Permission.VIEW_SALES,
            Permission.MAKE_SALES,
            Permission.CANCEL_SALES,
            Permission.VIEW_PRODUCTS,
            Permission.ADD_PRODUCTS,
            Permission.EDIT_PRODUCTS,
            Permission.DELETE_PRODUCTS,
            Permission.VIEW_REPORTS,
            Permission.VIEW_SETTINGS,
            Permission.MANAGE_USERS
        )
    ),
    MANAGER(
        "Gerente",
        setOf(
            Permission.VIEW_SALES,
            Permission.MAKE_SALES,
            Permission.CANCEL_SALES,
            Permission.VIEW_PRODUCTS,
            Permission.ADD_PRODUCTS,
            Permission.EDIT_PRODUCTS,
            Permission.VIEW_REPORTS,
            Permission.VIEW_SETTINGS
        )
    ),
    CASHIER(
        "Caixa",
        setOf(
            Permission.VIEW_SALES,
            Permission.MAKE_SALES,
            Permission.VIEW_PRODUCTS
        )
    ),
    STOCK(
        "Estoquista",
        setOf(
            Permission.VIEW_PRODUCTS,
            Permission.ADD_PRODUCTS,
            Permission.EDIT_PRODUCTS
        )
    );

    fun hasPermission(permission: Permission): Boolean {
        return permissions.contains(permission)
    }
}

enum class Permission {
    VIEW_SALES,
    MAKE_SALES,
    CANCEL_SALES,
    VIEW_PRODUCTS,
    ADD_PRODUCTS,
    EDIT_PRODUCTS,
    DELETE_PRODUCTS,
    VIEW_REPORTS,
    VIEW_SETTINGS,
    MANAGE_USERS
}

// Sessão do usuário logado
object UserSession {
    private var currentUser: User? = null

    fun login(user: User) {
        currentUser = user
    }

    fun logout() {
        currentUser = null
    }

    fun isLoggedIn(): Boolean = currentUser != null

    fun getCurrentUser(): User? = currentUser

    fun hasPermission(permission: Permission): Boolean {
        return currentUser?.role?.hasPermission(permission) ?: false
    }

    fun isAdmin(): Boolean = currentUser?.role == UserRole.ADMIN

    fun requirePermission(permission: Permission): Boolean {
        if (!hasPermission(permission)) {
            throw SecurityException("Usuário sem permissão: ${permission.name}")
        }
        return true
    }
}

