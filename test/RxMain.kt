import org.bread_experts_group.bread_systems_library.model.*
import org.bread_experts_group.bread_systems_library.model.rx.NationalDrugCode
import org.bread_experts_group.bread_systems_library.model.rx.dea.DEASchedule
import org.bread_experts_group.bread_systems_library.model.rx.openfda.OpenFDADrugListing
import org.xml.sax.SAXException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Source
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import kotlin.io.encoding.Base64

const val RX_NORM_SERVICE_DOMAIN = "https://rxnav.nlm.nih.gov"

const val RX_NORM_SCHEMA = "${RX_NORM_SERVICE_DOMAIN}/rxnormrest.xsd"
const val RX_NORM_GET_TERM_TYPES = "$RX_NORM_SERVICE_DOMAIN/REST/termtypes"

const val FDA_SERVICE_DOMAIN = "https://api.fda.gov"

const val FDA_DRUG = "$FDA_SERVICE_DOMAIN/drug/ndc.json"

const val FDA_API_KEY = "CJqDR9o1PScLOYAEZxszA09o6MkgaHHiQV6trKrZ"

fun main() {
    val http = HttpClient.newHttpClient()
    val b64 = Base64.UrlSafe
    var url = "$FDA_DRUG?limit=100"
    for (i in 0..2) {
        val result = http.send(
            HttpRequest.newBuilder(URI(url))
                .header("Authorization", "Basic ${b64.encode(FDA_API_KEY.toByteArray())}")
                .build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )
        val data = jsonParse(result.body().reader()) as JSONObject
        val manus = mutableSetOf<UInt>()
        (data.elements["results"] as JSONArray).elements.forEach { result ->
            result as JSONObject
            val ndc = (result.elements["product_ndc"] as JSONString).value.split('-', limit = 2)

            val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            OpenFDADrugListing(
                NationalDrugCode(ndc[0].toUInt(), ndc[1].toUShort(), null),
                (result.elements["generic_name"] as JSONString).value,
                (result.elements["brand_name"] as? JSONString)?.value,
                (result.elements["brand_name_base"] as? JSONString)?.value,
                (result.elements["brand_name_suffix"] as? JSONString)?.value,
                (result.elements["labeler_name"] as JSONString).value,
                (result.elements["finished"] as JSONBoolean).value,
                (result.elements["listing_expiration_date"] as? JSONString)?.value?.let {
                    LocalDate.parse(it, dateFormatter)
                },
                LocalDate.parse((result.elements["marketing_start_date"] as JSONString).value, dateFormatter),
                (result.elements["marketing_end_date"] as? JSONString)?.value?.let {
                    LocalDate.parse(it, dateFormatter)
                },
                (result.elements["marketing_category"] as JSONString).value,
                (result.elements["dosage_form"] as JSONString).value,
                result.elements["route"].let {
                    if (it is JSONArray) it.elements.map { route -> (route as JSONString).value }
                    else if (it != null) throw IllegalArgumentException("Route error: ${it::class}")
                    else emptyList()
                },
                result.elements["pharm_class"].let {
                    if (it is JSONArray) it.elements.map { pharmacologicalClass -> (pharmacologicalClass as JSONString).value }
                    else if (it != null) throw IllegalArgumentException("Pharmacological class error: ${it::class}")
                    else emptyList()
                },
                result.elements["active_ingredients"].let {
                    if (it is JSONArray) it.elements.map { activeIngredient ->
                        val elements = (activeIngredient as JSONObject).elements
                        OpenFDADrugListing.ActiveIngredient(
                            (elements["name"] as JSONString).value,
                            (elements["strength"] as JSONString).value
                        )
                    }
                    else if (it != null) throw IllegalArgumentException("Active ingredient error: ${it::class}")
                    else emptyList()
                },
                result.elements["packaging"].let {
                    if (it is JSONArray) it.elements.map { mPackage ->
                        val elements = (mPackage as JSONObject).elements
                        val ndc = (elements["package_ndc"] as JSONString).value.split('-', limit = 3)
                        OpenFDADrugListing.Package(
                            NationalDrugCode(ndc[0].toUInt(), ndc[1].toUShort(), ndc[2].toUShort()),
                            (elements["description"] as JSONString).value,
                            (elements["marketing_start_date"] as JSONString).value,
                            (elements["marketing_end_date"] as? JSONString)?.value,
                            (elements["sample"] as? JSONBoolean)?.value
                        )
                    }
                    else if (it != null) throw IllegalArgumentException("Package error: ${it::class}")
                    else emptyList()
                },
                UUID.fromString((result.elements["spl_id"] as JSONString).value),
                (result.elements["product_type"] as JSONString).value,
                (result.elements["application_number"] as? JSONString)?.value,
                (result.elements["dea_schedule"] as? JSONString)?.value?.let {
                    when (it) {
                        "CI" -> DEASchedule.SCHEDULE_I
                        "CII" -> DEASchedule.SCHEDULE_II
                        "CIII" -> DEASchedule.SCHEDULE_III
                        "CIV" -> DEASchedule.SCHEDULE_IV
                        "CV" -> DEASchedule.SCHEDULE_V
                        else -> throw IllegalStateException("Unknown DEA schedule: \"$it\"")
                    }
                } ?: DEASchedule.NONE,
                (result.elements["openfda"] as JSONObject).elements.let { elements ->
                    OpenFDADrugListing.OpenFDAInformation(
                        (elements["is_original_packager"] as? JSONArray)?.elements?.map {
                            (it as JSONBoolean).value
                        } ?: emptyList(),
                        (elements["manufacturer_name"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["nui"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["pharm_class_cs"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["pharm_class_epc"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["pharm_class_moa"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["pharm_class_pe"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["rxcui"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["spl_set_id"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["unii"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                        (elements["upc"] as? JSONArray)?.elements?.map {
                            (it as JSONString).value
                        } ?: emptyList(),
                    )
                },
            ).also {
                it.openFDA.originalPackager.forEachIndexed { index, bool ->
                    if (bool && manus.add(it.productNDC.manufacturer)) println("$index ${it.openFDA.manufacturerName[index]} ${it.openFDA.rxCUIs.getOrNull(index)}")
                }
            }
        }

        val next = result.headers().allValues("Link")
            .firstOrNull { it.contains("rel=\"next\"") }
            ?.substring(1)
            ?.substringBefore('>')
        if (next == null) break
        url = next
    }



    val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val validator = http.send(
        HttpRequest.newBuilder(URI(RX_NORM_SCHEMA)).build(),
        HttpResponse.BodyHandlers.ofInputStream()
    ).let {
        val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

        val schemaFile: Source = StreamSource(it.body())
        factory.newSchema(schemaFile).newValidator()
    }
    val termTypes = http.send(
        HttpRequest.newBuilder(URI("$RX_NORM_GET_TERM_TYPES.xml")).build(),
        HttpResponse.BodyHandlers.ofInputStream()
    )

    val document = parser.parse(termTypes.body())
    document.getElementsByTagName("termType").let {
        repeat(it.length) { index ->
            println(it.item(index).textContent)
        }
    }

    try {
        validator.validate(DOMSource(document))
    } catch (e: SAXException) {
        throw e
    }
}