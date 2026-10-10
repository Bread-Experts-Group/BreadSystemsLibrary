package org.bread_experts_group.bread_systems_library.model.rx.openfda

import org.bread_experts_group.bread_systems_library.model.rx.NationalDrugCode
import org.bread_experts_group.bread_systems_library.model.rx.dea.DEASchedule
import java.time.LocalDate
import java.util.*

data class OpenFDADrugListing(
    val productNDC: NationalDrugCode,
    val genericName: String,
    val brandName: String?,
    val brandNameBase: String?,
    val brandNameSuffix: String?,
    val labelerName: String,
    val finished: Boolean,
    val expirationDate: LocalDate?,
    val marketingStartDate: LocalDate,
    val marketingEndDate: LocalDate?,
    val marketingCategory: String,
    val dosageForm: String,
    val routes: List<String>,
    val pharmacologicalClasses: List<String>,
    val activeIngredients: List<ActiveIngredient>,
    val packaging: List<Package>,
    val spl: UUID,
    val productType: String,
    val applicationNumber: String?,
    val deaSchedule: DEASchedule,
    val openFDA: OpenFDAInformation
) {
    data class ActiveIngredient(
        val name: String,
        val strength: String
    )

    data class Package(
        val packageNDC: NationalDrugCode?,
        val description: String,
        val marketingStartDate: String,
        val marketingEndDate: String?,
        val sample: Boolean?
    )

    data class OpenFDAInformation(
        val originalPackager: List<Boolean>,
        val manufacturerName: List<String>,
        val ndfRTIdentifiers: List<String>,
        val pharmacologicalClassesChemicalStructure: List<String>,
        val pharmacologicalClassesIndication: List<String>,
        val pharmacologicalClassesMechanismOfAction: List<String>,
        val pharmacologicalClassesPharmacodynamic: List<String>,
        val rxCUIs: List<String>,
        val splSetIDs: List<String>,
        val uniqueIngredientIdentifiers: List<String>,
        val upcs: List<String>,
    )
}