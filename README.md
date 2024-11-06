Small personal project that communicates with gmail and keeps an eye on unread incoming emails.

This project is using Java 21, dotenv,  jakarta mail, spring boot, lombok, maven and Spring Scheduling.

If the email has an attachment, the application downloads the attachment in the attachment/sender/email-date folder and writes a .txt with some basic information about the email.

To use the project after cloning, just add an .env to the root directory with the following content

EMAIL_USERNAME= your email address 
EMAIL_PASSWORD=your gmail app password. The app password is different from the password you usually use to log in. It can be generated in gmail's security settings.

Be very careful with the app password as it gives you full access to your account.


Future commits may add unit tests and code more aligned to clean code.
