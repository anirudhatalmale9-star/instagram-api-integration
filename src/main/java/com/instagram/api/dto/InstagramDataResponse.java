package com.instagram.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramDataResponse {

    private InstagramProfileDTO profile;
    private List<InstagramMediaDTO> media;
    private PagingInfo paging;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagingInfo {
        private String nextCursor;
        private String previousCursor;
        private boolean hasMore;
    }
}
