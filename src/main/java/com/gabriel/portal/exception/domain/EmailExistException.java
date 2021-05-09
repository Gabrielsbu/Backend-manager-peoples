package com.gabriel.portal.exception.domain;

public class EmailExistException extends Exception{
    public EmailExistException(String message) {
        super(message);
    }
}
