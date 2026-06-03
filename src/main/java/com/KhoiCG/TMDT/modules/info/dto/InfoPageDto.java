package com.KhoiCG.TMDT.modules.info.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoPageDto {
    private String slug;
    private String title;
    private String contentHtml;
    private LocalDateTime updatedAt;
}
