package fr.uge.forkeat.service.exception;

public class ResourceNotFoundException extends Exception{

    public ResourceNotFoundException(String message){
        super(message);
    }

    public ResourceNotFoundException(){
        super();
    }

}
