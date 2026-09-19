# java-21-challenge-repository
challenge repository

1. Architecture:

this project contains 3 main layers:

1.1. service layer : which contains the main business logic of the application(credit,debit,transfer,getBalance)
1.2. Repository Layer : is responsible for database access including AccountRepository,TransactionRepository and also UserRepository
1.3. Entity Layer which represents the application's database model.

2. concurrency:

The application uses optimistic locking to handle concurrent updates to the same account.

The Account entity contains a JPA @Version field:

@Version
private Long version;

When an account is updated, JPA checks that the version of the entity being updated is still the same as the version that was originally read.

For example, if two requests try to debit the same account at the same time:

Request A → reads account (version = 1)
Request B → reads account (version = 1)

Request A → updates account → version becomes 2
Request B → tries to update using version 1
                       ↓
              optimistic lock failure

3. Idempotency

The application uses a unique transaction ID to make credit, debit, and transfer operations idempotent.

Before processing an operation, the service checks whether the transaction ID has already been processed:

if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
    return;
}

If the transaction ID already exists, the operation is skipped and the account balance is not changed again.

If the transaction ID does not exist, the operation is performed and the transaction ID is stored in the Transaction table.

For example:

First request:
transactionId = TX-123
Debit 1000
        ↓
Balance changes
        ↓
TX-123 is stored


Second request:
transactionId = TX-123
        ↓
TX-123 already exists
        ↓
Operation is skipped

This prevents the same credit, debit, or transfer request from being processed multiple times when the same transaction ID is submitted again.

The behavior is covered by tests for repeated credit, debit, and transfer operations using the same transaction ID, as well as separate transaction IDs being processed independently.

4. Transfer

The transfer() method performs both sides of the transfer inside the same transaction:

4.1. Debit the source account.
4.2. Credit the destination account.
4.3 Store the transaction ID.

Because these operations belong to the same transaction, the transfer follows an all-or-nothing approach.
The atomicity behavior is verified by an integration test that intentionally causes the destination update to fail after the source debit. The test verifies that both account balances return to their original values.
and about possibility of deadlock in transfer service:
The transfer operation can potentially be exposed to a database deadlock when multiple transfers operate on the same accounts in opposite directions.

For example, consider two concurrent transfers:

Transfer A: Account 1 → Account 2
Transfer B: Account 2 → Account 1

If each transaction obtains a lock on its source account before attempting to access the destination account, the transactions could end up waiting for each other.

Transfer A                  Transfer B

Account 1 locked            Account 2 locked
       ↓                           ↓
needs Account 2             needs Account 1
       ↓                           ↓
       └────── circular wait ──────┘

