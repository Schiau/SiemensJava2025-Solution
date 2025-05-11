package com.siemens.internship.dtos;

import com.siemens.internship.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseCreateItemDto {
    private String message = "";
    private boolean isError = false;
    private Item item = null;
}
