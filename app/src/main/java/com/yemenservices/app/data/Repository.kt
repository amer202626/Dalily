package com.yemenservices.app.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    private val settingDao = db.appSettingDao()
    private val categoryDao = db.categoryDao()
    private val providerDao = db.serviceProviderDao()

    // Settings
    val allSettings: Flow<List<AppSetting>> = settingDao.getAllSettings()

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun getSettingByKey(key: String): String? {
        return settingDao.getSettingByKey(key)?.value
    }

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    // Service Providers
    val allProviders: Flow<List<ServiceProvider>> = providerDao.getAllProviders()

    fun getProvidersByCategoryId(categoryId: Int): Flow<List<ServiceProvider>> {
        return providerDao.getProvidersByCategoryId(categoryId)
    }

    fun getProviderById(id: Int): Flow<ServiceProvider?> {
        return providerDao.getProviderById(id)
    }

    suspend fun insertProvider(provider: ServiceProvider) {
        providerDao.insertProvider(provider)
    }

    suspend fun updateProvider(provider: ServiceProvider) {
        providerDao.updateProvider(provider)
    }

    suspend fun deleteProvider(provider: ServiceProvider) {
        providerDao.deleteProvider(provider)
    }
}
