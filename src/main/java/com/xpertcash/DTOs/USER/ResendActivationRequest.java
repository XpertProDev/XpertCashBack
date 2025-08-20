package com.xpertcash.DTOs.USER;

import lombok.Data;

@Data
public class ResendActivationRequest {
     private String email;

     // Getter et Setter
        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }

}
