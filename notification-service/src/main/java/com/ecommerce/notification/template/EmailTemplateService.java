package com.ecommerce.notification.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public String renderOrderConfirmation(Map<String, Object> variables) {
        log.debug("Rendering order confirmation template for orderId={}", variables.get("orderId"));
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process("order-confirmation", context);
    }

    public String renderShippingUpdate(Map<String, Object> variables) {
        log.debug("Rendering shipping update template for orderId={}", variables.get("orderId"));
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process("shipping-update", context);
    }
}
