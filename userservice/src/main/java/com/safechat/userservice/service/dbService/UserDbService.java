package com.safechat.userservice.service.dbService;

import com.safechat.userservice.entity.UserEntity;
import com.safechat.userservice.repository.UserRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserDbService {

    private final UserRepo userRepository;

    public UserDbService(UserRepo userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserEntity> getUser(Specification<UserEntity> spec) {
        return userRepository.findOne(spec);
    }

    public Page<UserEntity> getUsers(Specification<UserEntity> spec, Pageable pageable) {
        return userRepository.findAll(spec, pageable);
    }

    public boolean exists(Specification<UserEntity> spec) {
        return userRepository.exists(spec);
    }

    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }

    public List<UserEntity> saveAll(List<UserEntity> users) {
        return userRepository.saveAll(users);
    }

    public void delete(Specification<UserEntity> spec) {
        List<UserEntity> users = userRepository.findAll(spec);
        userRepository.deleteAll(users);
    }
}