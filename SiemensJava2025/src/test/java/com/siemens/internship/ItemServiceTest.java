package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@EnableAsync
class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    private List<Item> dbItems;

    @BeforeEach
    void setUp() {
        dbItems = new ArrayList<>();
        dbItems.add(new Item(1L, "Item1", "desc", "NEW", "test1@mail.com"));
        dbItems.add(new Item(2L, "Item2", "desc", "PROCESSED", "test2@mail.com")); // Should be skipped
        dbItems.add(new Item(3L, "Item3", "desc", "NEW", "test3@mail.com"));
    }

    @Test
    void testProcessItemsAsync_allValidItemsProcessed() throws Exception {
        List<Long> ids = dbItems.stream().map(Item::getId).toList();

        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(dbItems.get(0)));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(dbItems.get(1)));
        when(itemRepository.findById(3L)).thenReturn(Optional.of(dbItems.get(2)));

        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));


        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get(5, TimeUnit.SECONDS);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(i -> "PROCESSED".equals(i.getStatus())));
        verify(itemRepository, times(2)).save(any(Item.class));
        verify(itemRepository, times(3)).findById(anyLong());
    }

    @Test
    void testProcessItemsAsync_withExceptionHandling() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(List.of(1L));
        when(itemRepository.findById(1L)).thenThrow(new RuntimeException("DB error"));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();

        assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS));
    }
}
