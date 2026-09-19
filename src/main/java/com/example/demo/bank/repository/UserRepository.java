package com.example.demo.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.bank.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
