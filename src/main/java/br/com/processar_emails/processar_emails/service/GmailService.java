package br.com.processar_emails.processar_emails.service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
public class GmailService {
    
    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    public void checkForNewEmails() {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");

        try {
            // Conecta ao servidor de email usando IMAP com SSL
            Session emailSession = Session.getDefaultInstance(properties);
            Store store = emailSession.getStore();
            store.connect("imap.gmail.com", username, password);

            // Acessa a pasta INBOX
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            System.out.println("Mensagens não lidas : " + inbox.getUnreadMessageCount());

            // Busca emails não lidos e recebidos hoje
            FlagTerm unseenFlagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = inbox.search(unseenFlagTerm);


             /* Usa fetch profile para agilizar a busca   */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.CONTENT_INFO);
            inbox.fetch(messages, fp);
            inbox.getMessageCount();

            for (Message message : messages) {
                processMessage(message);
            }

            // Fecha a pasta e desconecta do armazenamento
            inbox.close(false);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Message message) throws Exception {
        System.out.println("Email de: " + message.getFrom()[0]);
        System.out.println("Assunto: " + message.getSubject());
    
        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            
            // Verifica e processa os anexos
            boolean hasAttachments = processAttachments(message, multipart);
            
            if (!hasAttachments) {
                System.out.println("Nenhum anexo encontrado.");
            }
        } else {
            System.out.println("Mensagem não contém partes multiparte.");
        }
    }

    private String createDynamicDirectory(Message message) throws MessagingException {
       // Obtém o endereço de e-mail do remetente
        Address[] fromAddresses = message.getFrom();
        String senderEmail = fromAddresses.length > 0 ? fromAddresses[0].toString() : "desconhecido";
        
        // Formata o e-mail do remetente para ser usado como nome de diretório
        senderEmail = senderEmail.replaceAll("[^a-zA-Z0-9.@]", "_");

        // Obtém a data do e-mail
        Date receivedDate = message.getReceivedDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String emailDate = dateFormat.format(receivedDate);

        // Define o caminho do diretório dinâmico
        String directoryPath = "anexos" + File.separator + senderEmail + File.separator + emailDate;

        // Cria o diretório, se não existir
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directoryPath;
    }

    private boolean processAttachments(Message message, Multipart multipart) throws Exception {
        boolean hasAttachments = false;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            
            if (isAttachment(bodyPart)) {
                hasAttachments = true;
                String directoryPath = createDynamicDirectory(message);

                downloadAttachment(bodyPart, directoryPath);
                saveInfoEmailToFile(directoryPath, message);
                
                markMessageAsRead(message);
            }
        }
        return hasAttachments;
    }

    private void downloadAttachment(BodyPart bodyPart, String directoryPath) throws IOException, MessagingException {
        String fileName = bodyPart.getFileName();
        System.out.println("Baixando anexo: " + fileName);

        InputStream is = bodyPart.getInputStream();
        File file = new File(directoryPath + File.separator + fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        System.out.println("Anexo salvo: " + file.getAbsolutePath());
    }

    private boolean isAttachment(BodyPart bodyPart) throws MessagingException {
        return Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition());
    }

    private void markMessageAsRead(Message message) throws MessagingException {
        message.setFlag(Flags.Flag.SEEN, true);
    }

    private static void saveInfoEmailToFile(String path, Message message) {
        try {
            // Formata o nome do arquivo como "email-info-<data-e-hora>.txt"
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String fileName = "email-info-" + timestamp + ".txt";
            File file = new File(path + File.separator + fileName);
            SimpleDateFormat brasilDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            // Cria o arquivo .txt e inicia a escrita de informações
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Obtenção das informações do e-mail
                String sender = message.getFrom()[0].toString();
                String receivedDate = brasilDateFormat.format(message.getReceivedDate());
                String subject = message.getSubject();
                List<String> attachmentNames = extractAttachmentNames(message);

                // Escreve as informações no arquivo .txt
                writer.write("Remetente: " + sender);
                writer.newLine();
                writer.write("Data: " + receivedDate);
                writer.newLine();
                writer.write("Assunto: " + subject);
                writer.newLine();
                writer.newLine();

                if (!attachmentNames.isEmpty()) {
                    writer.write("Anexos: ");
                    writer.newLine();
                    for (String attachmentName : attachmentNames) {
                        writer.write("- " + attachmentName);
                        writer.newLine();
                    }
                } else {
                    writer.write("Anexos: Nenhum anexo encontrado");
                }

                System.out.println("Informações do e-mail salvas em: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para obter nomes de anexos
    private static List<String> extractAttachmentNames(Message message) throws Exception {
        List<String> attachmentNames = new ArrayList<>();

        if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();

            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    attachmentNames.add(bodyPart.getFileName());
                }
            }
        }
        return attachmentNames;
    }

}
