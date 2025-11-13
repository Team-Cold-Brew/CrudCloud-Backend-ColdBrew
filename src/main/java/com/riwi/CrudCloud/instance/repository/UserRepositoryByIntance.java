package com.riwi.CrudCloud.instance.repository;


import com.riwi.CrudCloud.instance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepositoryByIntance extends JpaRepository<User, Long> {}