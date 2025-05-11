package com.siemens.internship;

import com.siemens.internship.dtos.ResponseCreateItemDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        //fetch items from service
        List<Item> items = itemService.findAll();

        //send them to the user whit 200 code
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ResponseCreateItemDto> createItem(@RequestBody @Valid Item item, BindingResult result) {
        //wrapper to be more specific when it is an error
        ResponseCreateItemDto responseCreateItemDto = new ResponseCreateItemDto();

        if (result.hasErrors()) {
            //if the email is not in a correct format or and mandatory filed is missing send a 400 Bad Request code and create the ResponseCreateItemDto
            responseCreateItemDto.setError(true);
            responseCreateItemDto.setMessage(result.getAllErrors().get(0).getDefaultMessage());
            return new ResponseEntity<>(responseCreateItemDto, HttpStatus.BAD_REQUEST);
        }

        //save the item to the DB
        Item savedItem = itemService.save(item);
        responseCreateItemDto.setItem(savedItem);
        responseCreateItemDto.setError(false);

        //send the wrapper with a 200 OK code
        return new ResponseEntity<>(responseCreateItemDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        //fetch an item from DB by id
        return itemService.findById(id)
                //if the item is present send it with 200 OK code
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                //if the id do not have a corresponding item send a 404 NOT FOUND code
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody Item item) {
        //fetch the item from DB by id
        Optional<Item> existingItem = itemService.findById(id);

        //check if the item exist
        if (existingItem.isPresent()) {
            // set the updated it with the same id
            item.setId(id);

            //override(update) the old item with the new one and send 200 OK with the new item
            return new ResponseEntity<>(itemService.save(item), HttpStatus.OK);
        } else {

            // if the item do not exist return 404 code
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        //delete the item from the DB, if exist
        itemService.deleteById(id);

        //return an empty body with code 200 OK
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() {
        //start all the futures
        CompletableFuture<List<Item>> future = itemService.processItemsAsync();

        try {
            //try to get all the processed items
            List<Item> processed = future.get();

            //send the processed items to the user
            return new ResponseEntity<>(processed, HttpStatus.OK);
        } catch (Exception e) {
            //send an empty body with INTERNAL_SERVER_ERROR code
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
