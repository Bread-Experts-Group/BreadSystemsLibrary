import org.bread_experts_group.bread_systems_library.ffm.StateLayout
import org.bread_experts_group.bread_systems_library.ffm.StateLayout.LAST_ERROR
import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.get
import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.LPCWSTR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.WCHARptr
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.FILETIME
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.minwinbase.SYSTEMTIME
import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup

fun main() {
    val k32 = get<Kernel32>(SymbolLookup.libraryLookup("Kernel32.dll", Arena.ofAuto()), Linker.nativeLinker())

    val value = k32.CreateFileW!!(
        LPCWSTR(
            WCHARptr::class,
            Arena.global().allocateFrom("src/Main.kt", Charsets.UTF_16LE)
        ), DWORD(0), DWORD(0),
        null, DWORD(4), DWORD(0),
        null
    )
    println(value.segment)
    println(StateLayout.get(LAST_ERROR))

    val creation = Pointer.structure<FILETIME>(Arena.global())
    val lastAccess = Pointer.structure<FILETIME>(Arena.global())
    val lastWrite = Pointer.structure<FILETIME>(Arena.global())
    k32.GetFileTime!!(
        value, creation, lastAccess, lastWrite
    )
    val sysTime = Pointer.structure<SYSTEMTIME>(Arena.global())
    k32.FileTimeToSystemTime!!(creation, sysTime)
    sysTime.value.apply { println("$wMonth/$wDay/$wYear $wHour:$wMinute:$wSecond.$wMilliseconds") }
    k32.FileTimeToSystemTime(lastAccess, sysTime)
    sysTime.value.apply { println("$wMonth/$wDay/$wYear $wHour:$wMinute:$wSecond.$wMilliseconds") }
    k32.FileTimeToSystemTime(lastWrite, sysTime)
    sysTime.value.apply { println("$wMonth/$wDay/$wYear $wHour:$wMinute:$wSecond.$wMilliseconds") }
}