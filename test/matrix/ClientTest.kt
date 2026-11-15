package matrix

import org.bread_experts_group.bread_systems_library.model.codings.jsonDecode
import org.bread_experts_group.bread_systems_library.model.jsonParse
import org.bread_experts_group.bread_systems_library.model.matrix.MatrixClient
import org.bread_experts_group.bread_systems_library.model.matrix.MatrixID
import org.bread_experts_group.bread_systems_library.model.matrix.MatrixWellKnownClientProperties
import org.bread_experts_group.bread_systems_library.model.matrix.authentication.MatrixPasswordBasedAuthentication
import org.bread_experts_group.bread_systems_library.model.matrix.identifier.MatrixUser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.system.exitProcess

const val ID = "@ihbucskvbj:terryiscool160.xyz" //"@aerasto:aerasto.xyz"
const val PASSWORD = "TEST TEST TEST TEST t1!"

// @terry:terryiscool160.xyz

//  Retry-After


fun main() {
    val client = HttpClient.newHttpClient()

    // https://hostname/.well-known/matrix/client
    val (username, addresses, port) = MatrixID(ID)
    val matrixInit = client.send(
        HttpRequest.newBuilder(
            URI("https://${addresses[0].hostName}${if (port != null) ":$port" else ""}/.well-known/matrix/client")
        ).build(),
        HttpResponse.BodyHandlers.ofString()
    )
    when (matrixInit.statusCode()) {
        404 -> {
            println("IGNORE: Server did not find matrix properties")
            exitProcess(0)
        }
        200 -> {}
        else -> {
            println("FAIL_PROMPT: Server returned non-OK status")
            exitProcess(0)
        }
    }
    val matrixProperties = try {
        jsonDecode<MatrixWellKnownClientProperties>(jsonParse(matrixInit.body().reader()))
    } catch (_: Exception) {
        println("FAIL_PROMPT: JSON parsing failed in matrix properties")
        exitProcess(0)
    }
    val baseServer = try {
        URI(matrixProperties.mHomeserver.baseURL)
    } catch (_: Exception) {
        println("FAIL_ERROR: Homeserver URI is not parsable")
        exitProcess(0)
    }
    // TODO: If the m.identity_server property is present, extract the base_url value for use as the base URL of the identity server. Validation for this URL is done as in the step above, but using /_matrix/identity/v2 as the endpoint to connect to. If the m.identity_server property is present, but does not have a base_url value, then FAIL_PROMPT.
    val matrixClient = MatrixClient(baseServer = baseServer)
    println(matrixClient.supportedLoginTypes())
    matrixClient.login(
        MatrixClient.LoginInformation(
            identifier = MatrixUser(ID),
            authentication = MatrixPasswordBasedAuthentication(PASSWORD)
        )
    )
    println(matrixClient.capabilities().getOK())
    println(matrixClient.synchronize())
    println(matrixClient.createRoom(MatrixClient.CreateRoomInformation(name = "test room 123 for eam!", topic = "Kupya!")))
    println(matrixClient.synchronize())
//    val status = matrixClient.register(MatrixClient.RegistrationKind.USER)
//    if (status.isError) {
//        println(status.getError())
//        exitProcess(1)
//    }
//    val registerReport = status.getOK()
//    if (registerReport is MatrixLegacyRegistration401) {
//        registerReport.flows.forEach { flow ->
//            flow.stages.forEach { stage ->
//                when (stage) {
//                    AUTH_TYPE_EMAIL_IDENTITY -> {
//                        matrixClient.registerEmail("pmt.mailservice@gmail.com")
//                        TODO("!")
//
//                        val rr2 = matrixClient.register(
//                            MatrixClient.RegistrationKind.USER,
//                            MatrixEmailBasedAuthentication(
//                                MatrixEmailBasedAuthentication.ThreePIDCredentials(
//                                    "",
//                                    "",
//                                    null,
//                                    null
//                                ),
//                                registerReport.session
//                            )
//                        )
//                        println(rr2)
//                        TODO("*")
//                    }
//
//                    AUTH_TYPE_TOKEN_IDENTITY -> {
//                        val rr2 = matrixClient.register(
//                            MatrixClient.RegistrationKind.USER,
//                            MatrixTokenBasedAuthentication(
//                                "r9BaMIFA9yMitn4c",
//                                registerReport.session
//                            )
//                        )
//                        println(rr2)
//                        TODO("*")
//                    }
//
//                    else -> TODO("$stage")
//                }
//            }
//        }
//    } else TODO("$registerReport")
}