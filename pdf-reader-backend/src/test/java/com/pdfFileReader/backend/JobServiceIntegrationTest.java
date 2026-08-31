package com.pdfFileReader.backend;

import com.pdfFileReader.domain.entity.JobStatus;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.ProcessingJob;
import com.pdfFileReader.domain.service.impl.JobService;
import com.pdfFileReader.repository.NotificationRepository;
import com.pdfFileReader.repository.ProcessingJobRepository;
import com.pdfFileReader.testutil.TestPdfFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asenkron kuyruk akisi: enqueue aninda QUEUED doner, isci arka planda
 * tamamlar. @Transactional DEGIL (isci ayri transaction'da); test sonunda
 * uretilen kayitlar temizlenir.
 */
@SpringBootTest
class JobServiceIntegrationTest {

    private static final String MARKER = "ASENKRON-TEST-" + System.currentTimeMillis();

    @Autowired
    private JobService jobService;

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @AfterEach
    void cleanup() {
        List<Notification> leftovers = notificationRepository.findAll().stream()
                .filter(n -> containsMarker(n))
                .toList();
        if (!leftovers.isEmpty()) {
            notificationRepository.deleteAll(leftovers);
        }
    }

    private boolean containsMarker(Notification n) {
        return (n.getTitle() != null && n.getTitle().contains(MARKER))
                || (n.getDescription() != null && n.getDescription().contains(MARKER));
    }

    @Test
    void enqueueReturnsQueuedImmediatelyAndWorkerCompletesIt() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("asenkron.pdf",
                MARKER + " Yer teslim bildirimi 10.04.2026 tarihinde yapilacaktir.",
                "Bu evrak asenkron kuyruk akisini dogrulamak icin gonderilmistir."
        );

        ProcessingJob job = jobService.enqueue(pdf, false);

        assertEquals(JobStatus.QUEUED, job.getStatus(), "enqueue aninda QUEUED donmeli");

        ProcessingJob completed = awaitCompletion(job.getId());

        assertEquals(JobStatus.COMPLETED, completed.getStatus(), "isci isi tamamlamali");
        assertTrue(completed.getNotificationCount() >= 1, "en az 1 bildirim uretilmeli");
        assertTrue(notificationRepository.findAll().stream().anyMatch(this::containsMarker),
                "bildirim DB'ye kaydedilmeli (MARKER baslikta veya aciklamada olmali)");

        jobRepository.deleteById(job.getId());
    }

    private ProcessingJob awaitCompletion(java.util.UUID id) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            ProcessingJob job = jobRepository.findById(id).orElseThrow();
            if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
                return job;
            }
            Thread.sleep(300);
        }
        return jobRepository.findById(id).orElseThrow();
    }
}
