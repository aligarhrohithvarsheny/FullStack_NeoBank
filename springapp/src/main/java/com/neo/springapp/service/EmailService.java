package com.neo.springapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

@Service
public class EmailService {
    
    private Object mailSender;
    private boolean mailAvailable = false;
    
    @Autowired(required = false)
    private ApplicationContext applicationContext;
    
    /**
     * Initialize mail sender if available
     */
    private void initializeMailSender() {
        if (mailAvailable) {
            return; // Already initialized
        }
        
        System.out.println("==========================================");
        System.out.println("Initializing Email Service...");
        System.out.println("ApplicationContext available: " + (applicationContext != null));
        
        try {
            // Try to get JavaMailSender bean from context
            Class<?> mailSenderClass = Class.forName("org.springframework.mail.javamail.JavaMailSender");
            System.out.println("JavaMailSender class found");
            
            if (applicationContext != null) {
                try {
                    Object bean = applicationContext.getBean(mailSenderClass);
                    if (bean != null) {
                        this.mailSender = bean;
                        this.mailAvailable = true;
                        System.out.println("✅ Email service initialized with JavaMailSender");
                        System.out.println("Mail sender bean type: " + bean.getClass().getName());
                    }
                } catch (Exception e) {
                    // Bean not found, mail not configured
                    System.out.println("❌ JavaMailSender bean not found in context");
                    System.out.println("Error: " + e.getMessage());
                    this.mailAvailable = false;
                }
            } else {
                System.out.println("❌ ApplicationContext is null");
                this.mailAvailable = false;
            }
        } catch (ClassNotFoundException e) {
            // Mail dependency not available
            System.out.println("❌ JavaMailSender class not found - mail dependency missing");
            this.mailAvailable = false;
        }
        
        System.out.println("Mail available: " + this.mailAvailable);
        System.out.println("==========================================");
    }
    
    /**
     * Send OTP email to user
     */
    public boolean sendOtpEmail(String toEmail, String otp) {
        // Initialize mail sender if not already done
        initializeMailSender();
        
        try {
            // If mail sender is not configured, log OTP to console (for development)
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("EMAIL OTP (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("OTP: " + otp);
                System.out.println("==========================================");
                return true; // Return true for development
            }
            
            // Use reflection to send email if mail dependency is available
            Class<?> simpleMailMessageClass = Class.forName("org.springframework.mail.SimpleMailMessage");
            Object message = simpleMailMessageClass.getDeclaredConstructor().newInstance();
            
            Method setToMethod = simpleMailMessageClass.getMethod("setTo", String.class);
            Method setSubjectMethod = simpleMailMessageClass.getMethod("setSubject", String.class);
            Method setTextMethod = simpleMailMessageClass.getMethod("setText", String.class);
            Method setFromMethod = simpleMailMessageClass.getMethod("setFrom", String.class);
            
            setToMethod.invoke(message, toEmail);
            setSubjectMethod.invoke(message, "NeoBank - Login OTP Verification");
            setTextMethod.invoke(message, buildOtpEmailBody(otp));
            // Use the configured email username as the "from" address
            String fromEmail = getFromEmail();
            setFromMethod.invoke(message, fromEmail);
            System.out.println("Email 'From' address set to: " + fromEmail);
            
            System.out.println("Attempting to send email...");
            System.out.println("Mail sender available: " + (mailSender != null));
            System.out.println("Mail sender class: " + (mailSender != null ? mailSender.getClass().getName() : "null"));
            
            // Try to find the send method - it might be send(SimpleMailMessage) or send(MimeMessage)
            Method sendMethod = null;
            try {
                sendMethod = mailSender.getClass().getMethod("send", Object.class);
            } catch (NoSuchMethodException e) {
                // Try with SimpleMailMessage class
                try {
                    sendMethod = mailSender.getClass().getMethod("send", simpleMailMessageClass);
                } catch (Exception e2) {
                    throw new RuntimeException("Could not find send method", e2);
                }
            }
            
            System.out.println("Invoking send method...");
            sendMethod.invoke(mailSender, message);
            
            System.out.println("==========================================");
            System.out.println("✅ OTP email sent successfully!");
            System.out.println("To: " + toEmail);
            System.out.println("From: " + (applicationContext != null ? getFromEmail() : "noreply@neobank.com"));
            System.out.println("==========================================");
            return true;
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("FAILED TO SEND OTP EMAIL");
            System.err.println("To: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            System.err.println("Error Type: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            System.err.println("==========================================");
            
            // In development, still log the OTP even if email fails
            System.out.println("==========================================");
            System.out.println("EMAIL OTP (Email sending failed - Development Mode)");
            System.out.println("To: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Please check the error above for email configuration issues.");
            System.out.println("==========================================");
            
            return true; // Return true for development (allow login even if email fails)
        }
    }
    
    /**
     * Get the 'from' email address from configuration
     */
    private String getFromEmail() {
        try {
            Class<?> environmentClass = Class.forName("org.springframework.core.env.Environment");
            if (applicationContext != null) {
                Object environment = applicationContext.getBean(environmentClass);
                Method getPropertyMethod = environmentClass.getMethod("getProperty", String.class);
                String fromEmail = (String) getPropertyMethod.invoke(environment, "spring.mail.username");
                if (fromEmail != null && !fromEmail.isEmpty()) {
                    return fromEmail;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not get from email from config: " + e.getMessage());
        }
        return "noreply@neobank.com";
    }
    
    /**
     * Send password reset OTP email to user
     */
    public boolean sendPasswordResetOtpEmail(String toEmail, String otp) {
        // Initialize mail sender if not already done
        initializeMailSender();
        
        try {
            // If mail sender is not configured, log OTP to console (for development)
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("PASSWORD RESET OTP EMAIL (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("OTP: " + otp);
                System.out.println("==========================================");
                return true; // Return true for development
            }
            
            // Use reflection to send email if mail dependency is available
            Class<?> simpleMailMessageClass = Class.forName("org.springframework.mail.SimpleMailMessage");
            Object message = simpleMailMessageClass.getDeclaredConstructor().newInstance();
            
            Method setToMethod = simpleMailMessageClass.getMethod("setTo", String.class);
            Method setSubjectMethod = simpleMailMessageClass.getMethod("setSubject", String.class);
            Method setTextMethod = simpleMailMessageClass.getMethod("setText", String.class);
            Method setFromMethod = simpleMailMessageClass.getMethod("setFrom", String.class);
            
            setToMethod.invoke(message, toEmail);
            setSubjectMethod.invoke(message, "NeoBank - Password Reset OTP");
            setTextMethod.invoke(message, buildPasswordResetOtpEmailBody(otp));
            // Use the configured email username as the "from" address
            String fromEmail = getFromEmail();
            setFromMethod.invoke(message, fromEmail);
            System.out.println("Email 'From' address set to: " + fromEmail);
            
            System.out.println("Attempting to send password reset OTP email...");
            System.out.println("Mail sender available: " + (mailSender != null));
            
            // Try to find the send method
            Method sendMethod = null;
            try {
                sendMethod = mailSender.getClass().getMethod("send", Object.class);
            } catch (NoSuchMethodException e) {
                try {
                    sendMethod = mailSender.getClass().getMethod("send", simpleMailMessageClass);
                } catch (Exception e2) {
                    throw new RuntimeException("Could not find send method", e2);
                }
            }
            
            System.out.println("Invoking send method...");
            sendMethod.invoke(mailSender, message);
            
            System.out.println("==========================================");
            System.out.println("✅ Password reset OTP email sent successfully!");
            System.out.println("To: " + toEmail);
            System.out.println("From: " + (applicationContext != null ? getFromEmail() : "noreply@neobank.com"));
            System.out.println("==========================================");
            return true;
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("FAILED TO SEND PASSWORD RESET OTP EMAIL");
            System.err.println("To: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            System.err.println("==========================================");
            e.printStackTrace();
            
            // In development, still log the OTP even if email fails
            System.out.println("==========================================");
            System.out.println("PASSWORD RESET OTP EMAIL (Email sending failed - Development Mode)");
            System.out.println("To: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Please check the error above for email configuration issues.");
            System.out.println("==========================================");
            
            return true; // Return true for development (allow password reset even if email fails)
        }
    }

    /**
     * Build email body for OTP
     */
    private String buildOtpEmailBody(String otp) {
        return "Dear Customer,\n\n" +
               "Your One-Time Password (OTP) for NeoBank login is:\n\n" +
               otp + "\n\n" +
               "This OTP is valid for 5 minutes only.\n\n" +
               "Please do not share this OTP with anyone. NeoBank will never ask for your OTP via phone call or email.\n\n" +
               "If you did not request this OTP, please ignore this email or contact our support team immediately.\n\n" +
               "Thank you for banking with NeoBank!\n\n" +
               "Best regards,\n" +
               "NeoBank Security Team";
    }

    /**
     * Build email body for password reset OTP
     */
    private String buildPasswordResetOtpEmailBody(String otp) {
        return "Dear Customer,\n\n" +
               "Your One-Time Password (OTP) for NeoBank password reset is:\n\n" +
               otp + "\n\n" +
               "This OTP is valid for 5 minutes only.\n\n" +
               "Please do not share this OTP with anyone. NeoBank will never ask for your OTP via phone call or email.\n\n" +
               "If you did not request a password reset, please ignore this email or contact our support team immediately.\n\n" +
               "Thank you for banking with NeoBank!\n\n" +
               "Best regards,\n" +
               "NeoBank Security Team";
    }

    /**
     * Send KYC update OTP email to user
     */
    public boolean sendKycUpdateOtpEmail(String toEmail, String otp) {
        // Initialize mail sender if not already done
        initializeMailSender();
        
        try {
            // If mail sender is not configured, log OTP to console (for development)
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("KYC UPDATE OTP EMAIL (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("OTP: " + otp);
                System.out.println("==========================================");
                return true; // Return true for development
            }
            
            // Use reflection to send email if mail dependency is available
            Class<?> simpleMailMessageClass = Class.forName("org.springframework.mail.SimpleMailMessage");
            Object message = simpleMailMessageClass.getDeclaredConstructor().newInstance();
            
            Method setToMethod = simpleMailMessageClass.getMethod("setTo", String.class);
            Method setSubjectMethod = simpleMailMessageClass.getMethod("setSubject", String.class);
            Method setTextMethod = simpleMailMessageClass.getMethod("setText", String.class);
            Method setFromMethod = simpleMailMessageClass.getMethod("setFrom", String.class);
            
            setToMethod.invoke(message, toEmail);
            setSubjectMethod.invoke(message, "NeoBank - KYC Update OTP");
            setTextMethod.invoke(message, buildKycUpdateOtpEmailBody(otp));
            // Use the configured email username as the "from" address
            String fromEmail = getFromEmail();
            setFromMethod.invoke(message, fromEmail);
            System.out.println("Email 'From' address set to: " + fromEmail);
            
            System.out.println("Attempting to send KYC update OTP email...");
            System.out.println("Mail sender available: " + (mailSender != null));
            
            // Try to find the send method
            Method sendMethod = null;
            try {
                sendMethod = mailSender.getClass().getMethod("send", Object.class);
            } catch (NoSuchMethodException e) {
                try {
                    sendMethod = mailSender.getClass().getMethod("send", simpleMailMessageClass);
                } catch (Exception e2) {
                    throw new RuntimeException("Could not find send method", e2);
                }
            }
            
            System.out.println("Invoking send method...");
            sendMethod.invoke(mailSender, message);
            
            System.out.println("==========================================");
            System.out.println("✅ KYC update OTP email sent successfully!");
            System.out.println("To: " + toEmail);
            System.out.println("From: " + (applicationContext != null ? getFromEmail() : "noreply@neobank.com"));
            System.out.println("==========================================");
            return true;
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("FAILED TO SEND KYC UPDATE OTP EMAIL");
            System.err.println("To: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            System.err.println("==========================================");
            e.printStackTrace();
            
            // In development, still log the OTP even if email fails
            System.out.println("==========================================");
            System.out.println("KYC UPDATE OTP EMAIL (Email sending failed - Development Mode)");
            System.out.println("To: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Please check the error above for email configuration issues.");
            System.out.println("==========================================");
            
            return true; // Return true for development (allow KYC update even if email fails)
        }
    }

    /**
     * Build email body for KYC update OTP
     */
    private String buildKycUpdateOtpEmailBody(String otp) {
        return "Dear Customer,\n\n" +
               "Your One-Time Password (OTP) for NeoBank KYC update request is:\n\n" +
               otp + "\n\n" +
               "This OTP is valid for 5 minutes only.\n\n" +
               "Please do not share this OTP with anyone. NeoBank will never ask for your OTP via phone call or email.\n\n" +
               "If you did not request a KYC update, please ignore this email or contact our support team immediately.\n\n" +
               "Thank you for banking with NeoBank!\n\n" +
               "Best regards,\n" +
               "NeoBank Security Team";
    }

    /**
     * Send login notification email to user for security purposes
     */
    public boolean sendLoginNotificationEmail(String toEmail, String username, String loginTimestamp) {
        // Initialize mail sender if not already done
        initializeMailSender();
        
        try {
            // If mail sender is not configured, log to console (for development)
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("LOGIN NOTIFICATION EMAIL (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("Username: " + username);
                System.out.println("Login Timestamp: " + loginTimestamp);
                System.out.println("==========================================");
                return true; // Return true for development
            }
            
            // Use reflection to send email if mail dependency is available
            Class<?> simpleMailMessageClass = Class.forName("org.springframework.mail.SimpleMailMessage");
            Object message = simpleMailMessageClass.getDeclaredConstructor().newInstance();
            
            Method setToMethod = simpleMailMessageClass.getMethod("setTo", String.class);
            Method setSubjectMethod = simpleMailMessageClass.getMethod("setSubject", String.class);
            Method setTextMethod = simpleMailMessageClass.getMethod("setText", String.class);
            Method setFromMethod = simpleMailMessageClass.getMethod("setFrom", String.class);
            
            setToMethod.invoke(message, toEmail);
            setSubjectMethod.invoke(message, "NeoBank - Account Login Notification");
            setTextMethod.invoke(message, buildLoginNotificationEmailBody(username, loginTimestamp));
            // Use the configured email username as the "from" address
            String fromEmail = getFromEmail();
            setFromMethod.invoke(message, fromEmail);
            System.out.println("Email 'From' address set to: " + fromEmail);
            
            System.out.println("Attempting to send login notification email...");
            System.out.println("Mail sender available: " + (mailSender != null));
            
            // Try to find the send method
            Method sendMethod = null;
            try {
                sendMethod = mailSender.getClass().getMethod("send", Object.class);
            } catch (NoSuchMethodException e) {
                try {
                    sendMethod = mailSender.getClass().getMethod("send", simpleMailMessageClass);
                } catch (Exception e2) {
                    throw new RuntimeException("Could not find send method", e2);
                }
            }
            
            System.out.println("Invoking send method...");
            sendMethod.invoke(mailSender, message);
            
            System.out.println("==========================================");
            System.out.println("✅ Login notification email sent successfully!");
            System.out.println("To: " + toEmail);
            System.out.println("From: " + (applicationContext != null ? getFromEmail() : "noreply@neobank.com"));
            System.out.println("==========================================");
            return true;
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("FAILED TO SEND LOGIN NOTIFICATION EMAIL");
            System.err.println("To: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            System.err.println("==========================================");
            e.printStackTrace();
            
            // In development, still log even if email fails
            System.out.println("==========================================");
            System.out.println("LOGIN NOTIFICATION EMAIL (Email sending failed - Development Mode)");
            System.out.println("To: " + toEmail);
            System.out.println("Username: " + username);
            System.out.println("Login Timestamp: " + loginTimestamp);
            System.out.println("Please check the error above for email configuration issues.");
            System.out.println("==========================================");
            
            return true; // Return true for development (allow login even if email fails)
        }
    }

    /**
     * Build email body for login notification
     */
    private String buildLoginNotificationEmailBody(String username, String loginTimestamp) {
        return "Dear " + username + ",\n\n" +
               "This is a security notification from NeoBank.\n\n" +
               "Your account was successfully logged into internet banking at:\n" +
               loginTimestamp + "\n\n" +
               "If you recognize this login, no further action is required.\n\n" +
               "If you did NOT log in to your account, please:\n" +
               "1. Change your password immediately\n" +
               "2. Contact our support team at support@neobank.com\n" +
               "3. Review your account activity for any unauthorized transactions\n\n" +
               "For your security, we recommend:\n" +
               "- Using a strong, unique password\n" +
               "- Enabling two-factor authentication\n" +
               "- Not sharing your login credentials with anyone\n\n" +
               "Thank you for banking with NeoBank!\n\n" +
               "Best regards,\n" +
               "NeoBank Security Team\n\n" +
               "This is an automated security notification. Please do not reply to this email.";
    }

    /**
     * Send account tracking email to user with tracking ID and status
     */
    public boolean sendAccountTrackingEmail(String toEmail, String username, String trackingId, String aadharNumber, String status) {
        // Initialize mail sender if not already done
        initializeMailSender();
        
        try {
            // If mail sender is not configured, log to console (for development)
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("ACCOUNT TRACKING EMAIL (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("Username: " + username);
                System.out.println("Tracking ID: " + trackingId);
                System.out.println("Aadhar Number: " + aadharNumber);
                System.out.println("Status: " + status);
                System.out.println("==========================================");
                return true; // Return true for development
            }
            
            // Use reflection to send email if mail dependency is available
            Class<?> simpleMailMessageClass = Class.forName("org.springframework.mail.SimpleMailMessage");
            Object message = simpleMailMessageClass.getDeclaredConstructor().newInstance();
            
            Method setToMethod = simpleMailMessageClass.getMethod("setTo", String.class);
            Method setSubjectMethod = simpleMailMessageClass.getMethod("setSubject", String.class);
            Method setTextMethod = simpleMailMessageClass.getMethod("setText", String.class);
            Method setFromMethod = simpleMailMessageClass.getMethod("setFrom", String.class);
            
            setToMethod.invoke(message, toEmail);
            setSubjectMethod.invoke(message, "NeoBank - Account Creation Tracking ID");
            setTextMethod.invoke(message, buildAccountTrackingEmailBody(username, trackingId, aadharNumber, status));
            // Use the configured email username as the "from" address
            String fromEmail = getFromEmail();
            setFromMethod.invoke(message, fromEmail);
            System.out.println("Email 'From' address set to: " + fromEmail);
            
            System.out.println("Attempting to send account tracking email...");
            System.out.println("Mail sender available: " + (mailSender != null));
            
            // Try to find the send method
            Method sendMethod = null;
            try {
                sendMethod = mailSender.getClass().getMethod("send", Object.class);
            } catch (NoSuchMethodException e) {
                try {
                    sendMethod = mailSender.getClass().getMethod("send", simpleMailMessageClass);
                } catch (Exception e2) {
                    throw new RuntimeException("Could not find send method", e2);
                }
            }
            
            System.out.println("Invoking send method...");
            sendMethod.invoke(mailSender, message);
            
            System.out.println("==========================================");
            System.out.println("✅ Account tracking email sent successfully!");
            System.out.println("To: " + toEmail);
            System.out.println("From: " + (applicationContext != null ? getFromEmail() : "noreply@neobank.com"));
            System.out.println("Tracking ID: " + trackingId);
            System.out.println("==========================================");
            return true;
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("FAILED TO SEND ACCOUNT TRACKING EMAIL");
            System.err.println("To: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            System.err.println("==========================================");
            e.printStackTrace();
            
            // In development, still log even if email fails
            System.out.println("==========================================");
            System.out.println("ACCOUNT TRACKING EMAIL (Email sending failed - Development Mode)");
            System.out.println("To: " + toEmail);
            System.out.println("Username: " + username);
            System.out.println("Tracking ID: " + trackingId);
            System.out.println("Aadhar Number: " + aadharNumber);
            System.out.println("Status: " + status);
            System.out.println("Please check the error above for email configuration issues.");
            System.out.println("==========================================");
            
            return true; // Return true for development (allow account creation even if email fails)
        }
    }

    /**
     * Build email body for account tracking
     */
    private String buildAccountTrackingEmailBody(String username, String trackingId, String aadharNumber, String status) {
        String statusMessage = "";
        switch (status) {
            case "PENDING":
                statusMessage = "Your account creation request is pending admin review.";
                break;
            case "ADMIN_SEEN":
                statusMessage = "Your account creation request has been seen by admin and is under review.";
                break;
            case "ADMIN_APPROVED":
                statusMessage = "Your account creation request has been approved by admin.";
                break;
            case "ADMIN_SENT":
                statusMessage = "Your account details have been sent by admin. Please check your email for further instructions.";
                break;
            default:
                statusMessage = "Your account creation request status: " + status;
        }
        
        return "Dear " + username + ",\n\n" +
               "Thank you for creating an account with NeoBank!\n\n" +
               "Your account creation request has been received and is being processed.\n\n" +
               "TRACKING DETAILS:\n" +
               "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
               "Tracking ID: " + trackingId + "\n" +
               "Aadhar Number: " + aadharNumber + "\n" +
               "Current Status: " + status + "\n\n" +
               statusMessage + "\n\n" +
               "You can use your Tracking ID to check the status of your account creation request.\n\n" +
               "STATUS UPDATES:\n" +
               "• PENDING - Your request is waiting for admin review\n" +
               "• ADMIN_SEEN - Admin has viewed your request\n" +
               "• ADMIN_APPROVED - Your account has been approved\n" +
               "• ADMIN_SENT - Account details have been sent to you\n\n" +
               "We will notify you via email when your account status changes.\n\n" +
               "If you have any questions, please contact our support team at support@neobank.com or call 1800 103 1906.\n\n" +
               "Thank you for choosing NeoBank!\n\n" +
               "Best regards,\n" +
               "NeoBank Customer Service Team\n\n" +
               "This is an automated email. Please do not reply to this email.";
    }

    /**
     * Send bank statement PDF via email
     */
    public boolean sendBankStatementEmail(String toEmail, String accountNumber, String userName, byte[] pdfBytes) {
        // Initialize mail sender if not already done
        initializeMailSender();
        
        try {
            // If mail sender is not configured, log to console (for development)
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("BANK STATEMENT EMAIL (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("Account Number: " + accountNumber);
                System.out.println("User Name: " + userName);
                System.out.println("PDF Size: " + (pdfBytes != null ? pdfBytes.length + " bytes" : "null"));
                System.out.println("Note: Email would be sent if mail configuration was available.");
                System.out.println("==========================================");
                return true; // Return true for development
            }
            
            System.out.println("Attempting to send bank statement email with attachment...");
            System.out.println("Mail sender class: " + mailSender.getClass().getName());
            
            // Use reflection to send email with attachment if mail dependency is available
            Class<?> mimeMessageClass = Class.forName("javax.mail.internet.MimeMessage");
            Class<?> mimeMessageHelperClass = Class.forName("org.springframework.mail.javamail.MimeMessageHelper");
            Class<?> dataSourceClass = Class.forName("javax.activation.DataSource");
            
            // Try Spring's ByteArrayDataSource first, then fall back to javax.activation
            Class<?> byteArrayDataSourceClass = null;
            try {
                byteArrayDataSourceClass = Class.forName("org.springframework.mail.javamail.ByteArrayDataSource");
                System.out.println("Using Spring's ByteArrayDataSource");
            } catch (ClassNotFoundException e) {
                // Fall back to javax.activation.ByteArrayDataSource
                try {
                    byteArrayDataSourceClass = Class.forName("javax.activation.ByteArrayDataSource");
                    System.out.println("Using javax.activation.ByteArrayDataSource");
                } catch (ClassNotFoundException e2) {
                    System.err.println("Could not find ByteArrayDataSource class");
                    throw new RuntimeException("ByteArrayDataSource not found", e2);
                }
            }
            
            // Create MimeMessage
            Method createMimeMessageMethod = mailSender.getClass().getMethod("createMimeMessage");
            Object mimeMessage = createMimeMessageMethod.invoke(mailSender);
            System.out.println("MimeMessage created successfully");
            
            // Create MimeMessageHelper
            Object helper = mimeMessageHelperClass.getConstructor(mimeMessageClass, boolean.class)
                .newInstance(mimeMessage, true);
            System.out.println("MimeMessageHelper created successfully");
            
            // Set email properties
            Method setToMethod = mimeMessageHelperClass.getMethod("setTo", String.class);
            Method setSubjectMethod = mimeMessageHelperClass.getMethod("setSubject", String.class);
            Method setTextMethod = mimeMessageHelperClass.getMethod("setText", String.class, boolean.class);
            Method setFromMethod = mimeMessageHelperClass.getMethod("setFrom", String.class);
            Method addAttachmentMethod = mimeMessageHelperClass.getMethod("addAttachment", String.class, dataSourceClass);
            
            String fromEmail = getFromEmail();
            setFromMethod.invoke(helper, fromEmail);
            setToMethod.invoke(helper, toEmail);
            setSubjectMethod.invoke(helper, "NeoBank - Account Statement");
            setTextMethod.invoke(helper, buildBankStatementEmailBody(userName, accountNumber), true);
            System.out.println("Email properties set: From=" + fromEmail + ", To=" + toEmail);
            
            // Create DataSource for PDF attachment
            Object dataSource = byteArrayDataSourceClass.getConstructor(byte[].class, String.class)
                .newInstance(pdfBytes, "application/pdf");
            
            String fileName = "Bank_Statement_" + accountNumber + "_" + 
                            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            addAttachmentMethod.invoke(helper, fileName, dataSource);
            System.out.println("PDF attachment added: " + fileName + " (" + pdfBytes.length + " bytes)");
            
            // Send the email
            Method sendMimeMessageMethod = mailSender.getClass().getMethod("send", mimeMessageClass);
            System.out.println("Sending email...");
            
            try {
                sendMimeMessageMethod.invoke(mailSender, mimeMessage);
                
                System.out.println("==========================================");
                System.out.println("✅ Bank statement email sent successfully!");
                System.out.println("To: " + toEmail);
                System.out.println("From: " + fromEmail);
                System.out.println("Attachment: " + fileName);
                System.out.println("==========================================");
                return true;
            } catch (Exception sendException) {
                // Catch sending errors specifically
                System.err.println("==========================================");
                System.err.println("EMAIL SENDING FAILED (Exception during send)");
                System.err.println("To: " + toEmail);
                System.err.println("Error: " + sendException.getMessage());
                if (sendException.getCause() != null) {
                    System.err.println("Cause: " + sendException.getCause().getMessage());
                    System.err.println("Cause Type: " + sendException.getCause().getClass().getName());
                }
                sendException.printStackTrace();
                System.err.println("==========================================");
                throw sendException; // Re-throw to be caught by outer catch
            }
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("FAILED TO SEND BANK STATEMENT EMAIL");
            System.err.println("To: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            System.err.println("Error Type: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            System.err.println("==========================================");
            
            // In development, still log even if email fails and return true
            // This allows the feature to work even if email is not properly configured
            System.out.println("==========================================");
            System.out.println("BANK STATEMENT EMAIL (Email sending failed - Development Mode)");
            System.out.println("To: " + toEmail);
            System.out.println("Account Number: " + accountNumber);
            System.out.println("User Name: " + userName);
            System.out.println("PDF would have been sent if email was properly configured.");
            System.out.println("Please check the error above for email configuration issues.");
            System.out.println("==========================================");
            
            // Return true for development mode to allow the feature to work
            // The user will see a success message, but email won't actually be sent
            // In production, you may want to return false here
            return true; // Changed to true for development mode consistency
        }
    }

    /**
     * Build email body for bank statement
     */
    private String buildBankStatementEmailBody(String userName, String accountNumber) {
        return "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
               "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
               "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;'>" +
               "<h2 style='margin: 0;'>🏦 NeoBank</h2>" +
               "<p style='margin: 5px 0 0 0; font-size: 14px;'>Account Statement</p>" +
               "</div>" +
               "<div style='background: #f8f9fa; padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 8px 8px;'>" +
               "<p>Dear " + (userName != null ? userName : "Valued Customer") + ",</p>" +
               "<p>Thank you for banking with NeoBank. Please find attached your account statement for account number <strong>" + accountNumber + "</strong>.</p>" +
               "<p>The statement includes all your recent transactions and current account balance.</p>" +
               "<div style='background: white; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #667eea;'>" +
               "<p style='margin: 0; font-size: 12px; color: #666;'><strong>Important:</strong></p>" +
               "<ul style='margin: 10px 0; padding-left: 20px; font-size: 12px; color: #666;'>" +
               "<li>This is a computer-generated statement and does not require signature</li>" +
               "<li>Please keep this statement secure and do not share with unauthorized persons</li>" +
               "<li>For any queries, contact us at support@neobank.com or 1800 103 1906</li>" +
               "</ul>" +
               "</div>" +
               "<p>If you have any questions or concerns about your account, please don't hesitate to contact our customer support team.</p>" +
               "<p style='margin-top: 30px;'>Best regards,<br>" +
               "<strong>NeoBank Customer Service Team</strong></p>" +
               "<hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
               "<p style='font-size: 11px; color: #999; text-align: center;'>" +
               "This is an automated email. Please do not reply to this message.<br>" +
               "© " + java.time.Year.now() + " NeoBank India Limited. All rights reserved." +
               "</p>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }

    /**
     * Send loan foreclosure PDF via email
     */
    public boolean sendForeclosureEmail(String toEmail, String loanAccountNumber, String userName, byte[] pdfBytes) {
        // Initialize mail sender if not already done
        initializeMailSender();
        
        try {
            // If mail sender is not configured, log to console (for development)
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("FORECLOSURE EMAIL (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("Loan Account Number: " + loanAccountNumber);
                System.out.println("User Name: " + userName);
                System.out.println("PDF Size: " + (pdfBytes != null ? pdfBytes.length + " bytes" : "null"));
                System.out.println("==========================================");
                return true; // Return true for development
            }
            
            System.out.println("Attempting to send foreclosure email with attachment...");
            System.out.println("Mail sender class: " + mailSender.getClass().getName());
            
            // Use reflection to send email with attachment
            Class<?> mimeMessageClass = Class.forName("javax.mail.internet.MimeMessage");
            Class<?> mimeMessageHelperClass = Class.forName("org.springframework.mail.javamail.MimeMessageHelper");
            Class<?> dataSourceClass = Class.forName("javax.activation.DataSource");
            
            // Try Spring's ByteArrayDataSource first, then fall back to javax.activation
            Class<?> byteArrayDataSourceClass = null;
            try {
                byteArrayDataSourceClass = Class.forName("org.springframework.mail.javamail.ByteArrayDataSource");
                System.out.println("Using Spring's ByteArrayDataSource");
            } catch (ClassNotFoundException e) {
                try {
                    byteArrayDataSourceClass = Class.forName("javax.activation.ByteArrayDataSource");
                    System.out.println("Using javax.activation.ByteArrayDataSource");
                } catch (ClassNotFoundException e2) {
                    System.err.println("Could not find ByteArrayDataSource class");
                    throw new RuntimeException("ByteArrayDataSource not found", e2);
                }
            }
            
            // Create MimeMessage
            Method createMimeMessageMethod = mailSender.getClass().getMethod("createMimeMessage");
            Object mimeMessage = createMimeMessageMethod.invoke(mailSender);
            System.out.println("MimeMessage created successfully");
            
            // Create MimeMessageHelper
            Object helper = mimeMessageHelperClass.getConstructor(mimeMessageClass, boolean.class)
                .newInstance(mimeMessage, true);
            System.out.println("MimeMessageHelper created successfully");
            
            // Set email properties
            Method setToMethod = mimeMessageHelperClass.getMethod("setTo", String.class);
            Method setSubjectMethod = mimeMessageHelperClass.getMethod("setSubject", String.class);
            Method setTextMethod = mimeMessageHelperClass.getMethod("setText", String.class, boolean.class);
            Method setFromMethod = mimeMessageHelperClass.getMethod("setFrom", String.class);
            Method addAttachmentMethod = mimeMessageHelperClass.getMethod("addAttachment", String.class, dataSourceClass);
            
            String fromEmail = getFromEmail();
            setFromMethod.invoke(helper, fromEmail);
            setToMethod.invoke(helper, toEmail);
            setSubjectMethod.invoke(helper, "NeoBank - Loan Foreclosure Statement");
            setTextMethod.invoke(helper, buildForeclosureEmailBody(userName, loanAccountNumber), true);
            System.out.println("Email properties set: From=" + fromEmail + ", To=" + toEmail);
            
            // Create DataSource for PDF attachment
            Object dataSource = byteArrayDataSourceClass.getConstructor(byte[].class, String.class)
                .newInstance(pdfBytes, "application/pdf");
            
            String fileName = "Foreclosure_Statement_" + loanAccountNumber + "_" + 
                            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            addAttachmentMethod.invoke(helper, fileName, dataSource);
            System.out.println("PDF attachment added: " + fileName + " (" + pdfBytes.length + " bytes)");
            
            // Send the email
            Method sendMimeMessageMethod = mailSender.getClass().getMethod("send", mimeMessageClass);
            System.out.println("Sending email...");
            
            try {
                sendMimeMessageMethod.invoke(mailSender, mimeMessage);
                
                System.out.println("==========================================");
                System.out.println("✅ Foreclosure email sent successfully!");
                System.out.println("To: " + toEmail);
                System.out.println("From: " + fromEmail);
                System.out.println("Attachment: " + fileName);
                System.out.println("==========================================");
                return true;
            } catch (Exception sendException) {
                System.err.println("==========================================");
                System.err.println("EMAIL SENDING FAILED (Exception during send)");
                System.err.println("To: " + toEmail);
                System.err.println("Error: " + sendException.getMessage());
                if (sendException.getCause() != null) {
                    System.err.println("Cause: " + sendException.getCause().getMessage());
                }
                sendException.printStackTrace();
                System.err.println("==========================================");
                throw sendException;
            }
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("FAILED TO SEND FORECLOSURE EMAIL");
            System.err.println("To: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
            
            System.out.println("==========================================");
            System.out.println("FORECLOSURE EMAIL (Email sending failed - Development Mode)");
            System.out.println("To: " + toEmail);
            System.out.println("Loan Account Number: " + loanAccountNumber);
            System.out.println("User Name: " + userName);
            System.out.println("==========================================");
            
            return true; // Return true for development mode
        }
    }

    /**
     * Build email body for foreclosure
     */
    private String buildForeclosureEmailBody(String userName, String loanAccountNumber) {
        return "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
               "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
               "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;'>" +
               "<h2 style='margin: 0;'>🏦 NeoBank</h2>" +
               "<p style='margin: 5px 0 0 0; font-size: 14px;'>Loan Foreclosure Statement</p>" +
               "</div>" +
               "<div style='background: #f8f9fa; padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 8px 8px;'>" +
               "<p>Dear " + (userName != null ? userName : "Valued Customer") + ",</p>" +
               "<p>Your loan account <strong>" + loanAccountNumber + "</strong> has been successfully foreclosed.</p>" +
               "<p>Please find attached the detailed foreclosure statement containing:</p>" +
               "<ul style='margin: 15px 0; padding-left: 20px;'>" +
               "<li>Foreclosure amount breakdown</li>" +
               "<li>Principal and interest calculations</li>" +
               "<li>Foreclosure charges (4%) and GST (18%)</li>" +
               "<li>Total amount paid for foreclosure</li>" +
               "</ul>" +
               "<div style='background: white; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #667eea;'>" +
               "<p style='margin: 0; font-size: 12px; color: #666;'><strong>Important:</strong></p>" +
               "<ul style='margin: 10px 0; padding-left: 20px; font-size: 12px; color: #666;'>" +
               "<li>This is a computer-generated statement and does not require signature</li>" +
               "<li>The foreclosure amount has been debited from your account</li>" +
               "<li>Your loan account is now closed</li>" +
               "<li>For any queries, contact us at support@neobank.com or 1800 103 1906</li>" +
               "</ul>" +
               "</div>" +
               "<p>If you have any questions or concerns about the foreclosure, please don't hesitate to contact our customer support team.</p>" +
               "<p style='margin-top: 30px;'>Best regards,<br>" +
               "<strong>NeoBank Customer Service Team</strong></p>" +
               "<hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
               "<p style='font-size: 11px; color: #999; text-align: center;'>" +
               "This is an automated email. Please do not reply to this message.<br>" +
               "© " + java.time.Year.now() + " NeoBank India Limited. All rights reserved." +
               "</p>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }

    /**
     * Send EMI reminder email to user
     */
    public boolean sendEmiReminderEmail(String toEmail, String userName, String loanAccountNumber, 
                                       java.time.LocalDate dueDate, Double emiAmount, Integer emiNumber) {
        initializeMailSender();
        
        try {
            if (!mailAvailable || mailSender == null) {
                System.out.println("==========================================");
                System.out.println("EMI REMINDER EMAIL (Mail not configured - Development Mode)");
                System.out.println("To: " + toEmail);
                System.out.println("Loan Account: " + loanAccountNumber);
                System.out.println("Due Date: " + dueDate);
                System.out.println("EMI Amount: ₹" + emiAmount);
                System.out.println("Installment #" + emiNumber);
                System.out.println("==========================================");
                return true;
            }
            
            Class<?> simpleMailMessageClass = Class.forName("org.springframework.mail.SimpleMailMessage");
            Object message = simpleMailMessageClass.getDeclaredConstructor().newInstance();
            
            Method setToMethod = simpleMailMessageClass.getMethod("setTo", String.class);
            Method setSubjectMethod = simpleMailMessageClass.getMethod("setSubject", String.class);
            Method setTextMethod = simpleMailMessageClass.getMethod("setText", String.class);
            Method setFromMethod = simpleMailMessageClass.getMethod("setFrom", String.class);
            
            setToMethod.invoke(message, toEmail);
            setSubjectMethod.invoke(message, "NeoBank - EMI Payment Reminder");
            setTextMethod.invoke(message, buildEmiReminderEmailBody(userName, loanAccountNumber, dueDate, emiAmount, emiNumber));
            String fromEmail = getFromEmail();
            setFromMethod.invoke(message, fromEmail);
            
            Method sendMethod = mailSender.getClass().getMethod("send", Object.class);
            sendMethod.invoke(mailSender, message);
            
            System.out.println("✅ EMI reminder email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Error sending EMI reminder email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Build email body for EMI reminder
     */
    private String buildEmiReminderEmailBody(String userName, String loanAccountNumber, 
                                            java.time.LocalDate dueDate, Double emiAmount, Integer emiNumber) {
        return "Dear " + userName + ",\n\n" +
               "This is a friendly reminder from NeoBank regarding your upcoming EMI payment.\n\n" +
               "Loan Account Number: " + loanAccountNumber + "\n" +
               "EMI Installment: #" + emiNumber + "\n" +
               "Due Date: " + dueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n" +
               "EMI Amount: ₹" + String.format("%.2f", emiAmount) + "\n\n" +
               "Please ensure sufficient balance in your savings account for the EMI payment to be processed automatically.\n\n" +
               "If you have already made the payment, please ignore this reminder.\n\n" +
               "Best regards,\n" +
               "NeoBank Customer Service Team\n\n" +
               "For any queries, contact us at: 1800 103 1906 | support@neobank.in";
    }
}

