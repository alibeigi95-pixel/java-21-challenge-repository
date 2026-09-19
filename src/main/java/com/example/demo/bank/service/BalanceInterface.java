package com.example.demo.bank.service;
import java.util.*;

public interface BalanceInterface {

void credit(Long accountId,Long amount,String transactionId);
void debit(Long accountId,Long amount,String transactionId);
void transfer(Long sourceAccountId,Long destinationAccountId,Long amount,String transactionId);
Long getBalance(Long accountId);

}
