package br.com.processar_emails.processar_emails.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.processar_emails.processar_emails.service.GmailService;

@Component
public class EmailScheduler {

    private final GmailService gmailService;

    public EmailScheduler(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @Scheduled(fixedRate = 60000) // 6000 ms = 1 minuto
    public void checkEmails() {
        gmailService.checkForNewEmails();
    }
}
