package com.safechat.userservice.service.dbService;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.entity.PendingUserDeletionEntity;
import com.safechat.userservice.repository.PendingUserDeletionRepo;

@Service
public class PendingUserDeletionDbService {
    
    private final PendingUserDeletionRepo pendingUserDeletionRepo;

    public PendingUserDeletionDbService(PendingUserDeletionRepo pendingUserDeletionRepo) {
        this.pendingUserDeletionRepo = pendingUserDeletionRepo;
    }

    public Optional<PendingUserDeletionEntity> getPendingDeletion(Specification<PendingUserDeletionEntity> spec) {
        return pendingUserDeletionRepo.findOne(spec);
    }

     public Page<PendingUserDeletionEntity> getPendingDeletions(Specification<PendingUserDeletionEntity> spec, Pageable pageable) {
        return pendingUserDeletionRepo.findAll(spec, pageable);
    }

    public boolean exists(Specification<PendingUserDeletionEntity> spec) {
        return pendingUserDeletionRepo.exists(spec);
    }

    public void save(PendingUserDeletionEntity entity) {
        pendingUserDeletionRepo.save(entity);
    }

    public void saveAll(List<PendingUserDeletionEntity> entities) {
        pendingUserDeletionRepo.saveAll(entities);
    }

    public void delete(PendingUserDeletionEntity entity) {
        pendingUserDeletionRepo.delete(entity);
    }

    public void deleteAll(List<PendingUserDeletionEntity> entities) {
        pendingUserDeletionRepo.deleteAll(entities);
    }
}
