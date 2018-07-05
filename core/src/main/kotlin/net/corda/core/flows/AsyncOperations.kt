package net.corda.core.flows

import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.StateRef
import net.corda.core.internal.FlowAsyncOperation
import net.corda.core.internal.concurrent.asCordaFuture
import net.corda.core.node.ServiceHub
import java.util.concurrent.CompletableFuture

/** An [FlowAsyncOperation] which suspends a flow until the provided [StateRef]s have been updated. */
class WaitForStatesToUpdate(val stateRefs: Set<StateRef>, val services: ServiceHub) : FlowAsyncOperation<Unit> {
    override fun execute(): CordaFuture<Unit> {
        val futures = stateRefs.map { stateRef -> services.vaultService.whenConsumed(stateRef).toCompletableFuture() }
        // Hack alert!
        // CompletableFuture.allOf() returns a CompletableFuture<Void!>! and then returning a CordaFuture<Void>
        // breaks the state machine manager, so instead, we return a CordaFuture<Unit>, which works.
        // TODO: Fix the above.
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply { Unit }.asCordaFuture()
    }
}