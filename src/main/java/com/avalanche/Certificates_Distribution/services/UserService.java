package com.avalanche.Certificates_Distribution.services;

import com.avalanche.Certificates_Distribution.models.User;
import com.avalanche.Certificates_Distribution.repositories.UserRepository;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    UserService(UserRepository userRepository, JavaMailSender mailSender){
        this.userRepository=userRepository;
        this.mailSender=mailSender;
    }


    @Value("${certificate.template.path}")
    private String templatePath;

    public void importUsers(List<User> users) {
        users.forEach(user -> {
            user.setEmailSent(false);
            user.setErrorMessage(null);
        });
        userRepository.saveAll(users);
        log.info("Imported {} users", users.size());
    }

    public int importUsersFromCsv(MultipartFile file) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<User> csvToBean = new CsvToBeanBuilder<User>(reader)
                    .withType(User.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            List<User> users = csvToBean.parse();

            // Validate users
            List<User> validUsers = validateUsers(users);

            // Save to database
            userRepository.saveAll(validUsers);

            log.info("Imported {} users from CSV", validUsers.size());
            return validUsers.size();

        } catch (Exception e) {
            log.error("Error parsing CSV file: ", e);
            throw new RuntimeException("Error parsing CSV file: " + e.getMessage());
        }
    }

    private List<User> validateUsers(List<User> users) {
        return users.stream()
                .filter(user -> {
                    boolean isValid = true;
                    StringBuilder errors = new StringBuilder();

                    // Check required fields
                    if (StringUtils.isEmpty(user.getUsername())) {
                        errors.append("Username is required. ");
                        isValid = false;
                    }

                    if (StringUtils.isEmpty(user.getEmail())) {
                        errors.append("Email is required. ");
                        isValid = false;
                    } else if (!isValidEmail(user.getEmail())) {
                        errors.append("Invalid email format. ");
                        isValid = false;
                    }

                    if (StringUtils.isEmpty(user.getUsn())) {
                        errors.append("USN is required. ");
                        isValid = false;
                    }

                    // Set validation status
                    if (!isValid) {
                        user.setErrorMessage(errors.toString());
                    }

                    return isValid;
                })
                .collect(Collectors.toList());
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    public int sendCertificates() throws IOException {
        List<User> pendingUsers = userRepository.findByEmailSentFalse();
        int successCount = 0;

        for (User user : pendingUsers) {
            try {
                // Generate certificate
                byte[] certificatePdf = generateCertificate(user);

                // Send email
                sendEmail(user.getEmail(),user.getUsername(), certificatePdf);

                // Update user status
                user.setEmailSent(true);
                user.setErrorMessage(null);
                userRepository.save(user);

                successCount++;
                log.info("Certificate sent successfully to: {}", user.getEmail());
            } catch (Exception e) {
                user.setErrorMessage(e.getMessage());
                userRepository.save(user);
                log.error("Failed to send certificate to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        return successCount;
    }

    private byte[] generateCertificate(User user) throws IOException, DocumentException {
        // Load template image
        BufferedImage template = ImageIO.read(new File(templatePath));

        // Create graphics object with antialiasing
        Graphics2D g2d = template.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            // Load custom font for the name
            Font nameFont = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/Amsterdam One 400.ttf"))
                    .deriveFont(Font.PLAIN, 42);
            g2d.setFont(nameFont);
        } catch (IOException | FontFormatException e) {
            System.err.println("Error loading custom font for name: " + e.getMessage());
            e.printStackTrace();
            g2d.setFont(new Font("Times New Roman", Font.PLAIN, 42)); // Fallback font
        }

        g2d.setColor(Color.BLACK); // Set color for text

        // Coordinates for name
        int nameStartX = 544;
        int nameEndX = 1070;
        int nameStartY = 390;
        int nameEndY = 490;
        int nameWidth = nameEndX - nameStartX;
        int nameHeight = nameEndY - nameStartY;

        // Get the name text
        String name = formatName(user.getUsername());

        // Get font metrics for the name
        FontMetrics nameMetrics = g2d.getFontMetrics();
        int nameX = nameStartX + (nameWidth - nameMetrics.stringWidth(name)) / 2;
        int nameY = nameStartY + ((nameHeight + nameMetrics.getHeight()) / 2);

        // Draw the name
        g2d.drawString(name, nameX, nameY);

        try {
            // Load custom font for the USN
            Font usnFont = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/LeoHand-Light.ttf"))
                    .deriveFont(Font.BOLD, 42);
            g2d.setFont(usnFont);
        } catch (IOException | FontFormatException e) {
            System.err.println("Error loading custom font for USN: " + e.getMessage());
            e.printStackTrace();
            g2d.setFont(new Font("Arial", Font.BOLD, 42)); // Fallback font
        }

        // Coordinates for USN
        int usnStartX = 270;
        int usnEndX = 530; // Calculated as 561 + 232
        int usnStartY = 510;
        int usnEndY = 550;
        int usnWidth = usnEndX - usnStartX;
        int usnHeight = usnEndY - usnStartY;

        // Get the USN text
        String usn = user.getUsn().toUpperCase();

        // Get font metrics for the USN
        FontMetrics usnMetrics = g2d.getFontMetrics();
        int usnX = usnStartX + (usnWidth - usnMetrics.stringWidth(usn)) / 2;
        int usnY = usnStartY + ((usnHeight + usnMetrics.getHeight()) / 2);

        // Draw the USN
        g2d.drawString(usn, usnX, usnY);

        g2d.dispose();

        // Convert to PDF maintaining original dimensions
        ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
        com.lowagie.text.Rectangle pageSize = new com.lowagie.text.Rectangle(template.getWidth(), template.getHeight());
        Document document = new Document(pageSize, 0, 0, 0, 0); // Zero margins
        PdfWriter.getInstance(document, pdfOutput);
        document.open();

        com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(template, null);
        image.setAbsolutePosition(0, 0);
        image.scaleAbsolute(pageSize.getWidth(), pageSize.getHeight());
        document.add(image);
        document.close();

        return pdfOutput.toByteArray();
    }


    // Add a debug method to visualize the text areas (useful during development)
    private void debugDrawTextAreas(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        // Draw name area
        g2d.drawRect(799, 710, 1744-799, 789-710);
        // Draw USN area
        g2d.drawRect(342, 807, 1047-342, 866-807);
    }

    // Add this helper method to calculate center position if needed
    private int calculateCenterPosition(int containerWidth, int textWidth) {
        return (containerWidth - textWidth) / 2;
    }

    // Add this utility method to get font metrics for a specific font
    private FontMetrics getFontMetrics(Font font) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        g2d.dispose();
        return metrics;
    }

    private void sendEmail(String email, String name, byte[] certificatePdf) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("🎉 Congratulations on Your Achievement, " + name + "! 🎓");

            String body = String.format(
                    "Hi %s, 🌟\n\n" +
                            "Congratulations on successfully participating in our event! 🥳\n" +
                            "We truly appreciate your effort and enthusiasm. Your contribution made a big difference, and we’re thrilled to celebrate your achievement. 🙌\n\n" +
                            "🎖 Attached is your Certificate of Participation. Be proud of your accomplishment, and keep shining!\n\n" +
                            "Best Regards, 💐\n" +
                            "Team Avalanche 2024",
                    name
            );

            helper.setText(body);
            helper.addAttachment("certificate.pdf", new ByteArrayResource(certificatePdf));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // Method to capitalize the first letter of each word and lowercase the rest
    private String formatName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return fullName; // Return as is for null or empty input
        }
        StringBuilder formattedName = new StringBuilder();
        String[] words = fullName.trim().split("\\s+"); // Split by spaces
        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0))) // Capitalize first letter
                        .append(word.substring(1).toLowerCase())       // Make the rest lowercase
                        .append(" ");                                // Add space between words
            }
        }
        return formattedName.toString().trim(); // Remove trailing space
    }




    public List<User> getFailedDeliveries() {
        return userRepository.findByErrorMessageNotNull();
    }

    public void retryFailedEmails() {
        List<User> failedUsers = userRepository.findByEmailSentAndErrorMessageNotNull(false);
        // Process these users again
    }

    public Map<String, Long> getEmailStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", userRepository.countByEmailSentFalse());
        stats.put("sent", userRepository.countByEmailSentTrue());
        stats.put("failed", (long) userRepository.findByErrorMessageNotNull().size());
        return stats;
    }
}
