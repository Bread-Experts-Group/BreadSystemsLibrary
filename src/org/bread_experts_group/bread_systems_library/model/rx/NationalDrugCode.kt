package org.bread_experts_group.bread_systems_library.model.rx

data class NationalDrugCode(
    val manufacturer: UInt,
    val product: UShort,
    val form: UShort?
)