package com.fiap.clyvovet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mails transacionais (recuperação de senha, boas-vindas).
 * <p>
 * Se {@code spring.mail.host} não estiver configurado, o Spring Boot não cria
 * o bean {@link JavaMailSender} — em vez de a aplicação falhar ao subir ou o
 * fluxo quebrar ao tentar enviar, este serviço registra o conteúdo no log.
 * Isso permite testar cadastro e recuperação de senha em ambiente local sem
 * precisar de um servidor SMTP real; basta configurar as variáveis
 * {@code MAIL_HOST}/{@code MAIL_USERNAME}/{@code MAIL_PASSWORD} em produção.
 */
@Service
public class NotificacaoEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String remetente;

    public NotificacaoEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    org.springframework.core.env.Environment env) {
        this.mailSenderProvider = mailSenderProvider;
        this.remetente = env.getProperty("spring.mail.username", "no-reply@clyvovet.com.br");
    }

    public void enviar(String destinatario, String assunto, String corpo) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("======================================================================");
            log.warn("[E-MAIL NÃO ENVIADO - servidor SMTP não configurado] Para: {}", destinatario);
            log.warn("Assunto: {}", assunto);
            log.warn("Corpo:\n{}", corpo);
            log.warn("======================================================================");
            return;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        mailSender.send(mensagem);
    }

    public boolean isEnvioRealAtivo() {
        return mailSenderProvider.getIfAvailable() != null;
    }
}
