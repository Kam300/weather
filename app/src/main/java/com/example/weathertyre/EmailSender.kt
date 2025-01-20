package com.example.weathertyre

import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailSender {
    fun sendEmail(recipientEmail: String, code: String) {
        val props = Properties()
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.socketFactory.port"] = "465"
        props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.port"] = "465"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.ssl.protocols"] = "TLSv1.2"

        try {
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        "polgary200@gmail.com",
                        "wufe ahgh fglx mdtz" // Замените на ваш 16-значный пароль приложения
                    )
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress("polgary200@gmail.com"))
            message.addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
            message.subject = "Код подтверждения"
            message.setText("Ваш код подтверждения: $code")

            Transport.send(message)
        } catch (e: MessagingException) {
            e.printStackTrace()
            throw e // Чтобы видеть ошибку в логах
        }
    }
}