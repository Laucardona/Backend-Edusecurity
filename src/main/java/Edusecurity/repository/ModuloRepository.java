package Edusecurity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import Edusecurity.entity.ModuloEducativo;

public interface ModuloRepository
extends JpaRepository<
        ModuloEducativo,
        Long> {

}