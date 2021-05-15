package com.gabriel.portal.domain.exception;

public class UsernameExistException extends Exception{
    public UsernameExistException(String message){
        super(message);
    }
}
