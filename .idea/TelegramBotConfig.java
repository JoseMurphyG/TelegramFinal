package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
//import org.apache.hc.core5.http.io. HttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TelegramBotConfig extends TelegramLongPollingBot {

    private Map<Long, String> userState = new HashMap<>();  // Mapa para almacenar el estado de la conversación por usuario
    private final String openAiApiKey = "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";  // Coloca tu clave API de OpenAI aquí

    @Override
    public String getBotUsername() {
        return "P2ChatTelegram_Bot";
    }

    @Override
    public String getBotToken() {
        return "7244820179:AAHpa4OIHP6WNwYtPaITA0pzn2H4OwDCAK8";
    }

    @Override
    public void onUpdateReceived(Update update) {
        String mensajeRecibido = update.getMessage().getText();
		System.out.println("Alguien envio un msj al bot: "+mensajeRecibido);
		final long chatId1 = update.getMessage().getChatId();

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String response;
            if (!userState.containsKey(chatId)) {
                // Primera interacción con el usuario
                response = "¡Hola! ¿Cuál es tu nombre?";
                userState.put(chatId, "ASKED_NAME");
            } else if (userState.get(chatId).equals("ASKED_NAME")) {
                // Respuesta a la pregunta del nombre
                response = "¡Mucho gusto, " + messageText + "! ¿Qué te gustaría saber?";
                userState.put(chatId, "ASKED_TOPIC");
            } else if (userState.get(chatId).equals("ASKED_TOPIC")) {
                // Respuesta a la pregunta sobre qué quiere saber
                response = "Me alegra que preguntes por " + messageText + ". ¡Déjame contarte algo interesante sobre ello!";
                userState.put(chatId, "DONE");
                //responder con chatGPT
                response = callChatGPT(messageText);

            } else {
                // Conversación terminada o en espera de otra interacción
                response = "¡Gracias por la conversación! Puedes preguntarme más cosas si lo deseas.";
            }

            // Enviar el mensaje de respuesta
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(response);

            try {
                execute(message);  // Enviar el mensaje
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para enviar el mensaje al usuario en Telegram
    private void sendMessageToUser(long chatId, String messageText) {
        // Verifica si la respuesta es muy larga para dividirla en fragmentos
        int maxMessageLength = 4096;  // Límite de caracteres por mensaje en Telegram
        if (messageText.length() > maxMessageLength) {
            for (int i = 0; i < messageText.length(); i += maxMessageLength) {
                String part = messageText.substring(i, Math.min(i + maxMessageLength, messageText.length()));
                sendSingleMessage(chatId, part);
            }
        } else {
            sendSingleMessage(chatId, messageText);
        }

    }

    // Método para enviar un mensaje
    private void sendSingleMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Método mejorado para interactuar con la API de ChatGPT
    private String callChatGPT(String userMessage) {
        String responseMessage = "";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("https://api.openai.com/v1/chat/completions");
            request.setHeader("Authorization", "Bearer " + openAiApiKey);
            request.setHeader("Content-Type", "application/json");

            // Crear el cuerpo de la solicitud para OpenAI
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-3.5-turbo");
            JSONObject message = new JSONObject().put("role", "user").put("content", userMessage);
            requestBody.put("messages", new JSONArray().put(message));

            StringEntity entity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            // Obtener y parsear la respuesta de ChatGPT
            try (CloseableHttpResponse response = client.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(jsonResponse);
                responseMessage = jsonObject.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            } catch (IOException | ParseException e) {
                e.printStackTrace();
                responseMessage = "Lo siento, hubo un error al procesar tu solicitud.";
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con ChatGPT", e);
        }
        return responseMessage;
    }
}


