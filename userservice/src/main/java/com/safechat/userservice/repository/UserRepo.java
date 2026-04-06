package com.safechat.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.safechat.userservice.entity.UserEntity;

@Repository
public interface UserRepo extends JpaRepository<UserEntity,String>,JpaSpecificationExecutor<UserEntity>{
    
}
