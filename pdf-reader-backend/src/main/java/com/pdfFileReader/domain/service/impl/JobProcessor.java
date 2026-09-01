package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.domain.entity.JobStatus;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.ProcessingJob;
import com.pdfFileReader.domain.service.GeminiService;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.exception.NotificationNotFoundException;
import com.pdfFileReader.repository.ProcessingJobRepository;
import com.pdfFileReader.util.ByteArrayMultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asenkron evrak iscisi. @Async'in calismasi icin AYRI bean'dir
 * (JobService'ten proxy uzerinden cagrilir; self-invocation olsaydi
 * isleme istek thread'inde yurutulurdu).
 */
@Component
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private final ProcessingJobRepository jobRepository;
    private final NotificationService notificationService;
    private final GeminiService geminiService;

    /** jobId -> dosya icerigi (yalnizca bu sunucu oturumunda gecerli). */
    private final Map<UUID, byte[]> pendingFiles = new ConcurrentHashMap<>();

    public JobProcessor(
            ProcessingJobRepository jobRepository,
            NotificationService notificationService,
            GeminiService geminiService
    ) {
        this.jobRepository = jobRepository;
        this.notificationService = notificationService;
        this.geminiService = geminiService;
    }

    public void submitBytes(UUID jobId, byte[] bytes) {
        pendingFiles.put(jobId, bytes);
    }

    @Async("pdfProcessingExecutor")
    public void process(UUID jobId) {
        ProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotificationNotFoundException(jobId));

        byte[] bytes = pendingFiles.remove(jobId);
        if (bytes == null) {
            fail(job, "Dosya icerigi bulunamadi (sunucu yeniden baslatilmis olabilir).");
            return;
        }

        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);
        log.info("Evrak isleniyor: {} (job={})", job.getFileName(), jobId);

        MultipartFile file = new ByteArrayMultipartFile(bytes, job.getFileName(), "application/pdf");
        try {
            List<Notification> result = job.isUseAi()
                    ? geminiService.analyzeAndCreateNotifications(file, job.getUserId())
                    : notificationService.processAndSaveNotifications(file, job.getUserId());

            job.setStatus(JobStatus.COMPLETED);
            job.setNotificationCount(result.size());
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            log.info("Evrak islendi: {} (job={}, {} bildirim)", job.getFileName(), jobId, result.size());
        } catch (Exception e) {
            log.error("Evrak islenemedi: {} (job={})", job.getFileName(), jobId, e);
            fail(job, e.getMessage());
        }
    }

    private void fail(ProcessingJob job, String message) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(message != null && message.length() > 500 ? message.substring(0, 500) : message);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }
}
