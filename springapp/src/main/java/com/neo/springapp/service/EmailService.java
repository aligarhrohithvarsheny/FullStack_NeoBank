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
}

