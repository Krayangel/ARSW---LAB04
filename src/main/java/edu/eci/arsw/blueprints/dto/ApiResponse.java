package edu.eci.arsw.blueprints.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    int code,
    String message,
    T data
) {
    // Constructor para respuestas exitosas con datos
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "execute ok", data);
    }
    
    // Constructor para respuestas exitosas sin datos (creación, actualizacion)
    public static ApiResponse<Void> successWithoutData(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    // Constructor para respuestas de error
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}