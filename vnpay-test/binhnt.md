> java -jar corda-tools-database-manager-3.2.jar --base-directory /path/to/node --create-migration-sql-for-cordapp com.example.MyMappedSchema




java -jar libs/corda-tools-database-manager-3.2.jar  \
 --base-directory Notary/  \
  --create-migration-sql-for-cordapp  net.corda.finance.schemas.CashSchemaV1

java -jar libs/corda-tools-database-manager-3.2.jar  \
--base-directory Notary/  \
--create-migration-sql-for-cordapp  net.corda.finance.schemas.CommercialPaperSchemaV1
cd Notary/
jar cvf cordapps/CashSchemaV1-migration.jar migration

java -jar libs/corda-tools-database-manager-3.2.jar  --base-directory Notary/ --execute-migration  



SELECT nested.transaction_id, 
			nested.output_index,
			nested.pennies,
			nested.total as pre_total_pennies,
			nested.total+nested.pennies as total_pennies, 
			nested.lock_id
FROM
			 (SELECT vs.transaction_id, 
							vs.output_index, 
							ccs.pennies,
						coalesce((SUM(ccs.pennies) OVER (PARTITION BY 1 ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING)), 0) AS total,
						vs.lock_id
						FROM vault_states AS vs, contract_cash_states AS ccs
				WHERE vs.transaction_id = ccs.transaction_id AND vs.output_index = ccs.output_index
				AND vs.state_status = 0
				AND ccs.ccy_code = 'USD'
				AND  vs.lock_id is null
				) nested  
				where nested.total < 60001
												
												
												
												
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'banka'
  AND pid <> pg_backend_pid();