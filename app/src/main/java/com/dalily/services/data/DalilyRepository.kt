package com.dalily.services.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DalilyRepository(private val dao: DalilyDao) {

    // Exposure of Flows
    val categories: Flow<List<Category>> = dao.getCategories()
    val serviceProviders: Flow<List<ServiceProvider>> = dao.getServiceProviders()
    val auditLogs: Flow<List<AuditLog>> = dao.getAuditLogs()
    val chatMessages: Flow<List<ChatMessage>> = dao.getAllChatMessages()
    val banners: Flow<List<Banner>> = dao.getAllBanners()
    val activeBanners: Flow<List<Banner>> = dao.getActiveBanners()
    val appSettings: Flow<AppSettings?> = dao.getSettings()
    val authorizedDevices: Flow<List<AuthorizedDevice>> = dao.getAuthorizedDevices()
    val userServiceRequests: Flow<List<UserServiceRequest>> = dao.getUserServiceRequests()

    // Database operation methods (suspend as requested by skill rules)
    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun deleteCategory(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteCategory(id)
    }

    suspend fun clearCategories() = withContext(Dispatchers.IO) {
        dao.clearCategories()
    }

    suspend fun insertServiceProvider(provider: ServiceProvider) = withContext(Dispatchers.IO) {
        dao.insertServiceProvider(provider)
    }

    suspend fun deleteServiceProvider(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteServiceProvider(id)
    }

    suspend fun updatePinStatus(id: Int, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dao.updatePinStatus(id, isPinned)
    }

    suspend fun updateRecommendStatus(id: Int, isRecommended: Boolean) = withContext(Dispatchers.IO) {
        dao.updateRecommendStatus(id, isRecommended)
    }

    suspend fun updateVerificationStatus(id: Int, isVerified: Boolean) = withContext(Dispatchers.IO) {
        dao.updateVerificationStatus(id, isVerified)
    }

    suspend fun updateApprovalStatus(id: Int, approved: Boolean, reason: String) = withContext(Dispatchers.IO) {
        dao.updateApprovalStatus(id, approved, reason)
    }

    suspend fun updateBlockStatus(id: Int, blocked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBlockStatus(id, blocked)
    }

    suspend fun addLoyaltyPoints(id: Int, points: Int) = withContext(Dispatchers.IO) {
        dao.addLoyaltyPoints(id, points)
    }

    suspend fun updateSubscriptionStatus(id: Int, active: Boolean, verified: Boolean) = withContext(Dispatchers.IO) {
        dao.updateSubscriptionStatus(id, active, verified)
    }

    fun getReviewsForProvider(providerId: Int): Flow<List<Review>> = dao.getReviewsForProvider(providerId)

    suspend fun insertReview(review: Review) = withContext(Dispatchers.IO) {
        dao.insertReview(review)
        // Also recalculate provider stats
        recalculateProviderStats(review.providerId)
    }

    private suspend fun recalculateProviderStats(providerId: Int) {
        val reviews = dao.getReviewsForProvider(providerId).first()
        if (reviews.isNotEmpty()) {
            val avgRating = reviews.map { it.rating }.average()
            val count = reviews.size
            dao.updateRating(providerId, avgRating, count)
        }
    }

    suspend fun deleteReviewByProvider(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteReviewsByProvider(id)
    }

    suspend fun insertBanner(banner: Banner) = withContext(Dispatchers.IO) {
        dao.insertBanner(banner)
    }

    suspend fun deleteBanner(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteBanner(id)
    }

    suspend fun getSettingsDirect(): AppSettings {
        return withContext(Dispatchers.IO) {
            dao.getSettingsDirect() ?: AppSettings()
        }
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dao.saveSettings(settings)
    }

    suspend fun insertAuditLog(log: AuditLog) = withContext(Dispatchers.IO) {
        dao.insertAuditLog(log)
    }

    suspend fun clearAuditLogs(olderThan: Long) = withContext(Dispatchers.IO) {
        dao.clearAuditLogs(olderThan)
    }

    suspend fun clearAllAuditLogs() = withContext(Dispatchers.IO) {
        dao.clearAllAuditLogs()
    }

    suspend fun insertChatMessage(msg: ChatMessage) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(msg)
    }

    suspend fun clearChatMessages(olderThan: Long) = withContext(Dispatchers.IO) {
        dao.clearChatMessages(olderThan)
    }

    suspend fun clearAllChatMessages() = withContext(Dispatchers.IO) {
        dao.clearAllChatMessages()
    }

    suspend fun insertAuthorizedDevice(device: AuthorizedDevice) = withContext(Dispatchers.IO) {
        dao.insertAuthorizedDevice(device)
    }

    suspend fun deleteAuthorizedDevice(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteAuthorizedDevice(id)
    }

    suspend fun insertServiceRequest(request: UserServiceRequest) = withContext(Dispatchers.IO) {
        dao.insertServiceRequest(request)
    }

    // Seeding logic
    suspend fun initSeedDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = categories.first().size
        if (count == 0) {
            // Add default app settings
            dao.saveSettings(AppSettings())

            // Add default Main categories
            val mains = listOf(
                Category(id = 1, nameAr = "كهرباء", nameEn = "Electricity", sortOrder = 1),
                Category(id = 2, nameAr = "سباكة", nameEn = "Plumbing", sortOrder = 2),
                Category(id = 3, nameAr = "التعليم والتدريب", nameEn = "Education & Training", sortOrder = 3),
                Category(id = 4, nameAr = "صيانة سيارات", nameEn = "Car Maintenance", sortOrder = 4),
                Category(id = 5, nameAr = "خدمات صحية وطبية", nameEn = "Health & Medical", sortOrder = 5),
                Category(id = 6, nameAr = "برمجيات وتقنية", nameEn = "Tech & Software", sortOrder = 6)
            )
            mains.forEach { dao.insertCategory(it) }

            // Add default Sub categories
            val subs = listOf(
                Category(nameAr = "كهربائي منازل", nameEn = "Home Electrician", parentId = 1, sortOrder = 1),
                Category(nameAr = "كهربائي كابلات", nameEn = "Cable Electrician", parentId = 1, sortOrder = 2),
                Category(nameAr = "سباك صحي", nameEn = "Home Plumber", parentId = 2, sortOrder = 1),
                Category(nameAr = "تركيب مضخات مياه", nameEn = "Water Pump Installer", parentId = 2, sortOrder = 2),
                Category(nameAr = "مدرس رياضيات خصوصي", nameEn = "Private Math Tutor", parentId = 3, sortOrder = 1),
                Category(nameAr = "مدرب لغات وتنمية", nameEn = "Trainer & Coach", parentId = 3, sortOrder = 2),
                Category(nameAr = "ميكانيكي محركات سيارات", nameEn = "Car Engine Mechanic", parentId = 4, sortOrder = 1),
                Category(nameAr = "كهربائي كمبيوتر سيارات", nameEn = "Car ECU Electrician", parentId = 4, sortOrder = 2),
                Category(nameAr = "ممرض رعاية منزلية", nameEn = "Home Nurse Care", parentId = 5, sortOrder = 1),
                Category(nameAr = "علاج طبيعي منزلي", nameEn = "Home Physio Care", parentId = 5, sortOrder = 2),
                Category(nameAr = "مطور مواقع وتطبيقات الهواتف", nameEn = "App & Web Developer", parentId = 6, sortOrder = 1),
                Category(nameAr = "صيانة حاسوب وهواتف", nameEn = "PC & Phone Repairs", parentId = 6, sortOrder = 2)
            )
            subs.forEach { dao.insertCategory(it) }

            // Add default Banners
            dao.insertBanner(Banner(imageUrl = "", type = "TEXT", textContent = "خصوم حصرية 20% على خدمات الصيانة المنزلية هذا الأسبوع!", durationDays = 10, targetUrl = "https://dalily.services/discounts"))
            dao.insertBanner(Banner(imageUrl = "", type = "IMAGE", textContent = "انضم الآن كأخصائي مهني واحصل على شارة التوثيق مجاناً لفترة محدودة!", durationDays = 15, targetUrl = "https://dalily.services/promote"))

            // Add default Service Providers (some pending, some approved)
            val providers = listOf(
                ServiceProvider(
                    id = 1,
                    fullName = "الأستاذ ماهر محمد طاهر",
                    phone = "777644670",
                    mainCategory = "كهرباء",
                    subCategory = "كهربائي منازل",
                    address = "صنعاء، شارع الستين الرئيسي",
                    district = "الستين الغربي",
                    gpsCoordinates = "15.3694,44.1910",
                    personalPhoto = "maher_photo",
                    idCardPhoto = "maher_id",
                    isApproved = true,
                    isPinned = true,
                    isRecommended = true,
                    isVerified = true,
                    averageRating = 4.8,
                    reviewCount = 2,
                    loyaltyPoints = 1500,
                    subscriptionActive = true,
                    subscriptionVerified = true
                ),
                ServiceProvider(
                    id = 2,
                    fullName = "المهندس أسامة السنحاني",
                    phone = "733445566",
                    mainCategory = "سباكة",
                    subCategory = "سباك صحي",
                    address = "تعز، حي المسبح مقابل البركة",
                    district = "المسبح",
                    gpsCoordinates = "13.5795,44.0134",
                    personalPhoto = "osama_photo",
                    idCardPhoto = "osama_id",
                    isApproved = true,
                    isPinned = false,
                    isRecommended = true,
                    isVerified = true,
                    averageRating = 4.5,
                    reviewCount = 1,
                    loyaltyPoints = 400,
                    subscriptionActive = false,
                    subscriptionVerified = false
                ),
                ServiceProvider(
                    id = 3,
                    fullName = "فراس صالح الحربي",
                    phone = "711223344",
                    mainCategory = "التعليم والتدريب",
                    subCategory = "مدرس رياضيات خصوصي",
                    address = "عدن، المعلا الشارع العام",
                    district = "المعلا",
                    gpsCoordinates = "12.7836,44.9818",
                    personalPhoto = "firas_photo",
                    idCardPhoto = "firas_id",
                    isApproved = true,
                    isPinned = true,
                    isRecommended = false,
                    isVerified = false,
                    averageRating = 4.0,
                    reviewCount = 1,
                    loyaltyPoints = 100,
                    subscriptionActive = true,
                    subscriptionVerified = true
                ),
                // Pending ones: isApproved = false
                ServiceProvider(
                    id = 4,
                    fullName = "خالد ناصر الجبري",
                    phone = "772233445",
                    mainCategory = "صيانة سيارات",
                    subCategory = "كهربائي كمبيوتر سيارات",
                    address = "صنعاء، حي الحصبة خلف السوق",
                    district = "الحصبة",
                    gpsCoordinates = "15.3850,44.2050",
                    personalPhoto = "khaled_photo",
                    idCardPhoto = "khaled_id",
                    isApproved = false,
                    isPinned = false,
                    isRecommended = false,
                    isVerified = false
                )
            )
            providers.forEach { dao.insertServiceProvider(it) }

            // Default Reviews for Seed data
            dao.insertReview(Review(providerId = 1, userName = "طه الصنعاني", rating = 5, comment = "خدمة ممتازة وسريعة جداً وأنصح بالتعامل معه لتفانيه بالأمانة دقة المواعيد."))
            dao.insertReview(Review(providerId = 1, userName = "أحمد عدنان", rating = 4, comment = "عمل متقن وتجاوب فوري."))
            dao.insertReview(Review(providerId = 2, userName = "علي الحيمي", rating = 5, comment = "محترف وسعره معقول جداً."))
            dao.insertReview(Review(providerId = 3, userName = "أمجد مرشد", rating = 4, comment = "مدرس متمكن من المادة والدروس واضحة جداً."))

            // Seed Whitelisted Device
            dao.insertAuthorizedDevice(AuthorizedDevice(deviceId = "EMULATOR_DEVICE_ID", deviceName = "المحاكي الرئيسي", isApproved = true))
        }
    }
}
