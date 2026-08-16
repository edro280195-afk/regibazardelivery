package com.example.regibazarapp.data.local

import android.content.Context
import androidx.room.*

/**
 * ══════════════════════════════════════════════════════════════
 *  RouteDatabase — Modo Búnker (SQLite via Room)
 *
 *  Guarda la última ruta descargada como JSON blob.
 *  Si no hay internet al abrir la app, se carga desde aquí.
 * ══════════════════════════════════════════════════════════════
 */

@Entity(tableName = "route_cache")
data class RouteEntity(
    @PrimaryKey val token: String,
    val routeJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_routes_cache")
data class AdminRoutesEntity(
    @PrimaryKey val cacheKey: String = "routes",
    val routesJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface RouteDao {
    @Query("SELECT * FROM route_cache WHERE token = :token")
    suspend fun getRoute(token: String): RouteEntity?

    @Upsert
    suspend fun upsertRoute(route: RouteEntity)

    @Query("SELECT * FROM admin_routes_cache WHERE cacheKey = 'routes'")
    suspend fun getAdminRoutes(): AdminRoutesEntity?

    @Upsert
    suspend fun upsertAdminRoutes(routes: AdminRoutesEntity)
}

@Database(
    entities = [RouteEntity::class, AdminRoutesEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RouteDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile private var INSTANCE: RouteDatabase? = null

        fun getInstance(context: Context): RouteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RouteDatabase::class.java,
                    "regibazar_route.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
