package com.KhoiCG.TMDT.modules.info.service;

import com.KhoiCG.TMDT.modules.info.dto.InfoPageDto;
import com.KhoiCG.TMDT.modules.info.entity.InfoPageEntity;
import com.KhoiCG.TMDT.modules.info.repository.InfoPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InfoPageService {

    private final InfoPageRepository repository;

    public List<InfoPageDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public Optional<InfoPageDto> findBySlug(String slug) {
        return repository.findBySlug(slug).map(this::toDto);
    }

    @Transactional
    public InfoPageDto upsert(String slug, InfoPageDto req) {
        InfoPageEntity entity = repository.findBySlug(slug)
                .orElse(InfoPageEntity.builder().slug(slug).build());
        entity.setTitle(req.getTitle());
        entity.setContentHtml(req.getContentHtml());
        return toDto(repository.save(entity));
    }

    private InfoPageDto toDto(InfoPageEntity e) {
        return InfoPageDto.builder()
                .slug(e.getSlug())
                .title(e.getTitle())
                .contentHtml(e.getContentHtml())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
