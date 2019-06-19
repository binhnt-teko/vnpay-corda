package net.corda.finance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import java.util.*
import net.corda.finance.flows.AbstractCashFlow.Companion.GENERATING_ID
import net.corda.finance.flows.AbstractCashFlow.Companion.GENERATING_TX
import net.corda.finance.flows.AbstractCashFlow.Companion.SIGNING_TX
import net.corda.finance.flows.AbstractCashFlow.Companion.FINALISING_TX
import kotlin.system.*

/**
 * Initiates a flow that sends cash to a recipient.
 *
 * @param amount the amount of a currency to pay to the recipient.
 * @param recipient the party to pay the currency to.
 * @param issuerConstraint if specified, the payment will be made using only cash issued by the given parties.
 * @param anonymous whether to anonymous the recipient party. Should be true for normal usage, but may be false
 * for testing purposes.
 */
@StartableByRPC
open class CashPaymentFlow(
        val amount: Amount<Currency>,
        val recipient: Party,
        val anonymous: Boolean,
        progressTracker: ProgressTracker,
        val issuerConstraint: Set<Party> = emptySet()) : AbstractCashFlow<AbstractCashFlow.Result>(progressTracker) {
    /** A straightforward constructor that constructs spends using cash states of any issuer. */
    constructor(amount: Amount<Currency>, recipient: Party) : this(amount, recipient, true, tracker())

    /** A straightforward constructor that constructs spends using cash states of any issuer. */
    constructor(amount: Amount<Currency>, recipient: Party, anonymous: Boolean) : this(amount, recipient, anonymous, tracker())

    constructor(request: PaymentRequest) : this(request.amount, request.recipient, request.anonymous, tracker(), request.issuerConstraint)

    @Suspendable
    override fun call(): AbstractCashFlow.Result {

        val start = System.currentTimeMillis()
        //println("binhnt: CashPaymentFlow.call(): start")
        progressTracker.currentStep = GENERATING_ID

        //println("binhnt: CashPaymentFlow.call(): txIdentities")
        val txIdentities = if (anonymous) {
            subFlow(SwapIdentitiesFlow(recipient))
        } else {
            emptyMap<Party, AnonymousParty>()
        }

        //println("binhnt: CashPaymentFlow.call(): set recipient")
        val anonymousRecipient = txIdentities[recipient] ?: recipient
        progressTracker.currentStep = GENERATING_TX

        //println("binhnt: CashPaymentFlow.call(): build transaction")

        val builder = TransactionBuilder(notary = null)
        // TODO: Have some way of restricting this to states the caller controls
        val (spendTX, keysForSigning) = try {

            //println("binhnt: CashPaymentFlow.call(): call cash spent")
            Cash.generateSpend(serviceHub,
                builder,
                amount,
                ourIdentityAndCert,
                anonymousRecipient,
                issuerConstraint)
        } catch (e: InsufficientBalanceException) {
            throw CashException("Insufficient cash for spend: ${e.message}", e)
        }

        progressTracker.currentStep = SIGNING_TX
        //println("binhnt: CashPaymentFlow.call(): Sign initial transaction")
        val tx = serviceHub.signInitialTransaction(spendTX, keysForSigning)

        progressTracker.currentStep = FINALISING_TX
        //println("binhnt: CashPaymentFlow.call(): send to notary and finalise")
        val notarised = finaliseTx(tx, setOf(recipient), "Unable to notarise spend")

        val  total_time =  System.currentTimeMillis() - start
        println("binhnt: CashPaymentFlow.call(): total time: $total_time")

        return Result(notarised, anonymousRecipient)
    }

    @CordaSerializable
    class PaymentRequest(amount: Amount<Currency>,
                         val recipient: Party,
                         val anonymous: Boolean,
                         val issuerConstraint: Set<Party> = emptySet()) : AbstractRequest(amount)
}
