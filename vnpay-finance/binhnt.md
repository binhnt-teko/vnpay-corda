./gradlew  clean jar
cp finance/build/libs/corda-finance.jar  ../vnpay-node/test-nodes/notary/cordapps/
cp finance/build/libs/corda-finance.jar  ../vnpay-node/test-nodes/partyA/cordapps/
cp finance/build/libs/corda-finance.jar  ../vnpay-node/test-nodes/partyB/cordapps/