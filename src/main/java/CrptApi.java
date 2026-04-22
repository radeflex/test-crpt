import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final String AUTH_TOKEN = "TOKEN";
    private final String BASE_URL = "https://ismp.crpt.ru";
    private final String CREATE_URI = "/api/v3/lk/documents/create";

    private final int requestLimit;
    private final TimeUnit timeUnit;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("requestLimit must be positive");
        }

        this.requestLimit = requestLimit;
        this.timeUnit = Objects.requireNonNull(timeUnit, "timeUnit is required");

        this.objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JavaTimeModule());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String createDocument(ProductDocument productDocument, String signature) {
        try {
            Objects.requireNonNull(productDocument);
            Objects.requireNonNull(signature);

            DocumentRequest request = new DocumentRequest(objectMapper, productDocument, signature);
            String json = objectMapper.writeValueAsString(request);

            return parseResponse(DocumentResponse.class, CREATE_URI, "POST", json).token;

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T parseResponse(Class<T> wannable, String uri, String method, String writableJson) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + uri))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + AUTH_TOKEN)
                    .method(method, HttpRequest.BodyPublishers.ofString(writableJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code >= 500)
                throw new RuntimeException(
                    objectMapper.readValue(response.body(), ServerErrorResponse.class).message
                );
            if (code >= 400)
                throw new RuntimeException(
                    objectMapper.readValue(response.body(), BadRequestResponse.class).errorMessage
                );
            return objectMapper.readValue(response.body(), wannable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ProductDocument {
        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private final String docType = "LP_INTRODUCE_GOODS";

        @JsonProperty("import_request")
        private Boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate productionDate;

        @JsonProperty("production_type")
        private ProductionType productionType;

        @JsonProperty("products")
        private List<Product> products;

        private ProductDocument(Builder b) {
            this.description = b.description;
            this.docId = Objects.requireNonNull(b.docId, "docId is required");
            this.docStatus = Objects.requireNonNull(b.docStatus, "docStatus is required");
            this.importRequest = b.importRequest;
            this.ownerInn = Objects.requireNonNull(b.ownerInn, "ownerInn is required");
            this.participantInn = Objects.requireNonNull(b.participantInn, "participantInn is required");
            this.producerInn = Objects.requireNonNull(b.producerInn, "producerInn is required");
            this.productionDate = Objects.requireNonNull(b.productionDate, "productionDate is required");
            this.productionType = Objects.requireNonNull(b.productionType, "productionType is required");
            this.products = b.products;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Description description;
            private String docId;
            private String docStatus;
            private Boolean importRequest;
            private String ownerInn;
            private String participantInn;
            private String producerInn;
            private LocalDate productionDate;
            private ProductionType productionType;
            private List<Product> products;

            public Builder description(Description v) {
                this.description = v;
                return this;
            }

            public Builder docId(String v) {
                this.docId = v;
                return this;
            }

            public Builder docStatus(String v) {
                this.docStatus = v;
                return this;
            }

            public Builder importRequest(Boolean v) {
                this.importRequest = v;
                return this;
            }

            public Builder ownerInn(String v) {
                this.ownerInn = v;
                return this;
            }

            public Builder participantInn(String v) {
                this.participantInn = v;
                return this;
            }

            public Builder producerInn(String v) {
                this.producerInn = v;
                return this;
            }

            public Builder productionDate(LocalDate v) {
                this.productionDate = v;
                return this;
            }

            public Builder productionType(ProductionType v) {
                this.productionType = v;
                return this;
            }

            public Builder products(List<Product> v) {
                this.products = v;
                return this;
            }

            public ProductDocument build() {
                return new ProductDocument(this);
            }
        }
    }

    public static class Description {
        @JsonProperty("participant_inn")
        private String participantInn;

        public Description(String participantInn) {
            this.participantInn = Objects.requireNonNull(participantInn, "participantInn is required");
        }
    }

    public static class Product {
        @JsonProperty("certificate_document")
        private ProductCertificateDocument certificateDocument;

        @JsonProperty("certificate_document_date")
        private LocalDate certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private LocalDate productionDate;

        @JsonProperty("tnved_code")
        private String tnvedCode;

        @JsonProperty("uit_code")
        private String uitCode;

        @JsonProperty("uitu_code")
        private String uituCode;

        private Product(Builder b) {
            this.certificateDocument = b.certificateDocument;
            this.certificateDocumentDate = b.certificateDocumentDate;
            this.certificateDocumentNumber = b.certificateDocumentNumber;
            this.ownerInn = Objects.requireNonNull(b.ownerInn, "ownerInn is required");
            this.producerInn = Objects.requireNonNull(b.producerInn, "producerInn is required");
            this.productionDate = Objects.requireNonNull(b.productionDate, "productionDate is required");
            this.tnvedCode = Objects.requireNonNull(b.tnvedCode, "tnvedCode is required");

            this.uitCode = b.uitCode;
            this.uituCode = b.uituCode;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ProductCertificateDocument certificateDocument;
            private LocalDate certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private LocalDate productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            public Builder certificateDocument(ProductCertificateDocument v) {
                this.certificateDocument = v;
                return this;
            }

            public Builder certificateDocumentDate(LocalDate v) {
                this.certificateDocumentDate = v;
                return this;
            }

            public Builder certificateDocumentNumber(String v) {
                this.certificateDocumentNumber = v;
                return this;
            }

            public Builder ownerInn(String v) {
                this.ownerInn = Objects.requireNonNull(v, "ownerInn is required");
                return this;
            }

            public Builder producerInn(String v) {
                this.producerInn = Objects.requireNonNull(v, "producerInn is required");
                return this;
            }

            public Builder productionDate(LocalDate v) {
                this.productionDate = Objects.requireNonNull(v, "productionDate is required");
                return this;
            }

            public Builder tnvedCode(String v) {
                this.tnvedCode = Objects.requireNonNull(v, "tnvedCode is required");
                return this;
            }

            public Builder uitCode(String v) {
                this.uitCode = v;
                return this;
            }

            public Builder uituCode(String v) {
                this.uituCode = v;
                return this;
            }

            public Product build() {
                if (uitCode == null && uituCode == null) {
                    throw new IllegalStateException("uitCode or uituCode is required");
                }
                return new Product(this);
            }
        }
    }

    public enum ProductionType {
        OWN_PRODUCTION,
        CONTRACT_PRODUCTION
    }

    public enum ProductCertificateDocument {
        CONFORMITY_CERTIFICATE,
        CONFORMITY_DECLARATION
    }

    private static class DocumentRequest {
        @JsonProperty("document_format")
        private final String documentFormat = "MANUAL";

        @JsonProperty("product_document")
        private final String productDocument;

        @JsonProperty("signature")
        private final String signature;

        @JsonProperty("type")
        private final String type;

        public DocumentRequest(ObjectMapper mapper, ProductDocument doc, String signature)
                throws JsonProcessingException {

            this.productDocument = Base64.getEncoder()
                    .encodeToString(mapper.writeValueAsBytes(doc));

            this.signature = Objects.requireNonNull(signature);
            this.type = "LP_INTRODUCE_GOODS";
        }
    }

    private static class DocumentResponse {
        @JsonProperty("token")
        String token;
    }

    private static class BadRequestResponse {
        @JsonProperty("error_message")
        String errorMessage;
    }

    private static class ServerErrorResponse {
        @JsonProperty("timestamp")
        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        private LocalDateTime timestamp;
        @JsonProperty("code")
        private int code;
        @JsonProperty("error")
        private String error;
        @JsonProperty("message")
        private String message;
        @JsonProperty("path")
        private String path;
    }
}