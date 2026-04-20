package org.serwin.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstanceTokenRequest {

    @JsonProperty("instance_id")
    private String instanceId;

    @JsonProperty("user_id")
    private String userId;
}