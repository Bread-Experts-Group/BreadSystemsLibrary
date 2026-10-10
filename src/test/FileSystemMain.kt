package test

import org.bread_experts_group.bread_systems_library.ffm.get
import org.bread_experts_group.bread_systems_library.ffm.windows.Kernel32
import org.bread_experts_group.bread_systems_library.ffm.windows.KernelBase
import org.bread_experts_group.bread_systems_library.ffm.windows.WindowsCleaningArena
import org.bread_experts_group.bread_systems_library.ffm.windows.models.CleanableHandle
import org.bread_experts_group.bread_systems_library.ffm.windows.models.ioring.ThreadedIORing
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.LPCWSTR
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.WCHARptr
import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import kotlin.reflect.typeOf

fun main() {
    val linker = Linker.nativeLinker()
    val k32 = get<Kernel32>(SymbolLookup.libraryLookup("Kernel32.dll", Arena.ofAuto()), linker)
    val kBase = get<KernelBase>(SymbolLookup.libraryLookup("KernelBase.dll", Arena.ofAuto()), linker)

    val value = CleanableHandle(
        k32,
        k32.CreateFileW!!(
            LPCWSTR(
                typeOf<WCHARptr>(),
                Arena.global().allocateFrom("src/test/FileSystemMain.kt", Charsets.UTF_16LE)
            ), DWORD(0x80000000.toInt()), DWORD(0),
            null, DWORD(4), DWORD(0),
            null
        )
    )

    val cleaning = WindowsCleaningArena(k32, kBase)
    val buffer = cleaning.allocate(256000)

    repeat(10) {
        val ior = ThreadedIORing(k32, kBase)
        println(ior)
        Thread.sleep(5000)
    }
//    repeat(1_000_000) {
//        if (it % 10_000 == 0) {
//            println(it)
//
//            Runtime.getRuntime().gc()
//            Thread.sleep(2000)
//        }
//        val ior = ThreadedIORing(k32, kBase)
//
//        val read = AtomicLong(0)
//
//        ior.register(arrayOf(value))
//        ior.register(arrayOf(buffer))
//
//        ior.fillHandleRef(value)
//        ior.fillBufferRef(buffer, 0)
//
//        ior.readAsync(
//            0, 1024,
//            buffer, 0
//        ).thenAccept {
//            read.getAndAdd(it)
//        }
//
//        ior.submit()
////        println(buffer.reinterpret(read.get()).toArray(ValueLayout.JAVA_BYTE).toHexString())
//    }
}