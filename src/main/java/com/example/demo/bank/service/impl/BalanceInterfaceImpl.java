package com.example.demo.bank.service.impl;

import com.example.demo.bank.entity.*;
import com.example.demo.bank.repository.AccountRepository;
import com.example.demo.bank.repository.TransactionRepository;
import com.example.demo.bank.service.BalanceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceInterfaceImpl implements BalanceInterface {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override 
    @Transactional 
    public void credit(Long accountId,Long amount,String transactionId){

        if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
           return;
        }

        internalCredit(accountId,amount);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void debit(Long accountId,Long amount,String transactionId){

        // Check if this transaction was already processed
        if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
            return;
        }

        internalDebit(accountId,amount);

        // Remember that this transaction was processed
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transactionRepository.save(transaction);          
    }

    @Override
    @Transactional
    public void transfer(Long sourceAccountId,Long destinationAccountId,Long amount,String transactionId){
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new RuntimeException("Source and destination accounts must be different.");
        }
        if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
            return;
        }
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new RuntimeException("sourceAccount is not exist"));
        Account destinationAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new RuntimeException("destinationAccount is not exist"));
        internalDebit(sourceAccountId,amount);
        internalCredit(destinationAccountId, amount);
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transactionRepository.save(transaction);   
    }

    private void internalDebit(Long accountId,Long amount){

        if (amount <= 0) {
            throw new RuntimeException("Amount must be greater than zero.");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account is not exist"));

        if (account.getAmount() < amount) {
            throw new RuntimeException("account's amount is not enough.");
        }

        account.setAmount(account.getAmount() - amount);
        accountRepository.save(account);
    }

    private void internalCredit(Long accountId,Long amount){

        if (amount <= 0) {
            throw new RuntimeException("Amount must be greater than zero.");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account is not exist"));

        account.setAmount(account.getAmount() + amount);
        accountRepository.save(account);
    }

    @Override 
    public Long getBalance(Long accountId){
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account is not exist"));
        return account.getAmount();    
    }

}
