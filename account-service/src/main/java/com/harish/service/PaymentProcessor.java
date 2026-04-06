package com.harish.service;

import com.harish.dto.subscription.CheckoutRequest;
import com.harish.dto.subscription.CheckoutResponse;
import com.harish.dto.subscription.PortalResponse;
import com.stripe.model.StripeObject;

import java.util.Map;

public interface PaymentProcessor {

    CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request);

    PortalResponse openCustomerPortal();

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);

}
