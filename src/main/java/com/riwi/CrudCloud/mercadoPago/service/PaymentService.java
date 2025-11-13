package com.riwi.CrudCloud.mercadoPago.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.exceptions.MPApiException;

import com.riwi.CrudCloud.auth.model.Plan;
import com.riwi.CrudCloud.auth.model.User;
import com.riwi.CrudCloud.auth.repository.PlanRepository;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import com.riwi.CrudCloud.auth.util.exception.classes.ResourceNotFoundException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.InvalidPaymentDataException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.MercadoPagoException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.PaymentNotFoundException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.PaymentProcessingException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.WebhookValidationException;
import com.riwi.CrudCloud.mercadoPago.dto.request.CreatePreferenceRequest;
import com.riwi.CrudCloud.mercadoPago.dto.response.CheckoutProResponse;
import com.riwi.CrudCloud.mercadoPago.dto.response.TransactionResponse;
import com.riwi.CrudCloud.mercadoPago.dto.response.WebhookResponse;
import com.riwi.CrudCloud.mercadoPago.model.Currency;
import com.riwi.CrudCloud.mercadoPago.model.PaymentProvider;
import com.riwi.CrudCloud.mercadoPago.model.Transaction;
import com.riwi.CrudCloud.mercadoPago.model.TransactionStatus;
import com.riwi.CrudCloud.mercadoPago.repository.CurrencyRepository;
import com.riwi.CrudCloud.mercadoPago.repository.PaymentProviderRepository;
import com.riwi.CrudCloud.mercadoPago.repository.TransactionRepository;

/**
 * Service class for payment processing with MercadoPago
 */
@Service
public class PaymentService {

    @Autowired
    private PreferenceClient preferenceClient;

    @Autowired
    private PaymentClient paymentClient;

    @Value("${mercadopago.public-key:}")
    private String publicKey;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentProviderRepository paymentProviderRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    /**
     * Create a payment preference for Checkout Pro
     *
     * @param request the preference creation request
     * @return CheckoutProResponse with preference details and public key
     * @throws MercadoPagoException if MercadoPago API error occurs
     * @throws ResourceNotFoundException if user or plan not found
     */
    @Transactional
    public CheckoutProResponse createPreference(CreatePreferenceRequest request) {
        // Validate user exists
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        // Validate plan exists
        Plan plan = planRepository.findById(request.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + request.getPlanId()));

        // Get or create MercadoPago provider
        PaymentProvider provider = getOrCreateMercadoPagoProvider();

        // Get or create currency
        Currency currency = getOrCreateCurrency(request.getCurrency());

        try {
            // Create preference item
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Plan Upgrade - " + plan.getName())
                .description(request.getDescription() != null ? request.getDescription() : "Upgrade to " + plan.getName() + " plan")
                .quantity(1)
                .currencyId(request.getCurrency())
                .unitPrice(request.getAmount())
                .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(item);

            // Create back URLs for redirect after payment
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success("https://your-frontend-url.com/payment/success")
                .failure("https://your-frontend-url.com/payment/failure")
                .pending("https://your-frontend-url.com/payment/pending")
                .build();

            // Create preference request
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference("USER_" + user.getUserId() + "_PLAN_" + plan.getPlanId())
                .notificationUrl("https://your-backend-url.com/api/payments/webhook")
                .build();

            // Create preference with MercadoPago
            Preference preference = preferenceClient.create(preferenceRequest);

            // Create transaction record
            Transaction transaction = Transaction.builder()
                .user(user)
                .organizationId(request.getOrganizationId())
                .provider(provider)
                .currency(currency)
                .providerTransactionId(preference.getId())
                .amount(request.getAmount())
                .status(TransactionStatus.PENDING)
                .build();

            Transaction savedTransaction = transactionRepository.save(transaction);

            return new CheckoutProResponse(
                preference.getId(),
                publicKey,
                savedTransaction.getTransactionId()
            );

        } catch (MPException | MPApiException e) {
            throw new MercadoPagoException("Error creating MercadoPago preference: " + e.getMessage(), e);
        }
    }

    /**
     * Process webhook notification from MercadoPago
     *
     * @param webhookData the webhook payload
     * @return WebhookResponse
     */
    @Transactional
    public WebhookResponse processWebhook(Map<String, Object> webhookData) {
        String type = null;
        try {
            type = (String) webhookData.get("type");
            
            if (!"payment".equals(type)) {
                return WebhookResponse.success(null, "ignored");
            }

            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            String paymentId = (String) data.get("id");

            // Get payment details from MercadoPago
            Payment payment = paymentClient.get(Long.parseLong(paymentId));

            // Find transaction by external reference or preference ID
            String externalReference = payment.getExternalReference();
            Transaction transaction = findTransactionByReference(externalReference, payment.getId().toString());

            if (transaction == null) {
                throw new PaymentNotFoundException("Transaction not found for payment ID: " + paymentId, null, paymentId);
            }

            // Update transaction based on payment status
            updateTransactionStatus(transaction, payment);

            // If payment is approved, upgrade user plan
            if (payment.getStatus().equals("approved")) {
                upgradeUserPlan(transaction, externalReference);
            }

            return WebhookResponse.success(
                transaction.getTransactionId().toString(),
                payment.getStatus()
            );

        } catch (PaymentNotFoundException e) {
            throw e; // Re-throw payment specific exceptions
        } catch (Exception e) {
            throw new WebhookValidationException("Error processing webhook: " + e.getMessage(), 
                type != null ? type : "unknown", webhookData.toString());
        }
    }

    /**
     * Get transaction history for a user
     *
     * @param userId the user ID
     * @return list of transaction responses
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Integer userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        return transactions.stream()
            .map(this::mapToTransactionResponse)
            .toList();
    }

    /**
     * Get transaction by ID
     *
     * @param transactionId the transaction ID
     * @return TransactionResponse
     * @throws PaymentNotFoundException if transaction not found
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Integer transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new PaymentNotFoundException("Transaction not found with ID: " + transactionId, transactionId.toString()));

        return mapToTransactionResponse(transaction);
    }

    // Private helper methods

    private PaymentProvider getOrCreateMercadoPagoProvider() {
        return paymentProviderRepository.findMercadoPagoProvider()
            .orElseGet(() -> {
                PaymentProvider provider = PaymentProvider.builder()
                    .name("MERCADO_PAGO")
                    .active(true)
                    .build();
                return paymentProviderRepository.save(provider);
            });
    }

    private Currency getOrCreateCurrency(String currencyCode) {
        return currencyRepository.findByCurrency(currencyCode)
            .orElseGet(() -> {
                Currency currency = Currency.builder()
                    .name(getCurrencyName(currencyCode))
                    .currency(currencyCode)
                    .build();
                return currencyRepository.save(currency);
            });
    }

    private String getCurrencyName(String currencyCode) {
        return switch (currencyCode) {
            case "ARS" -> "Argentine Peso";
            case "USD" -> "US Dollar";
            case "EUR" -> "Euro";
            case "BRL" -> "Brazilian Real";
            default -> currencyCode;
        };
    }

    private Transaction findTransactionByReference(String externalReference, String paymentId) {
        // Try to find by provider transaction ID first
        return transactionRepository.findByProviderTransactionId(paymentId)
            .orElse(null);
    }

    private void updateTransactionStatus(Transaction transaction, Payment payment) {
        transaction.setPaymentMethod(payment.getPaymentMethodId());
        
        switch (payment.getStatus()) {
            case "approved" -> transaction.approve();
            case "rejected", "cancelled" -> transaction.reject();
            case "refunded" -> transaction.refund();
            // pending status remains as is
        }

        transactionRepository.save(transaction);
    }

    private void upgradeUserPlan(Transaction transaction, String externalReference) {
        // Extract plan ID from external reference
        if (externalReference != null && externalReference.contains("PLAN_")) {
            try {
                String planIdStr = externalReference.substring(externalReference.indexOf("PLAN_") + 5);
                Integer planId = Integer.parseInt(planIdStr);
                
                Plan newPlan = planRepository.findById(planId).orElse(null);
                if (newPlan != null) {
                    User user = transaction.getUser();
                    user.setPersonalPlan(newPlan);
                    userRepository.save(user);
                }
            } catch (Exception e) {
                // Log error but don't fail the webhook processing
                System.err.println("Error upgrading user plan: " + e.getMessage());
            }
        }
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.getTransactionId(),
            transaction.getProviderTransactionId(),
            transaction.getAmount(),
            transaction.getCurrency() != null ? transaction.getCurrency().getCurrency() : null,
            transaction.getStatus().toString(),
            transaction.getPaymentMethod(),
            transaction.getCreatedAt(),
            transaction.getApprovalDate(),
            transaction.getUser().getUserId(),
            transaction.getUser().getUsername(),
            transaction.getOrganizationId()
        );
    }
}
