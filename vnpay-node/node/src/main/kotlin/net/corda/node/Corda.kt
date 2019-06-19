@file:JvmName("Corda")
package net.corda.node

import net.corda.cliutils.start
import net.corda.node.internal.NodeStartupCli

fun main(args: Array<String>) {
    // Pass the arguments to the Node factory. In the Enterprise edition, this line is modified to point to a subclass.
    // It will exit the process in case of startup failure and is not intended to be used by embedders. If you want
    // to embed Node in your own container, instantiate it directly and set up the configuration objects yourself.
    println("Binhnt: net.corda.node.main: Start NodeStartupCli ")
    NodeStartupCli().start(args)
}