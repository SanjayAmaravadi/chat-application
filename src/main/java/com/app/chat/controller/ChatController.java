package com.app.chat.controller;

import com.app.chat.model.ChatMessage;
import com.app.chat.model.MessageType;
import com.app.chat.service.ChatService;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {

    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;

    // Online Users
    private final Set<String> onlineUsers =
            ConcurrentHashMap.newKeySet();

    public ChatController(
            ChatService chatService,
            SimpMessagingTemplate messagingTemplate
    ) {

        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sendMessage")
    public void sendMessage(@Payload ChatMessage message) {

        // First Time User Join
        if (!onlineUsers.contains(message.getSender())) {

            onlineUsers.add(message.getSender());

            ChatMessage joinMessage = new ChatMessage();

            joinMessage.setSender("System");

            joinMessage.setContent(
                    message.getSender() + " joined the chat"
            );

            joinMessage.setType(MessageType.JOIN);

            // Save Join Message
            chatService.save(joinMessage);

            // Broadcast Join Message
            messagingTemplate.convertAndSend(
                    "/topic/messages",
                    joinMessage
            );
        }

        // Normal Message
        message.setType(MessageType.CHAT);

        ChatMessage savedMessage =
                chatService.save(message);

        // Broadcast Chat Message
        messagingTemplate.convertAndSend(
                "/topic/messages",
                savedMessage
        );
    }

    @MessageMapping("/typing")
    public void typing(@Payload ChatMessage message) {

        message.setType(MessageType.TYPING);

        messagingTemplate.convertAndSend(
                "/topic/typing",
                message
        );
    }

    @GetMapping("/chat")
    public String chat(Model model) {

        List<ChatMessage> messages =
                chatService.getAllMessages();

        model.addAttribute("messages", messages);

        return "chat";
    }
}