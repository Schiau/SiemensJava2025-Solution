package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class ItemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private Item validItem() {
        return new Item(1L, "Item 1", "Description", "Active", "test@example.com");
    }

    @Test
    void getAllItems_shouldReturnOk() throws Exception {
        Mockito.when(itemService.findAll()).thenReturn(List.of(validItem()));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Item 1"));
    }

    @Test
    void getItemById_existing_shouldReturnOk() throws Exception {
        Mockito.when(itemService.findById(1L)).thenReturn(Optional.of(validItem()));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Item 1"));
    }

    @Test
    void getItemById_notFound_shouldReturnNotFound() throws Exception {
        Mockito.when(itemService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_valid_shouldReturnCreated() throws Exception {
        Item item = validItem();
        item.setId(null);
        Mockito.when(itemService.save(any(Item.class))).thenReturn(validItem());

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.error").value(false));
    }

    @Test
    void createItem_invalidEmail_shouldReturnBadRequest() throws Exception {
        Item invalidItem = new Item(null, "Item", "Desc", "Active", "bad-email");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true));
    }

    @Test
    void updateItem_existing_shouldReturnOk() throws Exception {
        Item updatedItem = validItem();
        Mockito.when(itemService.findById(1L)).thenReturn(Optional.of(validItem()));
        Mockito.when(itemService.save(any(Item.class))).thenReturn(updatedItem);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Item 1"));
    }

    @Test
    void updateItem_notFound_shouldReturnNotFound() throws Exception {
        Mockito.when(itemService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validItem())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_shouldReturnOk() throws Exception {
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isOk());
    }

    @Test
    void processItems_success_shouldReturnOk() throws Exception {
        Mockito.when(itemService.processItemsAsync())
                .thenReturn(CompletableFuture.completedFuture(List.of(validItem())));

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Item 1"));
    }

    @Test
    void processItems_exception_shouldReturnInternalServerError() throws Exception {
        CompletableFuture<List<Item>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Error"));

        Mockito.when(itemService.processItemsAsync()).thenReturn(failedFuture);

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isInternalServerError());
    }
}
