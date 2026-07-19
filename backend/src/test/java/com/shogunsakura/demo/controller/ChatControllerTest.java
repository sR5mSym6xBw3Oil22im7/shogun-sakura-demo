package com.shogunsakura.demo.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shogunsakura.demo.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  ChatService chatService;

  @Test
  void chatReturnsAnswer() throws Exception {
    when(chatService.answer(anyString())).thenReturn("税込のデモ価格で4,800円です。");

    mockMvc.perform(post("/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"価格は？\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("税込のデモ価格で4,800円です。"));
  }

  @Test
  void chatRejectsBlankMessage() throws Exception {
    mockMvc.perform(post("/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"   \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void chatRejectsTooLongMessage() throws Exception {
    String message = "あ".repeat(501);

    mockMvc.perform(post("/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"" + message + "\"}"))
        .andExpect(status().isBadRequest());
  }
}
