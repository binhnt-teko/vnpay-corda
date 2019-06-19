package net.corda.finance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.issuedBy
import java.util.*
import net.corda.finance.flows.AbstractCashFlow.Companion.GENERATING_TX
import net.corda.finance.flows.AbstractCashFlow.Companion.SIGNING_TX
import net.corda.finance.flows.AbstractCashFlow.Companion.FINALISING_TX

/**
 * Initiates a flow that self-issues cash (which should then be sent to recipient(s) using a payment transaction).
 *
 * We issue cash only to ourselves so that all KYC/AML checks on payments are enforced consistently, rather than risk
 * checks for issuance and payments differing. Outside of test scenarios it would be extremely unusual to issue cash
 * and immediately transfer it, so impact of this limitation is considered minimal.
 *
 * @param amount the amount of currency to issue.
 * @param issuerBankPartyRef a reference to put on the issued currency.
 * @param notary the notary to set on the output states.
 */
@StartableByRPC
class CashIssueFlow(private val account: Long,
                    private val amount: Amount<Currency>,
                    private val issuerBankPartyRef: OpaqueBytes,
                    private val notary: Party,
                    progressTracker: ProgressTracker) : AbstractCashFlow<AbstractCashFlow.Result>(progressTracker) {
    constructor(account: Long,
                amount: Amount<Currency>,
                issuerBankPartyRef: OpaqueBytes,
                notary: Party) : this(account,amount, issuerBankPartyRef, notary, tracker())

    constructor(request: IssueRequest) : this(request.account, request.amount, request.issueRef, request.notary, tracker())

    @Suspendable
    override fun call(): AbstractCashFlow.Result {
        println("binhnt: CashIssueFlow.call: inherit from  AbstractCashFlow.Result")

        progressTracker.currentStep = GENERATING_TX

        println("binhnt: CashIssueFlow.call: TransactionBuilder with notary")
        val builder = TransactionBuilder(notary)

        println("binhnt: CashIssueFlow.call: get issuer identity")
        val issuer = ourIdentity.ref(issuerBankPartyRef)

        println("binhnt: CashIssueFlow.call: call Cash().generateIssue ")
        val signers = Cash().generateIssue(builder, amount.issuedBy(issuer), ourIdentity, account, notary)

        progressTracker.currentStep = SIGNING_TX

        println("binhnt: CashIssueFlow.call: call serviceHub.signInitialTransaction ")
        val tx = serviceHub.signInitialTransaction(builder, signers)
        progressTracker.currentStep = FINALISING_TX
        // There is no one to send the tx to as we're the only participants

        println("binhnt: CashIssueFlow.call: call finaliseTx ")
        val notarised = finaliseTx(tx, emptySet(), "Unable to notarise issue")

        println("binhnt: CashIssueFlow.call: return resource from Result(notarised, ourIdentity). Save receipt ")
        return Result(notarised, ourIdentity)
    }

    @CordaSerializable
    class IssueRequest(amount: Amount<Currency>, val issueRef: OpaqueBytes, val notary: Party,val account: Long) : AbstractRequest(amount)
}
