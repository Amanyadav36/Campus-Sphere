package com.example.campus_sphere;

public class ChatMessage {
    public String text;
    public boolean isUser; // true = User message, false = AI message

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }
}