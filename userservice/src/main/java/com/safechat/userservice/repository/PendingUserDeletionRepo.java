package com.safechat.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.safechat.userservice.entity.PendingUserDeletionEntity;

@Repository
public interface PendingUserDeletionRepo extends JpaRepository<PendingUserDeletionEntity,String>,JpaSpecificationExecutor<PendingUserDeletionEntity>{
    
}
