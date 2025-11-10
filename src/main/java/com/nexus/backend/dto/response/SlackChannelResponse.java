package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackChannelResponse {

    private String id;
    private String name;
    private Boolean isPrivate;
    private Boolean isMember;
}
