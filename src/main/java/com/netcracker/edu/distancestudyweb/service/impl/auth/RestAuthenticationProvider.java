package com.netcracker.edu.distancestudyweb.service.impl.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.edu.distancestudyweb.domain.ClientPrincipal;
import com.netcracker.edu.distancestudyweb.domain.Role;
import com.netcracker.edu.distancestudyweb.domain.User;
import com.netcracker.edu.distancestudyweb.dto.authentication.AuthenticationRequest;
import com.netcracker.edu.distancestudyweb.dto.authentication.AuthenticationResponse;
import com.netcracker.edu.distancestudyweb.exception.ExternalServiceException;
import com.netcracker.edu.distancestudyweb.exception.InternalServiceException;
import com.netcracker.edu.distancestudyweb.service.HttpEntityProvider;
import com.netcracker.edu.distancestudyweb.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
public class RestAuthenticationProvider implements AuthenticationProvider {
    private @Value("${rest.url}") String url;
    private final String authEndpoint = "/auth";
    private final RestTemplate restTemplate;
    private final HttpEntityProvider httpEntityProvider;
    private final ObjectMapper mapper;
    private final UserService userService;

    public RestAuthenticationProvider(RestTemplate restTemplate, HttpEntityProvider httpEntityProvider, ObjectMapper mapper, UserService userService) {
        this.restTemplate = restTemplate;
        this.httpEntityProvider = httpEntityProvider;
        this.mapper = mapper;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AuthenticationRequest restAuthRequest = null;
        try {
            restAuthRequest = new AuthenticationRequest(authentication.getName(), authentication.getCredentials().toString());
            AuthenticationResponse restAuthResponse = authenticate(restAuthRequest);
            String token = restAuthResponse.getJwtToken();
            JwtPayload payload = getJwtPayloadFromToken(restAuthResponse.getJwtToken());
            String email = payload.getSub();
            User user  = userService.getUserInfo(token, email);
            ClientPrincipal principal = ClientPrincipal.builder()
                    .user(user)
                    .token(token).build();
            return new UsernamePasswordAuthenticationToken(principal, null, user.getAuthorities().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
        } catch (JsonProcessingException e) {
            String message = "Payload has incorrect structure";
            log.error(message, e);
            throw new ExternalServiceException(message);
        } catch (ExternalServiceException e) {
            log.error("An external exception has occurred while authenticating", e);
            throw e;
        } catch (BadCredentialsException e) {
            log.trace(String.format("Bad credentials:\n\temail: %s\n\tpassword: %s", restAuthRequest.getEmail(), restAuthRequest.getPassword()));
            throw e;
        } catch (Exception e) {
            log.error("An unexpected exception has occurred while authenticating", e);
            throw new InternalServiceException(e);
        }
    }



    private AuthenticationResponse authenticate(AuthenticationRequest restAuthRequest) {
        HttpEntity<AuthenticationRequest> httpEntity = httpEntityProvider.getDefault(restAuthRequest, null);
        ResponseEntity<AuthenticationResponse> restAuthResponse = restTemplate.exchange(url + authEndpoint, HttpMethod.POST, httpEntity, AuthenticationResponse.class);
        if (restAuthResponse.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            throw new BadCredentialsException("Invalid email or password");
        }
        AuthenticationResponse authResponse = restAuthResponse.getBody();
        if (authResponse == null || authResponse.getJwtToken() == null) {
            throw new ExternalServiceException("Server has not sent token");
        }
        return authResponse;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    private JwtPayload getJwtPayloadFromToken(String token) throws JsonProcessingException {
        String[] pieces = token.split("\\.");
        String b64payload = pieces[1];
        String jsonString = new String(Base64.decodeBase64(b64payload), StandardCharsets.UTF_8);
        return mapper.readValue(jsonString, JwtPayload.class);
    }

    @Data
    private static class JwtPayload {
        private String sub;
        private List<String> authorities;
    }
}
