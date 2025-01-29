package com.avalanche.Certificates_Distribution.controllers;

import com.avalanche.Certificates_Distribution.models.User;
import com.avalanche.Certificates_Distribution.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@Slf4j
public class UserController {
    private final UserService userService;


    UserController(UserService userService){
        this.userService=userService;
    }

    @PostMapping("/import")
    public ResponseEntity<String> importUsers(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a CSV file");
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("Please upload a valid CSV file");
            }

            int importedCount = userService.importUsersFromCsv(file);
            return ResponseEntity.ok("Successfully imported " + importedCount + " users");

        } catch (Exception e) {
            log.error("Error importing users from CSV: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error importing users: " + e.getMessage());
        }
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendCertificates() {
        try {
            int sentCount = userService.sendCertificates();
            return ResponseEntity.ok("Sent " + sentCount + " certificates successfully");
        } catch (Exception e) {
            log.error("Error sending certificates: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending certificates: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return userService.getEmailStats();
    }

    @GetMapping("/failed")
    public List<User> getFailedDeliveries() {
        return userService.getFailedDeliveries();
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<String> retryFailedEmails() {
        userService.retryFailedEmails();
        return ResponseEntity.ok("Retry process initiated");
    }
}
