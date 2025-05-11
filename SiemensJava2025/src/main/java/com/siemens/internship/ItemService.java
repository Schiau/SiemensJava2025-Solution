package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    //make a list which is thread safe using Collections.synchronizedList
    private List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());
    //modify from int which is not thread safe to AtomicInteger which is safe for async operations
    private AtomicInteger processedCount = new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        //get all the ids from DB
        List<Long> itemIds = itemRepository.findAllIds();

        //make an array with all the futures
        List<CompletableFuture<Item>> futures = new ArrayList<>();

        //reinitialize processedItems for every call
        processedItems = Collections.synchronizedList(new ArrayList<>());

        ////reinitialize processedCount for every call
        processedCount = new AtomicInteger(0);

        //iterate over every id
        for (Long id : itemIds) {
            //create a new future
            CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                try {
                    //simulate time for each thread
                    Thread.sleep(100);

                    //fetch every item from id
                    Item item = itemRepository.findById(id).orElse(null);

                    //skip the id if it was not found or is already processed
                    if (item == null || "PROCESSED".equals(item.getStatus())) {
                        return null;
                    }

                    //update the status of the item
                    item.setStatus("PROCESSED");

                    //update the processedCount for each item
                    processedCount.incrementAndGet();

                    //save the change to the DB and send the new updated item
                    return itemRepository.save(item);
                } catch (Exception e) {
                    // if an exception is throw in a future interrupt it and trow a new wxception
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Error processing item " + id, e);
                }
            }, executor);
            futures.add(future);
        }

        // wait for all futures to complete and collect non-null processed items
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        //wait to finish
                        .map(CompletableFuture::join)
                        //filter for the successfully PROCESSED items
                        .filter(item -> item != null)
                        //collect in a list
                        .toList()
                );
    }

}

