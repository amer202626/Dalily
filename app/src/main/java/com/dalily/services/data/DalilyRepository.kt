package com.dalily.services.data

import kotlinx.coroutines.flow.Flow

class DalilyRepository(private val db: AppDatabase) {
    private val serviceProviderDao = db.serviceProviderDao()
    private val reviewDao = db.reviewDao()
    private val auditLogDao = db.auditLogDao()

    val allProviders: Flow<List<ServiceProvider>> = serviceProviderDao.getAllProvidersFlow()
    val allAuditLogs: Flow<List<AuditLog>> = auditLogDao.getAllAuditLogsFlow()

    fun getReviewsForProvider(providerId: Int): Flow<List<Review>> =
        reviewDao.getReviewsForProviderFlow(providerId)

    suspend fun getProviderById(id: Int): ServiceProvider? =
        serviceProviderDao.getProviderById(id)

    suspend fun insertProvider(provider: ServiceProvider): Long =
        serviceProviderDao.insertProvider(provider)

    suspend fun updateProvider(provider: ServiceProvider) =
        serviceProviderDao.updateProvider(provider)

    suspend fun deleteProvider(provider: ServiceProvider) =
        serviceProviderDao.deleteProvider(provider)

    suspend fun insertReview(review: Review): Long =
        reviewDao.insertReview(review)

    suspend fun getAverageRatingForProvider(providerId: Int): Double? =
        reviewDao.getAverageRatingForProvider(providerId)

    suspend fun getReviewsCountForProvider(providerId: Int): Int =
        reviewDao.getReviewsCountForProvider(providerId)

    suspend fun insertAuditLog(log: AuditLog): Long =
        auditLogDao.insertAuditLog(log)

    suspend fun initSeedDataIfEmpty() {
        if (serviceProviderDao.getCount() == 0) {
            // Seed service providers
            val samples = listOf(
                ServiceProvider(
                    name = "أحمد مهدي للسباكة الحديثة",
                    category = "السباكة",
                    description = "متخصص في تأسيس وصيانة شبكات المياه والصرف الصحي وتصليح أي تسريبات بأحدث الأجهزة الإلكترونية دون تكسير.",
                    phone = "771234567",
                    address = "صنعاء - شارع الجزائر",
                    isPremium = true,
                    cardColorHex = "#FFF9C4", // Pastel Gold Yellow
                    isRecommended = true,
                    isPinned = true,
                    isVerified = true
                ),
                ServiceProvider(
                    name = "صادق الكهربائي للهندسة المنزلية",
                    category = "الكهرباء",
                    description = "تركيب وصيانة التوصيلات الكهربائية وأنظمة الطاقة الشمسية بجودة عالية وأمان تام للأجهزة المنزلية.",
                    phone = "773456789",
                    address = "تعز - شارع جمال",
                    isPremium = false,
                    cardColorHex = null,
                    isRecommended = false,
                    isPinned = false,
                    isVerified = true
                ),
                ServiceProvider(
                    name = "مؤسسة الأمل للتبريد والتكييف",
                    category = "التبريد والتكييف",
                    description = "صيانة وتنظيف مكيفات الهواء المركزية والعادية وتعبئة الفريون وتصليح ثلاجات العرض المنزلية والتجارية.",
                    phone = "733987654",
                    address = "عدن - كريتر",
                    isPremium = true,
                    cardColorHex = "#E1BEE7", // Pastel Royal Purple
                    isRecommended = true,
                    isPinned = false,
                    isVerified = false
                ),
                ServiceProvider(
                    name = "النجار المحترف - أبو بكر",
                    category = "النجارة",
                    description = "تفصيل وصيانة غرف النوم، المطابخ، الأبواب، والمكاتب الخشبية بلمسة فنية وتصاميم حديثة تلبي رغباتكم.",
                    phone = "775612345",
                    address = "إب - الدائري الغربي",
                    isPremium = false,
                    cardColorHex = null,
                    isRecommended = false,
                    isPinned = false,
                    isVerified = false
                ),
                ServiceProvider(
                    name = "المهندس رمزي لإصلاح الهواتف والكمبيوتر",
                    category = "صيانة برمجيات وهواتف",
                    description = "صيانة برمجة وعتاد الهواتف والكمبيوتر المحمول واللوحي، استرجاع البيانات المفقودة وتحديث الأنظمة ومكافحة الفيروسات.",
                    phone = "711554433",
                    address = "صنعاء - شارع حدة",
                    isPremium = true,
                    cardColorHex = "#B3E5FC", // Pastel Deep Blue
                    isRecommended = false,
                    isPinned = false,
                    isVerified = true
                ),
                ServiceProvider(
                    name = "برق للتوصيل السريع",
                    category = "خدمات التوصيل",
                    description = "نوفر خدمات نقل وتوصيل الطرود والمستندات والوجبات بين المحافظات وداخل المدن بسرعة فائقة وأمان وبأسعار مدروسة.",
                    phone = "770998877",
                    address = "الحديدة - شارع الميناء",
                    isPremium = false,
                    cardColorHex = null,
                    isRecommended = false,
                    isPinned = false,
                    isVerified = false
                )
            )

            for (provider in samples) {
                val pid = serviceProviderDao.insertProvider(provider)
                
                // Add positive review seed data
                if (provider.isPremium) {
                    reviewDao.insertReview(
                        Review(
                            providerId = pid.toInt(),
                            rating = 5,
                            comment = "خدمة ممتازة وسريعة، أنصح بالتعامل معه بشدة!",
                            reviewerName = "محمد عبد اللطيف"
                        )
                    )
                    reviewDao.insertReview(
                        Review(
                            providerId = pid.toInt(),
                            rating = 4,
                            comment = "مستواه رائع وملتزم بالوقت وجدير بالثقة. شكراً لجهودكم.",
                            reviewerName = "عوض اليافعي"
                        )
                    )
                } else {
                    reviewDao.insertReview(
                        Review(
                            providerId = pid.toInt(),
                            rating = 4,
                            comment = "جيد جداً وتعامل راقٍ وسعر مقبول أنصح به.",
                            reviewerName = "فاطمة اليماني"
                        )
                    )
                }
            }

            // Seed a default login audit log for demo purposes
            auditLogDao.insertAuditLog(
                AuditLog(
                    action = "تسجيل دخول المسؤول",
                    details = "تم تسجيل الدخول بنجاح إلى لوحة التحكم الافتراضية.",
                    location = "صنعاء، اليمن (IP: 192.168.1.10)",
                    adminName = "المسؤول الأول"
                )
            )
            auditLogDao.insertAuditLog(
                AuditLog(
                    action = "تسجيل دخول المسؤول",
                    details = "تم الدخول للإشراف وتحديث الحسابات التوثيقية.",
                    location = "عدن، اليمن (IP: 82.114.168.5)",
                    adminName = "أدمن الإشراف"
                )
            )
        }
    }
}
