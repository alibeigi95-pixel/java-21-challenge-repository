package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.demo.bank.entity.Account;
import com.example.demo.bank.service.BalanceInterface;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.bank.repository.*;
import java.util.*;
import java.util.concurrent.Callable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
    private BalanceInterface balanceInterface;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoSpyBean
    private AccountRepository accountRepositorySpy;

	@Test
	void testCreditWhenAccountIdIsNotExist() {
		Long nonExistingAccountId = 999L;

    RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.credit(nonExistingAccountId, 999L,UUID.randomUUID().toString())
    );

    assertEquals("Account is not exist", exception.getMessage());
		
	}

    @Test
	void testDebitWhenAccountIdIsNotExist() {
		Long nonExistingAccountId = 999L;

    RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.debit(nonExistingAccountId, 999L,UUID.randomUUID().toString())
    );

    assertEquals("Account is not exist", exception.getMessage());
		
	}

    @Test
    void testDebitWhenAmountIsNotEnough(){
        Account account = new Account();
        account.setAmount(1000L);
        account.setAccountNumber("123");
        accountRepository.save(account);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.debit(account.getAccountId(), 5000L,UUID.randomUUID().toString()));
        assertEquals("account's amount is not enough.", exception.getMessage());
        Account accountUpdatedAccount = accountRepository.findById(account.getAccountId()).orElseThrow();
        assertEquals(1000L, accountUpdatedAccount.getAmount());
    }

    @Test
    void testCreditWhenAmountIsNotPositive(){
        Account account = new Account();
        account.setAmount(1000L);
        account.setAccountNumber("123");
        accountRepository.save(account);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.credit(account.getAccountId(), -1000L,UUID.randomUUID().toString()));
        assertEquals("Amount must be greater than zero.", exception.getMessage());
    }

    @Test
    void testCreditInSingleCase(){
        Account account = new Account();
        account.setAmount(1000L);
        account.setAccountNumber("123");
        accountRepository.save(account);
        balanceInterface.credit(account.getAccountId(), 2000L, "trx-1001");
        Account updatedAccount =accountRepository.findById(account.getAccountId()).orElseThrow();
        assertEquals(3000L, updatedAccount.getAmount());
    }

    @Test
    void testDebitWhenAmountIsNotPositive(){
        Account account = new Account();
        account.setAmount(1000L);
        account.setAccountNumber("123");
        accountRepository.save(account);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.debit(account.getAccountId(), -1000L, UUID.randomUUID().toString()));
        assertEquals("Amount must be greater than zero.", exception.getMessage());
    }

    @Test
    void testDebitInSingleCase(){
        Account account = new Account();
        account.setAmount(3000L);
        account.setAccountNumber("123");
        accountRepository.save(account);
        balanceInterface.debit(account.getAccountId(), 2000L, UUID.randomUUID().toString());
        Account updatedAccount =accountRepository.findById(account.getAccountId()).orElseThrow();
        assertEquals(1000L, updatedAccount.getAmount());
    }

    @Test
    void testTransferWhenSourceAccountIsNotExist(){
        Account destination = new Account();
        destination.setAmount(5000L);
        destination.setAccountNumber("1234");
        accountRepository.save(destination);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.transfer(999L,destination.getAccountId(), 5000L,UUID.randomUUID().toString()));
        assertEquals("sourceAccount is not exist", exception.getMessage());
        
    }

    @Test
    void testTransferWhenDestinationAccountIsNotExist(){
        Account source = new Account();
        source.setAmount(5000L);
        source.setAccountNumber("1234");
        accountRepository.save(source);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.transfer(source.getAccountId(),999L, 5000L,UUID.randomUUID().toString()));
        assertEquals("destinationAccount is not exist", exception.getMessage());
    }

    @Test
    void testTransferWhenAmountIsNotEnough(){
        Account source = new Account();
        source.setAmount(3000L);
        source.setAccountNumber("123");
        accountRepository.save(source);
        Account destination = new Account();
        destination.setAmount(5000L);
        destination.setAccountNumber("1234");
        accountRepository.save(destination);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.transfer(source.getAccountId(),destination.getAccountId(), 5000L,UUID.randomUUID().toString()));
        assertEquals("account's amount is not enough.", exception.getMessage());
        Account sourceUpdatedAccount = accountRepository.findById(source.getAccountId()).orElseThrow();
        assertEquals(3000L, sourceUpdatedAccount.getAmount());
        Account destinationUpdatedAccount = accountRepository.findById(destination.getAccountId()).orElseThrow();
        assertEquals(5000L, destinationUpdatedAccount.getAmount());
    }

    @Test
    void testTransferWhenAmountIsNotPositive(){
        Account source = new Account();
        source.setAmount(3000L);
        source.setAccountNumber("123");
        accountRepository.save(source);
        Account destination = new Account();
        destination.setAmount(5000L);
        destination.setAccountNumber("1234");
        accountRepository.save(destination);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.transfer(source.getAccountId(),destination.getAccountId(), 0L,UUID.randomUUID().toString()));
        assertEquals("Amount must be greater than zero.", exception.getMessage());
        Account sourceUpdatedAccount = accountRepository.findById(source.getAccountId()).orElseThrow();
        assertEquals(3000L, sourceUpdatedAccount.getAmount());
        Account destinationUpdatedAccount = accountRepository.findById(destination.getAccountId()).orElseThrow();
        assertEquals(5000L, destinationUpdatedAccount.getAmount());
    }

    @Test
    void testTransferWhenSourceAndDestinationAreSame(){
        Account source = new Account();
        source.setAmount(3000L);
        source.setAccountNumber("123");
        accountRepository.save(source);
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> balanceInterface.transfer(source.getAccountId(),source.getAccountId(), 1000L,UUID.randomUUID().toString()));
        assertEquals("Source and destination accounts must be different.", exception.getMessage());
        Account sourceUpdatedAccount = accountRepository.findById(source.getAccountId()).orElseThrow();
        assertEquals(3000L, sourceUpdatedAccount.getAmount());
    }

    @Test
    void testTransferInSingleCase(){
        Account source = new Account();
        source.setAmount(3000L);
        source.setAccountNumber("123");
        accountRepository.save(source);
        Account destination = new Account();
        destination.setAmount(5000L);
        destination.setAccountNumber("1234");
        accountRepository.save(destination);
        balanceInterface.transfer(source.getAccountId(),destination.getAccountId(), 2000L, UUID.randomUUID().toString());
        Account sourceUpdatedAccount = accountRepository.findById(source.getAccountId()).orElseThrow();
        Account destinationUpdatedAccount = accountRepository.findById(destination.getAccountId()).orElseThrow();
        assertEquals(1000L, sourceUpdatedAccount.getAmount());
        assertEquals(7000L, destinationUpdatedAccount.getAmount());
    }

@Test
void testConcurrentDebit2() throws Exception {

    Account account = new Account();
    account.setAmount(1000L);
    account.setAccountNumber("concurrent-123");
    accountRepository.saveAndFlush(account);

    Long accountId = account.getAccountId();

    ExecutorService executor = Executors.newFixedThreadPool(2);

    Callable<Throwable> debitTask = () -> {
        try {
            balanceInterface.debit(
                    accountId,
                    700L,
                    UUID.randomUUID().toString()
            );
            return null;
        } catch (Throwable e) {
            return e;
        }
    };

    Future<Throwable> result1 = executor.submit(debitTask);
    Future<Throwable> result2 = executor.submit(debitTask);

    Throwable exception1 = result1.get();
    Throwable exception2 = result2.get();

    executor.shutdown();

    long successfulOperations = 0;

    if (exception1 == null) {
        successfulOperations++;
    }

    if (exception2 == null) {
        successfulOperations++;
    }

    assertEquals(1, successfulOperations);

    Account updatedAccount =
            accountRepository.findById(accountId).orElseThrow();

    assertEquals(300L, updatedAccount.getAmount());
}

@Test
void testConcurrentDebit() throws Exception {

    Account account = new Account();
    account.setAmount(1000L);
    account.setAccountNumber("concurrent-123");

    accountRepository.saveAndFlush(account);

    Long accountId = account.getAccountId();

    ExecutorService executor = Executors.newFixedThreadPool(2);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<Throwable> debitTask = () -> {

        ready.countDown();

        // Wait until both threads are ready
        start.await();

        try {
            balanceInterface.debit(
                    accountId,
                    700L,
                    UUID.randomUUID().toString()
            );

            return null;

        } catch (Throwable e) {
            return e;
        }
    };

    Future<Throwable> result1 = executor.submit(debitTask);
    Future<Throwable> result2 = executor.submit(debitTask);

    // Wait until BOTH threads reached the ready point
    ready.await();

    // Now let both threads continue
    start.countDown();

    Throwable exception1 = result1.get();
    Throwable exception2 = result2.get();

    executor.shutdown();

    long successfulOperations = 0;

    if (exception1 == null) {
        successfulOperations++;
    }

    if (exception2 == null) {
        successfulOperations++;
    }

    assertEquals(1, successfulOperations);

    Account updatedAccount =
            accountRepository.findById(accountId).orElseThrow();

    assertEquals(300L, updatedAccount.getAmount());
}

@Test
void testConcurrentDebitOnDifferentAccounts() throws Exception {

    Account account1 = new Account();
    account1.setAmount(1000L);
    account1.setAccountNumber("account-1");

    Account account2 = new Account();
    account2.setAmount(1000L);
    account2.setAccountNumber("account-2");

    accountRepository.saveAndFlush(account1);
    accountRepository.saveAndFlush(account2);

    Long accountId1 = account1.getAccountId();
    Long accountId2 = account2.getAccountId();

    ExecutorService executor = Executors.newFixedThreadPool(2);

    CountDownLatch start = new CountDownLatch(1);

    Callable<Throwable> debitAccount1 = () -> {

        start.await();

        try {
            balanceInterface.debit(
                    accountId1,
                    700L,
                    UUID.randomUUID().toString()
            );

            return null;

        } catch (Throwable e) {
            return e;
        }
    };

    Callable<Throwable> debitAccount2 = () -> {

        start.await();

        try {
            balanceInterface.debit(
                    accountId2,
                    700L,
                    UUID.randomUUID().toString()
            );

            return null;

        } catch (Throwable e) {
            return e;
        }
    };

    Future<Throwable> result1 = executor.submit(debitAccount1);
    Future<Throwable> result2 = executor.submit(debitAccount2);

    // Start both threads at approximately the same time
    start.countDown();

    Throwable exception1 = result1.get();
    Throwable exception2 = result2.get();

    executor.shutdown();

    // Both operations must succeed
    assertNull(exception1);
    assertNull(exception2);

    // Check account 1
    Account updatedAccount1 =
            accountRepository.findById(accountId1).orElseThrow();

    // Check account 2
    Account updatedAccount2 =
            accountRepository.findById(accountId2).orElseThrow();

    assertEquals(300L, updatedAccount1.getAmount());
    assertEquals(300L, updatedAccount2.getAmount());
}

@Test
void testSameTransactionIdDoesNotDebitTwice() {

    Account account = new Account();
    account.setAmount(1000L);
    account.setAccountNumber("idempotent-123");

    accountRepository.saveAndFlush(account);

    Long accountId = account.getAccountId();

    String transactionId = UUID.randomUUID().toString();

    balanceInterface.debit(accountId, 700L, transactionId);

    balanceInterface.debit(accountId, 700L, transactionId);

    Account updatedAccount =
            accountRepository.findById(accountId).orElseThrow();

    assertEquals(300L, updatedAccount.getAmount());
}

@Test
void testSameTransactionIdDoesNotCreditTwice() {

    Account account = new Account();
    account.setAmount(1000L);
    account.setAccountNumber("idempotent-credit-123");

    accountRepository.saveAndFlush(account);

    Long accountId = account.getAccountId();

    String transactionId = UUID.randomUUID().toString();

    balanceInterface.credit(accountId, 1000L, transactionId);

    balanceInterface.credit(accountId, 1000L, transactionId);

    Account updatedAccount =
            accountRepository.findById(accountId).orElseThrow();

    assertEquals(2000L, updatedAccount.getAmount());
}

@Test
void testDifferentTransactionIdsBothCredit() {

    Account account = new Account();
    account.setAmount(1000L);
    account.setAccountNumber("idempotent-credit-456");

    accountRepository.saveAndFlush(account);

    Long accountId = account.getAccountId();

    balanceInterface.credit(accountId, 1000L, "credit-TX-1");

    balanceInterface.credit(accountId, 1000L, "credit-TX-2");

    Account updatedAccount =
            accountRepository.findById(accountId).orElseThrow();

    assertEquals(3000L, updatedAccount.getAmount());
}

@Test
void testDifferentTransactionIdsBothDebit() {

    Account account = new Account();
    account.setAmount(3000L);
    account.setAccountNumber("idempotent-debit-456");

    accountRepository.saveAndFlush(account);

    Long accountId = account.getAccountId();

    balanceInterface.debit(accountId, 1000L, "debit-TX-1");

    balanceInterface.debit(accountId, 1000L, "debit-TX-2");

    Account updatedAccount =
            accountRepository.findById(accountId).orElseThrow();

    assertEquals(1000L, updatedAccount.getAmount());
}

@Test
void testSameTransactionIdDoesNotTransferTwice() {

    Account source = new Account();
    source.setAmount(5000L);
    source.setAccountNumber("transfer-source-idempotent");

    Account destination = new Account();
    destination.setAmount(1000L);
    destination.setAccountNumber("transfer-destination-idempotent");

    accountRepository.saveAndFlush(source);
    accountRepository.saveAndFlush(destination);

    Long sourceId = source.getAccountId();
    Long destinationId = destination.getAccountId();

    String transactionId = UUID.randomUUID().toString();

    balanceInterface.transfer(
            sourceId,
            destinationId,
            2000L,
            transactionId
    );

    // Same transaction again
    balanceInterface.transfer(
            sourceId,
            destinationId,
            2000L,
            transactionId
    );

    Account updatedSource =
            accountRepository.findById(sourceId).orElseThrow();

    Account updatedDestination =
            accountRepository.findById(destinationId).orElseThrow();

    assertEquals(3000L, updatedSource.getAmount());
    assertEquals(3000L, updatedDestination.getAmount());
}

@Test
void testDifferentTransactionIdsBothTransfer() {

    Account source = new Account();
    source.setAmount(5000L);
    source.setAccountNumber("transfer-source-different");

    Account destination = new Account();
    destination.setAmount(1000L);
    destination.setAccountNumber("transfer-destination-different");

    accountRepository.saveAndFlush(source);
    accountRepository.saveAndFlush(destination);

    Long sourceId = source.getAccountId();
    Long destinationId = destination.getAccountId();

    balanceInterface.transfer(
            sourceId,
            destinationId,
            1000L,
            "transfer-TX-1"
    );

    balanceInterface.transfer(
            sourceId,
            destinationId,
            1000L,
            "transfer-TX-2"
    );

    Account updatedSource =
            accountRepository.findById(sourceId).orElseThrow();

    Account updatedDestination =
            accountRepository.findById(destinationId).orElseThrow();

    assertEquals(3000L, updatedSource.getAmount());
    assertEquals(3000L, updatedDestination.getAmount());
}

@Test
void testSameTransactionIdDoesNotTransferThreeTimes() {

    Account source = new Account();
    source.setAmount(5000L);
    source.setAccountNumber("transfer-source-three");

    Account destination = new Account();
    destination.setAmount(1000L);
    destination.setAccountNumber("transfer-destination-three");

    accountRepository.saveAndFlush(source);
    accountRepository.saveAndFlush(destination);

    Long sourceId = source.getAccountId();
    Long destinationId = destination.getAccountId();

    String transactionId = UUID.randomUUID().toString();

    balanceInterface.transfer(
            sourceId,
            destinationId,
            1000L,
            transactionId
    );

    balanceInterface.transfer(
            sourceId,
            destinationId,
            1000L,
            transactionId
    );

    balanceInterface.transfer(
            sourceId,
            destinationId,
            1000L,
            transactionId
    );

    Account updatedSource =
            accountRepository.findById(sourceId).orElseThrow();

    Account updatedDestination =
            accountRepository.findById(destinationId).orElseThrow();

    assertEquals(4000L, updatedSource.getAmount());
    assertEquals(2000L, updatedDestination.getAmount());
}

@Test
void testTransferIsAtomic() {

    Account source = new Account();
    source.setAmount(5000L);
    source.setAccountNumber("source");

    Account destination = new Account();
    destination.setAmount(1000L);
    destination.setAccountNumber("destination");

    accountRepository.save(source);
    accountRepository.save(destination);
    accountRepository.flush();

    doAnswer(invocation -> {

        Account account = invocation.getArgument(0);

        if (account.getAccountId().equals(destination.getAccountId())) {
            throw new RuntimeException("Simulated failure");
        }

        return invocation.callRealMethod();

    }).when(accountRepositorySpy).save(any(Account.class));

    assertThrows(
        RuntimeException.class,
        () -> balanceInterface.transfer(
            source.getAccountId(),
            destination.getAccountId(),
            2000L,
            "ATOMIC-1"
        )
    );

    Account sourceAfter =
        accountRepository.findById(source.getAccountId()).orElseThrow();

    Account destinationAfter =
        accountRepository.findById(destination.getAccountId()).orElseThrow();

    assertEquals(5000L, sourceAfter.getAmount());
    assertEquals(1000L, destinationAfter.getAmount());
}

@Test
void testGetBalance() {

    Account account = new Account();
    account.setAmount(5000L);
    account.setAccountNumber("123");

    Account savedAccount = accountRepository.save(account);

    Long balance = balanceInterface.getBalance(savedAccount.getAccountId());

    assertEquals(5000L, balance);
}

@Test
void testGetBalanceWhenAccountDoesNotExist() {

    RuntimeException exception = assertThrows(
        RuntimeException.class,
        () -> balanceInterface.getBalance(999999L)
    );

    assertEquals("Account is not exist", exception.getMessage());
}

}
