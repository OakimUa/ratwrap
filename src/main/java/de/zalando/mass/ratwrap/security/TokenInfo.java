package de.zalando.mass.ratwrap.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@EqualsAndHashCode
public class TokenInfo {
    private String uid;
    private List<String> scope;
    private String grantType;
    private String cn;
    private String realm;
    private String tokenType;
    private Long expiresIn;
    private String accessToken;
    private String error;
    private String errorDescription;
}
