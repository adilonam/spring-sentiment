package ma.code212.gateway.service;

import ma.code212.gateway.model.ScrapingJob;
import ma.code212.gateway.model.User;
import ma.code212.gateway.enums.JobStatus;
import ma.code212.gateway.repository.ScrapingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingJobService {

    private final ScrapingJobRepository scrapingJobRepository;

    /**
     * Create a new scraping job
     */
    @Transactional
    public ScrapingJob createScrapingJob(User user, String targetUrl, Map<String, Object> configuration) {
        log.info("Creating new scraping job for user: {} and URL: {}", user.getId(), targetUrl);
        
        ScrapingJob scrapingJob = ScrapingJob.builder()
                .user(user)
                .targetUrl(targetUrl)
                .status(JobStatus.PENDING)
                .pagesScraped(0)
                .commentsFound(0)
                .configuration(configuration)
                .build();
        
        ScrapingJob savedJob = scrapingJobRepository.save(scrapingJob);
        log.info("Created scraping job with ID: {}", savedJob.getId());
        
        return savedJob;
    }

    /**
     * Start a scraping job
     */
    @Transactional
    public ScrapingJob startScrapingJob(UUID jobId) {
        log.info("Starting scraping job with ID: {}", jobId);
        
        ScrapingJob job = scrapingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scraping job not found with ID: " + jobId));
        
        job.setStatus(JobStatus.RUNNING);
        job.setStartTime(LocalDateTime.now());
        
        ScrapingJob updatedJob = scrapingJobRepository.save(job);
        log.info("Started scraping job with ID: {}", updatedJob.getId());
        
        return updatedJob;
    }

    /**
     * Complete a scraping job successfully
     */
    @Transactional
    public ScrapingJob completeScrapingJob(UUID jobId, int pagesScraped, int commentsFound) {
        log.info("Completing scraping job with ID: {}, pages: {}, comments: {}", 
                jobId, pagesScraped, commentsFound);
        
        ScrapingJob job = scrapingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scraping job not found with ID: " + jobId));
        
        job.setStatus(JobStatus.COMPLETED);
        job.setEndTime(LocalDateTime.now());
        job.setPagesScraped(pagesScraped);
        job.setCommentsFound(commentsFound);
        
        ScrapingJob updatedJob = scrapingJobRepository.save(job);
        log.info("Completed scraping job with ID: {}", updatedJob.getId());
        
        return updatedJob;
    }

    /**
     * Fail a scraping job with error message
     */
    @Transactional
    public ScrapingJob failScrapingJob(UUID jobId, String errorMessage) {
        log.error("Failing scraping job with ID: {} due to error: {}", jobId, errorMessage);
        
        ScrapingJob job = scrapingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scraping job not found with ID: " + jobId));
        
        job.setStatus(JobStatus.FAILED);
        job.setEndTime(LocalDateTime.now());
        job.setErrors(errorMessage);
        
        ScrapingJob updatedJob = scrapingJobRepository.save(job);
        log.error("Failed scraping job with ID: {}", updatedJob.getId());
        
        return updatedJob;
    }

    /**
     * Update scraping job progress
     */
    @Transactional
    public ScrapingJob updateScrapingJobProgress(UUID jobId, int pagesScraped, int commentsFound) {
        log.debug("Updating scraping job progress - ID: {}, pages: {}, comments: {}", 
                jobId, pagesScraped, commentsFound);
        
        ScrapingJob job = scrapingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scraping job not found with ID: " + jobId));
        
        job.setPagesScraped(pagesScraped);
        job.setCommentsFound(commentsFound);
        
        return scrapingJobRepository.save(job);
    }

    /**
     * Find scraping job by ID
     */
    public Optional<ScrapingJob> findById(UUID id) {
        return scrapingJobRepository.findById(id);
    }

    /**
     * Find scraping jobs by user
     */
    public List<ScrapingJob> findByUser(User user) {
        return scrapingJobRepository.findByUser(user);
    }

    /**
     * Find scraping jobs by user ID
     */
    public List<ScrapingJob> findByUserId(UUID userId) {
        return scrapingJobRepository.findByUserId(userId);
    }

    /**
     * Find scraping jobs by status
     */
    public List<ScrapingJob> findByStatus(JobStatus status) {
        return scrapingJobRepository.findByStatus(status);
    }

    /**
     * Find scraping jobs by user and status
     */
    public List<ScrapingJob> findByUserIdAndStatus(UUID userId, JobStatus status) {
        return scrapingJobRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * Count scraping jobs by status
     */
    public long countByStatus(JobStatus status) {
        return scrapingJobRepository.countByStatus(status);
    }

    /**
     * Cancel a scraping job
     */
    @Transactional
    public ScrapingJob cancelScrapingJob(UUID jobId, String reason) {
        log.info("Cancelling scraping job with ID: {} due to: {}", jobId, reason);
        
        ScrapingJob job = scrapingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scraping job not found with ID: " + jobId));
        
        job.setStatus(JobStatus.CANCELLED);
        job.setEndTime(LocalDateTime.now());
        if (reason != null) {
            job.setErrors(reason);
        }
        
        ScrapingJob updatedJob = scrapingJobRepository.save(job);
        log.info("Cancelled scraping job with ID: {}", updatedJob.getId());
        
        return updatedJob;
    }
}
