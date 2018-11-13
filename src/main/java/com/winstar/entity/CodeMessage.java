package com.winstar.entity;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CodeMessage {
	private Integer code;
	private String message;
}
