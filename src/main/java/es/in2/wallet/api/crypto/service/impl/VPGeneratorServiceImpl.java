package es.in2.wallet.api.crypto.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;


@Service
@RequiredArgsConstructor
public class VPGeneratorServiceImpl {
    private final ObjectMapper objectMapper;
    public Mono<Void> buildPresentableCredentialList(List<String> credentials){
        for (int i = 0; i < credentials.size(); i++){
            String credential = credentials.get(i);
            ObjectNode objectNode = toJsonObject(credential);
            String vc = objectNode.toString();


        }
        return null;
    }
    public void buildVP(){

    }


    private ObjectNode toJsonObject(String jsonCredential) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonCredential);
            ObjectNode newObject = objectMapper.createObjectNode();

            // "type"
            if (rootNode.has("type") && rootNode.get("type").isArray()) {
                ArrayNode typeArray = objectMapper.createArrayNode();
                rootNode.get("type").forEach(typeArray::add);
                newObject.set("type", typeArray);
            }

            // "@context"
            if (rootNode.has("@context") && rootNode.get("@context").isArray()) {
                ArrayNode contextArray = objectMapper.createArrayNode();
                rootNode.get("@context").forEach(contextArray::add);
                newObject.set("@context", contextArray);
            }

            // Optional fields
            copyFieldIfExists(rootNode, newObject, "id");
            copyFieldIfExists(rootNode, newObject, "issuer");
            copyFieldIfExists(rootNode, newObject, "issuanceDate");
            copyFieldIfExists(rootNode, newObject, "issued");
            copyFieldIfExists(rootNode, newObject, "validFrom");
            copyFieldIfExists(rootNode, newObject, "expirationDate");
            copyFieldIfExists(rootNode, newObject, "proof");
            copyFieldIfExists(rootNode, newObject, "credentialSchema");
            copyFieldIfExists(rootNode, newObject, "credentialSubject");

            // Custom properties
            rootNode.fields().forEachRemaining(entry -> {
                if (!newObject.has(entry.getKey())) {
                    newObject.set(entry.getKey(), entry.getValue());
                }
            });

            return newObject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON string to ObjectNode", e);
        }
    }

    private static void copyFieldIfExists(JsonNode fromNode, ObjectNode toNode, String fieldName) {
        if (fromNode.has(fieldName)) {
            toNode.set(fieldName, fromNode.get(fieldName));
        }
    }
}
