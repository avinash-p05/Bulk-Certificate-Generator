package com.avalanche.Certificates_Distribution.models;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @CsvBindByName(column = "username")
    private String username;

    @CsvBindByName(column = "email")
    private String email;

    @CsvBindByName(column = "usn")
    private String usn;

    private boolean emailSent = false;
    private String errorMessage;
}

