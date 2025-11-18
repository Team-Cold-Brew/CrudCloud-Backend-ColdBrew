package com.riwi.CrudCloud.database.repository;

import com.riwi.CrudCloud.common.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepositoryByIntance extends JpaRepository<User, Long> {}