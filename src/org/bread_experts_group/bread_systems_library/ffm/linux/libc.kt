package org.bread_experts_group.bread_systems_library.ffm.linux

import org.bread_experts_group.bread_systems_library.ffm.LibrarySource
import org.bread_experts_group.bread_systems_library.ffm.c.PreserveErrno
import org.bread_experts_group.bread_systems_library.ffm.c.types.*
import org.bread_experts_group.bread_systems_library.ffm.linux.types.def.nfds_t
import org.bread_experts_group.bread_systems_library.ffm.linux.types.structs.pollfd
import java.lang.foreign.Linker

@Suppress("ClassName")
class libc(
    override val linker: Linker,
    val socket: @PreserveErrno ((domain: int, type: int, protocol: int) -> int)?,
    val send: @PreserveErrno ((sockfd: int, buf: void?, size: size_t, flags: int) -> ssize_t)?,
    val recv: @PreserveErrno ((sockfd: int, buf: void?, size: size_t, flags: int) -> ssize_t)?,
    val poll: @PreserveErrno ((pollfds: Pointer<pollfd>?, nfds: nfds_t, timeout: int) -> int)?
) : LibrarySource