package com.whoj;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@AllArgsConstructor
public class FindErrorCodeRequest {
    private String code;
    private String errMsg;
}
