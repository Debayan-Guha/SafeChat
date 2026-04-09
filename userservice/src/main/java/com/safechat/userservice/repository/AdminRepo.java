package com.safechat.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.safechat.userservice.entity.AdminEntity;

@Repository
public interface AdminRepo extends JpaRepository<AdminEntity,String>,JpaSpecificationExecutor<AdminEntity>{
    
}
