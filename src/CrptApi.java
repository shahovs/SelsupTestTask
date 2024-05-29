import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    // Согласно заданию:
    // 1) Решение должно быть оформлено в виде одного файла CrptApi.java.
    // Все дополнительные классы, которые используются должны быть внутренними.
    // 2) В задании необходимо сделать вызов метода создания документа.

    public static void main(String[] args) throws Exception {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 2);
        DocumentForIntroduceGoodsRF document = new DocumentForIntroduceGoodsRF();

        // заполняем тестовыми данными (можно удалить):
        Description description = new Description();
        description.setParticipantInn("7700112233");
        document.setDescription(description);
        document.setDoc_id("docId");
        document.setDoc_status("docStatus");
        document.setImportRequest(true);
        document.setOwner_inn("ownerInn");
        document.setParticipant_inn("participantInn");
        document.setProducer_inn("producerInn");
        document.setProduction_date(LocalDate.of(2022, 3, 14));
        document.setProduction_type("productionType");
        document.setReg_date(LocalDate.of(2023, 12, 22));
        document.setReg_number("regNumber");

        Product product = new Product();
        product.setCertificate_document("certDoc");
        product.setCertificate_document_date(LocalDate.of(2021, 11, 28));
        product.setCertificate_document_number("certDocNumber");
        product.setOwner_inn("ownINN");
        product.setProducer_inn("prodINN");
        product.setProduction_date(LocalDate.of(2020, 1, 27));
        product.setTnved_code("tcode");
        product.setUit_code("uit");
        product.setUitu_code("UITU");
        document.setProducts(List.of(product));

        // вызываем метод создания документа
        try {
            crptApi.createDocumentForIntroduceGoodsRF(document, "mySignature");
        } finally {
            crptApi.shutdownScheduler();
        }
    }

    private static final String DOCUMENTS_CREATE = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final TimeUnit timeUnit; // единица времени для интервала запросов
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .create();
    }

    // requestLimit - макс. кол-во запросов в заданном промежутке времени
    public CrptApi(TimeUnit timeUnit, int requestLimit) throws Exception {
        this.timeUnit = timeUnit;
        if (requestLimit < 1) {
            throw new Exception("requestLimit is not positive number");
        }
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /* При превышении лимита запрос вызов должен блокироваться, чтобы не превысить максимальное количество запросов к API
    и продолжить выполнение, когда ограничение на количество вызовов API не будет превышено в результате этого вызова.
    В любой ситуации превышать лимит на количество запросов запрещено для метода. */
    public void createDocumentForIntroduceGoodsRF(DocumentForIntroduceGoodsRF document, String signature)
            throws InterruptedException, IOException {
        semaphore.acquire();
        scheduler.scheduleAtFixedRate(semaphore::release, 1, 1, timeUnit);

        String jsonDocument = gson.toJson(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DOCUMENTS_CREATE))
                .header("Content-Type", "application/json")
                .header("signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public void shutdownScheduler() {
        scheduler.shutdown();
    }

    static class LocalDateAdapter extends TypeAdapter<LocalDate> {

        @Override
        public void write( final JsonWriter jsonWriter, final LocalDate localDate ) throws IOException {
            jsonWriter.value(localDate.toString());
        }

        @Override
        public LocalDate read( final JsonReader jsonReader ) throws IOException {
            return LocalDate.parse(jsonReader.nextString());
        }

    }

    static class DocumentForIntroduceGoodsRF {

        private Description description;
        private String doc_id;
        private String doc_status;
        public final String doc_type = "LP_INTRODUCE_GOODS";
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private LocalDate production_date;
        private String production_type;
        private List<Product> products;
        private LocalDate reg_date;
        private String reg_number;


        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public LocalDate getProduction_date() {
            return production_date;
        }

        public void setProduction_date(LocalDate production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public LocalDate getReg_date() {
            return reg_date;
        }

        public void setReg_date(LocalDate reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }

    }

    static class Description {

        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

    }

    static class Product {

        private String certificate_document;
        private LocalDate certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private LocalDate production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;


        public String getCertificate_document() {
            return certificate_document;
        }

        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        public LocalDate getCertificate_document_date() {
            return certificate_document_date;
        }

        public void setCertificate_document_date(LocalDate certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public LocalDate getProduction_date() {
            return production_date;
        }

        public void setProduction_date(LocalDate production_date) {
            this.production_date = production_date;
        }

        public String getTnved_code() {
            return tnved_code;
        }

        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        public String getUit_code() {
            return uit_code;
        }

        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        public String getUitu_code() {
            return uitu_code;
        }

        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }

    }

}