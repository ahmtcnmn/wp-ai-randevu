package com.appointflow.tenant;

/**
 * Tenant'ın hangi sektörde faaliyet gösterdiği.
 * Faz 1: BARBER + HAIR_SALON desteklenir. Diğerleri mimari hazır, frontend henüz yok.
 *
 * Etiketler ve default'lar için bkz. {@link SectorLabels}.
 */
public enum BusinessType {
    /** Erkek berber */
    BARBER,
    /** Kadın kuaförü */
    HAIR_SALON,
    /** Diş kliniği */
    DENTAL_CLINIC,
    /** Güzellik salonu (cilt bakımı, kalıcı makyaj, lazer epilasyon) */
    BEAUTY_SALON,
    /** Spa/masaj */
    SPA,
    /** Spor salonu/antrenör */
    GYM,
    /** Veteriner kliniği */
    VETERINARY,
    /** Pet kuaförü */
    PET_GROOMING,
    /** Tırnak salonu */
    NAIL_SALON,
    /** Dövme stüdyosu */
    TATTOO,
    /** Diğer / karma işletme */
    OTHER
}
