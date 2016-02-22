package de.zalando.mass.ratwrap.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.annotation.ContextParam;
import de.zalando.mass.ratwrap.annotation.Filtered;
import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.controller.TestNotBeanFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@RequestController(uri = "secured/")
@Filtered({OAuth2SecurityFilter.class, TestNotBeanFilter.class})
public class SecuredController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredController.class);

    @Autowired
    private ObjectMapper objectMapper;

    @RequestHandler(uri = "verify", produce = "text/plain")
    public String checkTokenInfo(@ContextParam final TokenInfo tokenInfo) throws Exception {
        return tokenInfo.getAccessToken();
    }
}
