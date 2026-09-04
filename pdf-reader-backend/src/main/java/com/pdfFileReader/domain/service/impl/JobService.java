package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.domain.entity.JobStatus;
import com.pdfFileReader.domain.entity.ProcessingJob;
import com.pdfFileReader.exception.NotificationNotFoundException;
import com.pdfFileReader.repository.ProcessingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Evrak isleme kuyrugunun HTTP tarafi: evrak'i kabul eder, isi kaydeder ve
 * arka plan iscisine (JobProcessor) devreder; aninda QUEUED durumuyla doner.
 */
@Service
public class JobService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final ProcessingJobRepository jobRepository;
    private final JobProcessor jobProcessor;

    public JobService(ProcessingJobRepository jobRepository, JobProcessor jobProcessor) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
    }

    /** Evrak'i kuyruga alir, aninda doner (agir islem arka planda). */
    public ProcessingJob enqueue(MultipartFile file, boolean useAi, UUID userId) {
        return enqueue(file, useAi, userId, null);
    }

    public ProcessingJob enqueue(MultipartFile file, boolean useAi, UUID userId, UUID groupId) {
        try {
            byte[] bytes = file.getBytes();

            ProcessingJob job = new ProcessingJob();
            job.setStatus(JobStatus.QUEUED);
            job.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "bilinmeyen.pdf");
            job.setUseAi(useAi);
            job.setUserId(userId);
            job.setGroupId(groupId);
            job.setSubmittedAt(LocalDateTime.now());

            job = jobRepository.save(job);
            jobProcessor.submitBytes(job.getId(), bytes);
            jobProcessor.process(job.getId());

            log.info("Evrak siraya alindi: {} (job={}, ai={}, user={})", job.getFileName(), job.getId(), useAi, userId);
            return job;
        } catch (IOException e) {
            throw new IllegalStateException("Dosya okunamadi: " + e.getMessage(), e);
        }
    }

    public ProcessingJob getJob(UUID id, UUID userId) {
        return jobRepository.findById(id)
                .filter(j -> userId.equals(j.getUserId()))
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    public List<ProcessingJob> recentJobs(int limit, UUID userId) {
        return jobRepository.findAllByUserIdOrderBySubmittedAtDesc(userId).stream()
                .limit(Math.max(Math.min(limit, 50), 1))
                .toList();
    }

    /** Sunucu acilirken yarim kalan isleri FAILED yapar (bytes kayboldugu icin devam edilemez). */
    @Override
    public void run(ApplicationArguments args) {
        List<ProcessingJob> orphans = jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == JobStatus.QUEUED || j.getStatus() == JobStatus.PROCESSING)
                .toList();

        for (ProcessingJob orphan : orphans) {
            orphan.setStatus(JobStatus.FAILED);
            orphan.setErrorMessage("Sunucu yeniden baslatildigi icin islem iptal edildi. Lutfen evrak'i tekrar gonderin.");
            orphan.setCompletedAt(LocalDateTime.now());
            jobRepository.save(orphan);
            log.info("Yarim kalan is FAILED yapildi: {}", orphan.getId());
        }
    }
}
