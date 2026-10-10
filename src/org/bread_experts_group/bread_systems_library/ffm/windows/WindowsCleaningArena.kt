package org.bread_experts_group.bread_systems_library.ffm.windows

import org.bread_experts_group.bread_systems_library.ffm.c.types.Pointer
import org.bread_experts_group.bread_systems_library.ffm.windows.models.handleLastError
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.DWORD64
import org.bread_experts_group.bread_systems_library.ffm.windows.types.defs.SIZE_T
import org.bread_experts_group.bread_systems_library.ffm.windows.types.structs.winnt.*
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.ref.Cleaner

class WindowsCleaningArena(
    private val kernel32: Kernel32,
    private val kernelBase: KernelBase
) : SegmentAllocator {
    private companion object {
        val cleaner: Cleaner = Cleaner.create()
    }

    private val addressRequirements = Pointer<MEM_ADDRESS_REQUIREMENTS>(this)
    private val extendedParameter = Pointer<MEM_EXTENDED_PARAMETER>(this)

    override fun allocate(byteSize: Long, byteAlignment: Long): MemorySegment? {
        var extendedParameters: Pointer<MEM_EXTENDED_PARAMETER>? = null
        var parametersCount = 0
        if (byteAlignment > 8) {
            addressRequirements.value.Alignment = SIZE_T(byteAlignment)
            extendedParameter.value.Type = DWORD64(MemExtendedParameterAddressRequirements)
            @Suppress("UNCHECKED_CAST")
            extendedParameter.value.Union.Pointer = addressRequirements as Pointer<Void>

            parametersCount = 1
            extendedParameters = extendedParameter
        }
        val address = kernelBase.VirtualAlloc2!!(
            null,
            null, SIZE_T(byteSize), DWORD(MEM_COMMIT), DWORD(PAGE_READWRITE),
            extendedParameters, DWORD(parametersCount)
        )
        if (address.segment == MemorySegment.NULL) handleLastError(kernel32)
        return address.segment.reinterpret(byteSize).also {
            cleaner.register(it) {
                val stat = kernel32.VirtualFreeEx!!(
                    kernel32.GetCurrentProcess!!(),
                    address, SIZE_T(0L), DWORD(MEM_RELEASE)
                )
                if (stat.value as Int == 0) handleLastError(kernel32)
            }
        }
    }
}