package com.cabex.backend.utilisateur.service;

import com.cabex.backend.utilisateur.dto.PermissionResponse;
import com.cabex.backend.utilisateur.mapper.PermissionMapper;
import com.cabex.backend.utilisateur.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAll().stream()
                .map(PermissionMapper::toResponse)
                .toList();
    }
}
