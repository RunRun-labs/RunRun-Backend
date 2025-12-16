package com.multi.runrunbackend.common.exception.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@ToString
public class ApiExceptionDto {


    private int state;
    private String message;

    public ApiExceptionDto(HttpStatus state, String message) {
        this.state = state.value();
        this.message = message;
    }

}