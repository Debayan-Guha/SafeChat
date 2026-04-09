package com.safechat.userservice.service.dbService;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.entity.AdminEntity;
import com.safechat.userservice.repository.AdminRepo;

@Service
public class AdminDbService {

    private final AdminRepo adminRepository;

    public AdminDbService(AdminRepo adminRepository) {
        this.adminRepository = adminRepository;
    }

    public Optional<AdminEntity> getAdmin(Specification<AdminEntity> spec) {
        return adminRepository.findOne(spec);
    }

    public Page<AdminEntity> getAdmins(Specification<AdminEntity> spec, Pageable pageable) {
        return adminRepository.findAll(spec, pageable);
    }

    public boolean exists(Specification<AdminEntity> spec) {
        return adminRepository.exists(spec);
    }

    public AdminEntity save(AdminEntity admin) {
        return adminRepository.save(admin);
    }

    public List<AdminEntity> saveAll(List<AdminEntity> admins) {
        return adminRepository.saveAll(admins);
    }

    public void delete(AdminEntity admin) {
        adminRepository.delete(admin);
    }

    public void deleteAll(List<AdminEntity> admins) {
        adminRepository.deleteAll(admins);
    }

}